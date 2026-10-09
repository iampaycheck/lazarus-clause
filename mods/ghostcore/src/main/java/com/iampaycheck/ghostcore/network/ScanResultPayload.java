package com.iampaycheck.ghostcore.network;

import com.iampaycheck.ghostcore.GhostCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server → client: what a scan found. Hostiles are entity ids so the client can track them as they move. */
public record ScanResultPayload(BlockPos origin, List<BlockPos> loot, List<BlockPos> ores, List<Integer> hostiles, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<ScanResultPayload> TYPE = new Type<>(GhostCore.id("scan_result"));
    public static final StreamCodec<ByteBuf, ScanResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScanResultPayload::origin,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ScanResultPayload::loot,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ScanResultPayload::ores,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ScanResultPayload::hostiles,
            ByteBufCodecs.VAR_INT, ScanResultPayload::durationTicks,
            ScanResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
