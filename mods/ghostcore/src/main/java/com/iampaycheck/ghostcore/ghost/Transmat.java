package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

/** Hold still for a few seconds and the Ghost transmats you to your linked Transmat Beacon. */
@EventBusSubscriber(modid = GhostCore.MODID)
public final class Transmat {

    public static void linkBeacon(ServerPlayer player, GlobalPos pos) {
        GhostData data = GhostManager.data(player);
        if (!data.bound) {
            GhostManager.notify(player, "ghostcore.message.no_ghost");
            return;
        }
        data.beacon = Optional.of(pos);
        player.level().playSound(null, pos.pos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1F, 1.2F);
        GhostManager.notify(player, "ghostcore.message.beacon_linked");
    }

    /** Key press: start a channel, or cancel the one in progress. */
    public static void request(ServerPlayer player, GhostData data) {
        if (data.isDowned()) return;
        if (data.isChanneling()) {
            cancel(player, data, "ghostcore.message.transmat_cancelled");
            return;
        }
        long now = player.level().getGameTime();
        if (data.beacon.isEmpty()) {
            GhostManager.notify(player, "ghostcore.message.no_beacon");
            return;
        }
        if (now < data.transmatReadyAt) {
            GhostManager.notify(player, "ghostcore.message.transmat_cooldown", Scanner.seconds(data.transmatReadyAt - now));
            return;
        }
        if (!GhostConfig.TRANSMAT_CROSS_DIMENSION.get() && data.beacon.get().dimension() != player.level().dimension()) {
            GhostManager.notify(player, "ghostcore.message.transmat_wrong_dimension");
            return;
        }
        data.transmatAt = now + Math.max(1, GhostConfig.ticks(GhostConfig.TRANSMAT_CHANNEL_SECONDS));
        data.transmatOrigin = player.position();
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, 1.5F);
        GhostManager.notify(player, "ghostcore.message.transmat_charging");
    }

    static void tick(ServerPlayer player, GhostData data, long now) {
        if (!data.isChanneling()) return;
        if (data.transmatOrigin == null || player.position().distanceToSqr(data.transmatOrigin) > 0.75 * 0.75) {
            cancel(player, data, "ghostcore.message.transmat_interrupted");
            return;
        }
        if (now % 2 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 6, 0.4, 0.8, 0.4, 0.02);
        }
        if (now >= data.transmatAt) complete(player, data, now);
    }

    private static void complete(ServerPlayer player, GhostData data, long now) {
        data.transmatAt = 0;
        GlobalPos target = data.beacon.orElse(null);
        ServerLevel dest = target == null ? null : player.server.getLevel(target.dimension());
        if (dest == null || !dest.getBlockState(target.pos()).is(ModBlocks.TRANSMAT_BEACON.get())) {
            data.beacon = Optional.empty();
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1F, 1.2F);
            GhostManager.notify(player, "ghostcore.message.transmat_signal_lost");
            return;
        }
        BlockPos arrival = target.pos().above();
        if (!dest.getBlockState(arrival).getCollisionShape(dest, arrival).isEmpty()
                || !dest.getBlockState(arrival.above()).getCollisionShape(dest, arrival.above()).isEmpty()) {
            GhostManager.notify(player, "ghostcore.message.transmat_obstructed");
            return;
        }

        ServerLevel origin = player.serverLevel();
        origin.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
        origin.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.4F);

        Vec3 to = Vec3.atBottomCenterOf(arrival);
        player.teleportTo(dest, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();

        dest.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 1, to.z, 30, 0.3, 0.8, 0.3, 0.05);
        dest.playSound(null, arrival, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        data.transmatReadyAt = now + GhostConfig.ticks(GhostConfig.TRANSMAT_COOLDOWN_SECONDS);
        GhostManager.notify(player, "ghostcore.message.transmat_complete");
    }

    private static void cancel(ServerPlayer player, GhostData data, String reasonKey) {
        data.transmatAt = 0;
        data.transmatOrigin = null;
        GhostManager.notify(player, reasonKey);
    }

    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GhostData data = GhostManager.data(player);
            if (data.isChanneling()) cancel(player, data, "ghostcore.message.transmat_interrupted");
        }
    }

    private Transmat() {}
}
