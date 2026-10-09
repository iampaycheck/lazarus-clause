package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The visible Ghost drone. It is a puppet: the server spawns one per bound player and discards it
 * whenever its owner is gone. Both sides run the same follow logic each tick, and clients ignore
 * server position updates while they can see the owner, so motion stays smooth without heavy syncing.
 */
public class GhostEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> LIGHT = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);

    /** Offset from the entity origin to the model's center. */
    public static final double CENTER = 0.2;

    /** Server: where this Ghost's light block currently sits. */
    @Nullable BlockPos lightPos;

    /** Client: 0 = shell closed, 1 = fully expanded. */
    private float shellOpen;
    private float shellOpenO;

    public GhostEntity(EntityType<? extends GhostEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public static GhostEntity create(ServerPlayer owner) {
        GhostEntity ghost = new GhostEntity(ModEntities.GHOST.get(), owner.level());
        ghost.entityData.set(OWNER_ID, owner.getId());
        Vec3 spawn = targetPosition(owner, GhostState.IDLE, 0);
        ghost.moveTo(spawn.x, spawn.y, spawn.z, owner.getYRot(), 0);
        return ghost;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
        builder.define(STATE, (byte) 0);
        builder.define(LIGHT, false);
    }

    @Nullable
    public Player getOwner() {
        return level().getEntity(entityData.get(OWNER_ID)) instanceof Player player ? player : null;
    }

    public GhostState getGhostState() {
        return GhostState.byId(entityData.get(STATE));
    }

    void setGhostState(GhostState state) {
        entityData.set(STATE, (byte) state.ordinal());
    }

    public boolean isLightOn() {
        return entityData.get(LIGHT);
    }

    void setLightOn(boolean on) {
        entityData.set(LIGHT, on);
    }

    public float getShellOpen(float partialTick) {
        return Mth.lerp(partialTick, shellOpenO, shellOpen);
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }

        GhostState state = getGhostState();
        Vec3 target = targetPosition(owner, state, tickCount);
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() > 64) {
            setPos(target);
        } else {
            setPos(position().add(delta.scale(state == GhostState.IDLE ? 0.3 : 0.45)));
        }

        switch (state) {
            case REVIVING -> {
                setYRot(Mth.rotLerp(0.3F, getYRot(), owner.getYRot() + 180F));
                setXRot(Mth.lerp(0.3F, getXRot(), 25F));
            }
            case TRANSMAT -> {
                setYRot(Mth.wrapDegrees(getYRot() + 18F));
                setXRot(Mth.lerp(0.3F, getXRot(), 0F));
            }
            default -> {
                setYRot(Mth.rotLerp(0.35F, getYRot(), owner.getYRot()));
                setXRot(Mth.lerp(0.35F, getXRot(), owner.getXRot() * (state == GhostState.SCANNING ? 1F : 0.5F)));
            }
        }

        if (level().isClientSide) {
            shellOpenO = shellOpen;
            float goal = state == GhostState.IDLE ? 0F : 1F;
            shellOpen += (goal - shellOpen) * 0.25F;
        } else {
            GhostLight.update(this);
        }
    }

    /** Where the Ghost wants its origin to be for the given owner and state. */
    public static Vec3 targetPosition(Player owner, GhostState state, float age) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 eye = owner.getEyePosition();
        double bob = Mth.sin(age * 0.12F) * 0.06;
        Vec3 center = switch (state) {
            // Over the right shoulder, slightly behind: out of the first-person view until you turn.
            case IDLE -> eye.add(right.scale(0.7)).add(forward.scale(-0.4)).add(0, 0.3 + bob, 0);
            // Pops out in front of your face to scan.
            case SCANNING -> eye.add(owner.getLookAngle().scale(1.3)).add(0, 0.05, 0);
            // Hovers in front of the downed player, facing them.
            case REVIVING -> eye.add(forward.scale(1.1)).add(0, 0.2 + bob, 0);
            case TRANSMAT -> eye.add(0, 0.75, 0);
        };
        return center.subtract(0, CENTER, 0);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // The client simulates the follow itself; only accept server positions when it can't.
        if (getOwner() == null || distanceToSqr(x, y, z) > 64) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            GhostLight.clear(this);
        }
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
