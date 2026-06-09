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
 * Server -> client. Spawns a hallucination sighting in the receiving player's
 * client level only, so no one else can ever see it. The client builds a
 * {@code HallucinationEntity} at the given position with the given lifespan.
 */
public record HallucinationPacket(double x, double y, double z, int lifespan, int variant)
        implements CustomPacketPayload {

    public static final Type<HallucinationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "hallucination"));

    public static final StreamCodec<ByteBuf, HallucinationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, HallucinationPacket::x,
            ByteBufCodecs.DOUBLE, HallucinationPacket::y,
            ByteBufCodecs.DOUBLE, HallucinationPacket::z,
            ByteBufCodecs.VAR_INT, HallucinationPacket::lifespan,
            ByteBufCodecs.VAR_INT, HallucinationPacket::variant,
            HallucinationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HallucinationPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.onHallucination(pkt)));
    }
}
