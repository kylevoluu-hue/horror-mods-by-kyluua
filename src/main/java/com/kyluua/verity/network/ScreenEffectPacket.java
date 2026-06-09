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
 * Server -> client. Triggers a one-off screen effect on a targeted player.
 *
 * <p>{@code effect} values: 0 = vignette pulse, 1 = wobble/distortion, 2 = glitch
 * flash, 3 = jumpscare flash. {@code intensity} 0-1 and {@code durationTicks}
 * control strength/length. The client respects the player's comfort config.</p>
 */
public record ScreenEffectPacket(int effect, float intensity, int durationTicks) implements CustomPacketPayload {

    public static final Type<ScreenEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "screen_effect"));

    public static final StreamCodec<ByteBuf, ScreenEffectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScreenEffectPacket::effect,
            ByteBufCodecs.FLOAT, ScreenEffectPacket::intensity,
            ByteBufCodecs.VAR_INT, ScreenEffectPacket::durationTicks,
            ScreenEffectPacket::new);

    // Effect id constants for readability at call sites.
    public static final int VIGNETTE = 0;
    public static final int DISTORTION = 1;
    public static final int GLITCH = 2;
    public static final int JUMPSCARE = 3;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScreenEffectPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.onScreenEffect(pkt));
    }
}
