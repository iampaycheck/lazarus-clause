package com.iampaycheck.ghostcore.registry;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.block.GhostLightBlock;
import com.iampaycheck.ghostcore.block.TransmatBeaconBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GhostCore.MODID);

    public static final DeferredBlock<TransmatBeaconBlock> TRANSMAT_BEACON = BLOCKS.registerBlock("transmat_beacon",
            TransmatBeaconBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 10)
                    .noOcclusion());

    /** Invisible light the Ghost carries. Removes itself when no Ghost claims it. Never obtainable. */
    public static final DeferredBlock<GhostLightBlock> GHOST_LIGHT = BLOCKS.registerBlock("ghost_light",
            GhostLightBlock::new,
            BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .instabreak()
                    .lightLevel(state -> 14)
                    .pushReaction(PushReaction.DESTROY)
                    .isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false));

    private ModBlocks() {}
}
