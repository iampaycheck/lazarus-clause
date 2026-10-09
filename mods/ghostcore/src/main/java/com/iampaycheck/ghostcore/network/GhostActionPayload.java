package com.iampaycheck.ghostcore.network;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import com.iampaycheck.ghostcore.ghost.GhostPocket;
import com.iampaycheck.ghostcore.ghost.Scanner;
import com.iampaycheck.ghostcore.ghost.Transmat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the player pressed one of the Ghost keys. */
public record GhostActionPayload(Action action) implements CustomPacketPayload {
    public enum Action { SCAN, TRANSMAT, TOGGLE_LIGHT, OPEN_POCKET }

    private static final Action[] ACTIONS = Action.values();

    public static final Type<GhostActionPayload> TYPE = new Type<>(GhostCore.id("action"));
    public static final StreamCodec<ByteBuf, GhostActionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            id -> new GhostActionPayload(ACTIONS[Math.floorMod(id, ACTIONS.length)]),
            payload -> payload.action().ordinal());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(GhostActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        GhostData data = GhostManager.data(player);
        if (!data.bound) {
            GhostManager.notify(player, "ghostcore.message.no_ghost");
            return;
        }
        switch (payload.action()) {
            case SCAN -> Scanner.scan(player, data);
            case TRANSMAT -> Transmat.request(player, data);
            case OPEN_POCKET -> GhostPocket.open(player, data);
            case TOGGLE_LIGHT -> {
                data.lightOn = !data.lightOn;
                GhostManager.notify(player, data.lightOn ? "ghostcore.message.light_on" : "ghostcore.message.light_off");
            }
        }
    }
}
