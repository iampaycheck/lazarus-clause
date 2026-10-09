package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.api.GhostReviveEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;

/**
 * Lethal damage puts a bound player into a short "downed" state instead of killing them: they can't move,
 * act or be hurt while the Ghost restores them. Each resurrection spends a charge; with none left, death is real.
 */
@EventBusSubscriber(modid = GhostCore.MODID)
public final class Resurrection {
    private static final ResourceLocation DOWNED = GhostCore.id("downed");
    private static final AttributeModifier ROOTED = new AttributeModifier(DOWNED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // Low priority so totems and other mods' death handlers get first say; cancelled events never reach us.
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GhostData data = GhostManager.data(player);
        DamageSource source = event.getSource();
        if (!data.bound || data.isDowned() || data.charges <= 0 || source.is(DamageTypes.GENERIC_KILL)) return;

        boolean relocate = needsRelocation(player, source);
        if (relocate && (data.lastSafePos == null || data.lastSafeDim != player.level().dimension())) return;
        if (NeoForge.EVENT_BUS.post(new GhostReviveEvent(player, source)).isCanceled()) return;

        event.setCanceled(true);
        long now = player.level().getGameTime();
        player.setHealth(1.0F);
        data.charges--;
        data.downedUntil = now + Math.max(1, GhostConfig.ticks(GhostConfig.REVIVE_DELAY_SECONDS));
        data.transmatAt = 0;

        if (relocate) {
            Vec3 safe = data.lastSafePos;
            player.teleportTo(player.serverLevel(), safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        }
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.stopUsingItem();
        applyDownedModifiers(player);

        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1F, 0.8F);
        GhostManager.notify(player, "ghostcore.message.downed");
    }

    /** Environmental deaths would kill again on the spot, so the Ghost pulls you back to safe ground. */
    private static boolean needsRelocation(ServerPlayer player, DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FREEZE)
                || player.getY() < player.level().getMinBuildHeight()
                || player.isInLava()
                || player.isInWall()
                || player.isEyeInFluid(FluidTags.WATER);
    }

    static void tick(ServerPlayer player, GhostData data, long now) {
        if (!data.isDowned()) return;
        applyDownedModifiers(player); // idempotent; restores them after a relog
        if (now % 4 == 0) {
            GhostEntity ghost = GhostManager.ghostOf(player);
            if (ghost != null) beam((ServerLevel) player.level(), ghost.position().add(0, GhostEntity.CENTER, 0), player.position().add(0, 1.0, 0));
        }
        if (now >= data.downedUntil) revive(player, data);
    }

    private static void revive(ServerPlayer player, GhostData data) {
        data.downedUntil = 0;
        clearDownedModifiers(player);
        player.setHealth(Math.max(1F, (float) (player.getMaxHealth() * GhostConfig.REVIVE_HEALTH_FRACTION.get())));
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) player.removeEffect(effect.getEffect());
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
        player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), 6));

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 0.3, 0.6, 0.3, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.2F);
        GhostManager.notify(player, "ghostcore.message.revived", data.charges);
    }

    private static void beam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).scale(1 / 6.0);
        for (int i = 0; i <= 6; i++) {
            Vec3 p = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void applyDownedModifiers(Player player) {
        addRoot(player.getAttribute(Attributes.MOVEMENT_SPEED));
        addRoot(player.getAttribute(Attributes.JUMP_STRENGTH));
    }

    private static void addRoot(AttributeInstance attribute) {
        if (attribute != null && !attribute.hasModifier(DOWNED)) attribute.addTransientModifier(ROOTED);
    }

    static void clearDownedModifiers(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(DOWNED);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(DOWNED);
    }

    private static boolean isDowned(Player player) {
        return !player.level().isClientSide && GhostManager.data(player).isDowned();
    }

    // While downed: untouchable, untargetable, and unable to act.

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isDowned(player) && !event.getSource().is(DamageTypes.GENERIC_KILL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && isDowned(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        cancelIfDowned(event.getEntity(), event);
    }

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfDowned(event.getEntity(), event);
    }

    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfDowned(event.getEntity(), event);
    }

    @SubscribeEvent
    static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfDowned(event.getEntity(), event);
    }

    @SubscribeEvent
    static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        cancelIfDowned(event.getEntity(), event);
    }

    private static void cancelIfDowned(Player player, ICancellableEvent event) {
        if (isDowned(player)) event.setCanceled(true);
    }

    private Resurrection() {}
}
