package com.iampaycheck.ghostcore.network;

import com.iampaycheck.ghostcore.client.ClientPayloads;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GhostNetwork {
    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(GhostActionPayload.TYPE, GhostActionPayload.STREAM_CODEC, GhostActionPayload::handle);
        // Lambdas keep client classes from loading on a dedicated server.
        registrar.playToClient(GhostSyncPayload.TYPE, GhostSyncPayload.STREAM_CODEC, (payload, context) -> ClientPayloads.onSync(payload));
        registrar.playToClient(ScanResultPayload.TYPE, ScanResultPayload.STREAM_CODEC, (payload, context) -> ClientPayloads.onScan(payload));
    }

    /** Sends only to real clients that negotiated our channel (not fake/mock players). */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private GhostNetwork() {}
}
