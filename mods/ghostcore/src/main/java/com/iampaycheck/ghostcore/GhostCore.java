package com.iampaycheck.ghostcore;

import com.iampaycheck.ghostcore.compat.TaczCompat;
import com.iampaycheck.ghostcore.network.GhostNetwork;
import com.iampaycheck.ghostcore.registry.ModAttachments;
import com.iampaycheck.ghostcore.registry.ModBlocks;
import com.iampaycheck.ghostcore.registry.ModCreativeTab;
import com.iampaycheck.ghostcore.registry.ModEntities;
import com.iampaycheck.ghostcore.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Ghost Core — the foundational mod of the pack. Every operator is issued a Ghost:
 * a Company-owned AI drone that keeps its asset (you) alive, scans, lights the way and transmats.
 */
@Mod(GhostCore.MODID)
public class GhostCore {
    public static final String MODID = "ghostcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GhostCore(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(GhostNetwork::register);
        TaczCompat.register();

        container.registerConfig(ModConfig.Type.SERVER, GhostConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, GhostConfig.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
