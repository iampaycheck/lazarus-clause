package com.iampaycheck.ghostcore.network;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

/**
 * Server → client: everything the HUD needs. Timers are absolute game times, so this only
 * changes on real events (scan, death, transmat...) and is sent only when it changes.
 */
public record GhostSyncPayload(
        boolean bound,
        int charges,
        int maxCharges,
        long nextChargeAt,
        long scanReadyAt,
        long transmatReadyAt,
        long transmatAt,
        int transmatTotal,
        long downedUntil,
        int reviveTotal,
        boolean lightOn,
        Optional<GlobalPos> beacon
) implements CustomPacketPayload {
    public static final GhostSyncPayload EMPTY = new GhostSyncPayload(false, 0, 0, 0, 0, 0, 0, 1, 0, 1, false, Optional.empty());

    public static final Type<GhostSyncPayload> TYPE = new Type<>(GhostCore.id("sync"));
    public static final StreamCodec<FriendlyByteBuf, GhostSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(GhostSyncPayload::write, GhostSyncPayload::read);

    private static final StreamCodec<io.netty.buffer.ByteBuf, Optional<GlobalPos>> BEACON_CODEC = ByteBufCodecs.optional(GlobalPos.STREAM_CODEC);

    public static GhostSyncPayload of(GhostData data) {
        return new GhostSyncPayload(
                data.bound,
                data.charges,
                GhostConfig.MAX_CHARGES.get(),
                data.nextChargeAt,
                data.scanReadyAt,
                data.transmatReadyAt,
                data.transmatAt,
                Math.max(1, GhostConfig.ticks(GhostConfig.TRANSMAT_CHANNEL_SECONDS)),
                data.downedUntil,
                Math.max(1, GhostConfig.ticks(GhostConfig.REVIVE_DELAY_SECONDS)),
                data.lightOn,
                data.beacon);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(bound);
        buf.writeVarInt(charges);
        buf.writeVarInt(maxCharges);
        buf.writeVarLong(nextChargeAt);
        buf.writeVarLong(scanReadyAt);
        buf.writeVarLong(transmatReadyAt);
        buf.writeVarLong(transmatAt);
        buf.writeVarInt(transmatTotal);
        buf.writeVarLong(downedUntil);
        buf.writeVarInt(reviveTotal);
        buf.writeBoolean(lightOn);
        BEACON_CODEC.encode(buf, beacon);
    }

    private static GhostSyncPayload read(FriendlyByteBuf buf) {
        return new GhostSyncPayload(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readBoolean(),
                BEACON_CODEC.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
