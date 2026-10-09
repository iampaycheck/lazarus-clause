package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.network.ScanResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import com.iampaycheck.ghostcore.network.GhostNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative scan. The server knows which containers still hold their loot table (unlooted caches),
 * which the client can't see, then the client renders the results as a sweeping x-ray pulse.
 */
public final class Scanner {
    private static final int MAX_LOOT = 128;
    private static final int MAX_ORES = 256;
    private static final int MAX_HOSTILES = 128;

    public static void scan(ServerPlayer player, GhostData data) {
        if (data.isDowned()) return;
        long now = player.level().getGameTime();
        if (now < data.scanReadyAt) {
            GhostManager.notify(player, "ghostcore.message.scan_cooldown", seconds(data.scanReadyAt - now));
            return;
        }

        ServerLevel level = player.serverLevel();
        ScanResultPayload result = collect(level, player.blockPosition(), GhostConfig.SCAN_RADIUS.get(),
                GhostConfig.SCAN_ORE_RADIUS.get(), GhostConfig.ticks(GhostConfig.SCAN_HIGHLIGHT_SECONDS));
        GhostNetwork.send(player, result);

        data.scanReadyAt = now + GhostConfig.ticks(GhostConfig.SCAN_COOLDOWN_SECONDS);
        data.scanAnimUntil = now + 30;
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getEyeY(), player.getZ(), 12, 0.6, 0.3, 0.6, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.8F);
        GhostManager.notify(player, "ghostcore.message.scan_result", result.loot().size(), result.hostiles().size(), result.ores().size());
    }

    public static ScanResultPayload collect(ServerLevel level, BlockPos origin, int radius, int oreRadius, int durationTicks) {
        double maxDistSq = (double) radius * radius;
        List<Integer> hostiles = level.getEntitiesOfClass(LivingEntity.class, new AABB(origin).inflate(radius),
                        e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(origin.getCenter()) <= maxDistSq)
                .stream().limit(MAX_HOSTILES).map(Entity::getId).toList();
        return new ScanResultPayload(origin, findUnlootedCaches(level, origin, radius), findOres(level, origin, oreRadius),
                hostiles, durationTicks);
    }

    private static List<BlockPos> findUnlootedCaches(ServerLevel level, BlockPos origin, int radius) {
        List<BlockPos> found = new ArrayList<>();
        double maxDistSq = (double) radius * radius;
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof RandomizableContainer container && container.getLootTable() != null
                            && be.getBlockPos().distSqr(origin) <= maxDistSq) {
                        found.add(be.getBlockPos());
                        if (found.size() >= MAX_LOOT) return found;
                    }
                }
            }
        }
        return found;
    }

    private static List<BlockPos> findOres(ServerLevel level, BlockPos origin, int radius) {
        List<BlockPos> found = new ArrayList<>();
        int maxDistSq = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
                    cursor.setWithOffset(origin, dx, dy, dz);
                    if (level.isOutsideBuildHeight(cursor)) continue;
                    if (level.getBlockState(cursor).is(Tags.Blocks.ORES)) {
                        found.add(cursor.immutable());
                        if (found.size() >= MAX_ORES) return found;
                    }
                }
            }
        }
        return found;
    }

    static long seconds(long ticks) {
        return (ticks + 19) / 20;
    }

    private Scanner() {}
}
