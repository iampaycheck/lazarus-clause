package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.network.GhostSyncPayload;
import com.iampaycheck.ghostcore.registry.ModAttachments;
import com.iampaycheck.ghostcore.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.iampaycheck.ghostcore.network.GhostNetwork;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side lifecycle: binding, the per-player tick, Ghost entity upkeep and HUD sync. */
@EventBusSubscriber(modid = GhostCore.MODID)
public final class GhostManager {
    private static final Map<UUID, GhostEntity> GHOSTS = new HashMap<>();
    private static final Map<UUID, GhostSyncPayload> LAST_SYNC = new HashMap<>();

    public static GhostData data(Player player) {
        return player.getData(ModAttachments.GHOST);
    }

    @Nullable
    public static GhostEntity ghostOf(Player player) {
        GhostEntity ghost = GHOSTS.get(player.getUUID());
        return ghost != null && !ghost.isRemoved() ? ghost : null;
    }

    /** Links a Ghost to the player. Returns false if they already have one. */
    public static boolean bind(ServerPlayer player) {
        GhostData data = data(player);
        if (data.bound) return false;
        data.bound = true;
        data.starterGiven = true;
        data.charges = GhostConfig.MAX_CHARGES.get();
        data.nextChargeAt = 0;
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 1.6F);
        player.sendSystemMessage(Component.translatable("ghostcore.message.bound").withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static void unbind(ServerPlayer player) {
        GhostData data = data(player);
        data.bound = false;
        data.downedUntil = 0;
        data.transmatAt = 0;
        Resurrection.clearDownedModifiers(player);
        despawn(player);
    }

    public static void notify(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA), true);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        GhostData data = data(player);
        long now = player.level().getGameTime();

        if (data.bound) {
            tickCharges(player, data, now);
            Resurrection.tick(player, data, now);
            Transmat.tick(player, data, now);
            trackSafePosition(player, data);
            upkeepGhost(player, data, now);
        } else {
            despawn(player);
        }
        sync(player, data);
    }

    private static void tickCharges(ServerPlayer player, GhostData data, long now) {
        int max = GhostConfig.MAX_CHARGES.get();
        if (data.charges >= max) {
            data.charges = max;
            data.nextChargeAt = 0;
            return;
        }
        if (data.nextChargeAt == 0) {
            data.nextChargeAt = now + GhostConfig.ticks(GhostConfig.CHARGE_REGEN_SECONDS);
        } else if (now >= data.nextChargeAt) {
            data.charges++;
            data.nextChargeAt = data.charges < max ? now + GhostConfig.ticks(GhostConfig.CHARGE_REGEN_SECONDS) : 0;
            notify(player, "ghostcore.message.charge_restored", data.charges, max);
        }
    }

    private static void trackSafePosition(ServerPlayer player, GhostData data) {
        if (player.tickCount % 5 != 0 || data.isDowned()) return;
        if (player.onGround() && !player.isInLava() && !player.isInWater() && !player.isInWall()
                && !player.isOnFire() && player.getY() > player.level().getMinBuildHeight()) {
            data.lastSafePos = player.position();
            data.lastSafeDim = player.level().dimension();
        }
    }

    private static void upkeepGhost(ServerPlayer player, GhostData data, long now) {
        if (!player.isAlive() || player.isSpectator()) {
            despawn(player);
            return;
        }
        GhostEntity ghost = GHOSTS.get(player.getUUID());
        if (ghost == null || ghost.isRemoved() || ghost.level() != player.level()) {
            if (ghost != null && !ghost.isRemoved()) ghost.discard();
            ghost = GhostEntity.create(player);
            player.serverLevel().addFreshEntity(ghost);
            GHOSTS.put(player.getUUID(), ghost);
        }

        GhostState state;
        if (data.isDowned()) state = GhostState.REVIVING;
        else if (data.isChanneling()) state = GhostState.TRANSMAT;
        else if (now < data.scanAnimUntil) state = GhostState.SCANNING;
        else state = GhostState.IDLE;
        ghost.setGhostState(state);
        ghost.setLightOn(data.lightOn);
        ghost.setInvisible(player.isInvisible());
    }

    private static void despawn(Player player) {
        GhostEntity ghost = GHOSTS.remove(player.getUUID());
        if (ghost != null && !ghost.isRemoved()) ghost.discard();
    }

    private static void sync(ServerPlayer player, GhostData data) {
        GhostSyncPayload snapshot = GhostSyncPayload.of(data);
        if (!snapshot.equals(LAST_SYNC.get(player.getUUID()))) {
            GhostNetwork.send(player, snapshot);
            LAST_SYNC.put(player.getUUID(), snapshot);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LAST_SYNC.remove(player.getUUID());
        GhostData data = data(player);
        if (data.starterGiven || data.bound) return;
        switch (GhostConfig.STARTER_MODE.get()) {
            case BIND -> bind(player);
            case ITEM -> {
                player.getInventory().placeItemBackInInventory(ModItems.GHOST_SHELL.toStack());
                data.starterGiven = true;
            }
            case NONE -> data.starterGiven = true;
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        despawn(event.getEntity());
        LAST_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        GhostData data = data(event.getEntity());
        data.downedUntil = 0;
        data.transmatAt = 0;
        LAST_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        GhostData data = data(event.getEntity());
        data.lastSafePos = null;
        data.lastSafeDim = null;
        LAST_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        GHOSTS.clear();
        LAST_SYNC.clear();
    }

    private GhostManager() {}
}
