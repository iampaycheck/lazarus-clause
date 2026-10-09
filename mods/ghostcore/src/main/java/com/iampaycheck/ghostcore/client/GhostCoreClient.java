package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.client.render.GhostModel;
import com.iampaycheck.ghostcore.client.render.GhostRenderer;
import com.iampaycheck.ghostcore.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GhostCore.MODID, dist = Dist.CLIENT)
public class GhostCoreClient {
    public GhostCoreClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(GhostCoreClient::registerRenderers);
        modBus.addListener(GhostCoreClient::registerLayers);
        modBus.addListener(GhostKeys::register);
        modBus.addListener(GhostHud::register);

        NeoForge.EVENT_BUS.addListener(GhostKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScanHighlights::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(GhostCoreClient::onLogout);
        TaczClientGuard.register();
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GHOST.get(), GhostRenderer::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GhostModel.LAYER, GhostModel::createLayer);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPayloads.reset();
    }
}
