package com.iampaycheck.ghostcore.registry;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.item.GhostShellItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GhostCore.MODID);

    public static final DeferredItem<GhostShellItem> GHOST_SHELL = ITEMS.registerItem("ghost_shell",
            GhostShellItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<BlockItem> TRANSMAT_BEACON = ITEMS.registerSimpleBlockItem("transmat_beacon",
            ModBlocks.TRANSMAT_BEACON);

    private ModItems() {}
}
