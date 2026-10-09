package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.network.GhostActionPayload;
import com.iampaycheck.ghostcore.network.GhostActionPayload.Action;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class GhostKeys {
    private static final String CATEGORY = "key.categories.ghostcore";

    public static final KeyMapping SCAN = key("scan", GLFW.GLFW_KEY_G);
    public static final KeyMapping TRANSMAT = key("transmat", GLFW.GLFW_KEY_H);
    public static final KeyMapping POCKET = key("pocket", GLFW.GLFW_KEY_J);
    public static final KeyMapping LIGHT = key("light", GLFW.GLFW_KEY_K);

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.ghostcore." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(SCAN);
        event.register(TRANSMAT);
        event.register(POCKET);
        event.register(LIGHT);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;
        while (SCAN.consumeClick()) send(Action.SCAN);
        while (TRANSMAT.consumeClick()) send(Action.TRANSMAT);
        while (POCKET.consumeClick()) send(Action.OPEN_POCKET);
        while (LIGHT.consumeClick()) send(Action.TOGGLE_LIGHT);
    }

    private static void send(Action action) {
        PacketDistributor.sendToServer(new GhostActionPayload(action));
    }

    private GhostKeys() {}
}
