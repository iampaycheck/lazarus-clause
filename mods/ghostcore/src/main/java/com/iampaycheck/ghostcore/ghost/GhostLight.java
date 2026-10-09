package com.iampaycheck.ghostcore.ghost;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Moves an invisible light block along with the Ghost when it's dark. Light blocks re-check every second
 * and delete themselves once no Ghost claims them, so a crash or unload never leaves stray lights behind.
 */
public final class GhostLight {
    /** Below this sky light (after time-of-day darkening) the Ghost lights up. Ignores block light to avoid feedback. */
    private static final int DARK_THRESHOLD = 8;
    /** Keep the current light while the Ghost bobs within this distance of it, to avoid relighting every tick. */
    private static final double STICKY_DISTANCE_SQ = 1.5 * 1.5;

    static void update(GhostEntity ghost) {
        ServerLevel level = (ServerLevel) ghost.level();
        BlockPos here = ghost.blockPosition();
        if (!ghost.isLightOn() || !GhostConfig.LIGHT_ENABLED.get() || !isDark(level, here)) {
            clear(ghost);
            return;
        }
        BlockPos current = ghost.lightPos;
        if (current != null && level.getBlockState(current).is(ModBlocks.GHOST_LIGHT.get())
                && current.getCenter().distanceToSqr(ghost.position()) < STICKY_DISTANCE_SQ) {
            return;
        }
        BlockPos spot = findSpot(level, here);
        if (spot == null) return;
        clear(ghost);
        level.setBlock(spot, ModBlocks.GHOST_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
        ghost.lightPos = spot;
    }

    static void clear(GhostEntity ghost) {
        BlockPos pos = ghost.lightPos;
        if (pos == null) return;
        ghost.lightPos = null;
        Level level = ghost.level();
        if (level.isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.GHOST_LIGHT.get())) {
            level.removeBlock(pos, false);
        }
    }

    public static boolean isClaimed(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(GhostEntity.class, new AABB(pos).inflate(2), g -> pos.equals(g.lightPos)).isEmpty();
    }

    private static boolean isDark(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() < DARK_THRESHOLD;
    }

    @Nullable
    private static BlockPos findSpot(Level level, BlockPos origin) {
        for (BlockPos candidate : new BlockPos[]{origin, origin.above(), origin.below()}) {
            BlockState state = level.getBlockState(candidate);
            if (state.is(ModBlocks.GHOST_LIGHT.get())) return null; // another Ghost already lights this spot
            if (state.isAir()) return candidate;
        }
        return null;
    }

    private GhostLight() {}
}
