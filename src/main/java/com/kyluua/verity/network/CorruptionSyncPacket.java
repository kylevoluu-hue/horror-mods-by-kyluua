package com.kyluua.verity.network;

import com.kyluua.verity.Verity;
import com.kyluua.verity.client.ClientPacketHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.handling.IPayloadContext;

/**
 * Server -> client. Tells the client the current corruption level + stage so it can
 * drive fog density, music intensity and other ambient effects. Broadcast whenever
 * the level changes and on player join.
 */
public record CorruptionSyncPacket(int level, int stage) implements CustomPacketPayload {

    public static final Type<CorruptionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "corruption_sync"));

    public static final StreamCodec<ByteBuf, CorruptionSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CorruptionSyncPacket::level,
            ByteBufCodecs.VAR_INT, CorruptionSyncPacket::stage,
            CorruptionSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the client; DistExecutor keeps the client class off the dedicated server. */
    public static void handle(CorruptionSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.onCorruptionSync(pkt)));
    }
}
