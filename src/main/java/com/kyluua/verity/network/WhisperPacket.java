package com.kyluua.verity.network;

import com.kyluua.verity.Verity;
import com.kyluua.verity.client.ClientPacketHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client. Plays a positioned whisper/ambient sound at a world location for
 * a single player, so the sound seems to come from somewhere specific only they hear.
 *
 * <p>{@code soundId} indexes into a small client-side table of whisper/ambient
 * sounds; {@code volume} and {@code pitch} let the director shape the delivery.</p>
 */
public record WhisperPacket(double x, double y, double z, int soundId, float volume, float pitch)
        implements CustomPacketPayload {

    public static final Type<WhisperPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "whisper"));

    public static final StreamCodec<ByteBuf, WhisperPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, WhisperPacket::x,
            ByteBufCodecs.DOUBLE, WhisperPacket::y,
            ByteBufCodecs.DOUBLE, WhisperPacket::z,
            ByteBufCodecs.VAR_INT, WhisperPacket::soundId,
            ByteBufCodecs.FLOAT, WhisperPacket::volume,
            ByteBufCodecs.FLOAT, WhisperPacket::pitch,
            WhisperPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhisperPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.onWhisper(pkt));
    }
}
