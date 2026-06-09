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
 * Server -> client. Injects a fake message into one player's chat. Used for fake
 * player chatter and fake join/leave notifications - messages only that player
 * sees, with no real player behind them.
 *
 * <p>{@code kind}: 0 = chat line (already formatted), 1 = fake join, 2 = fake leave.
 * For join/leave the {@code text} is the fake player name.</p>
 */
public record FakeChatPacket(String text, int kind) implements CustomPacketPayload {

    public static final Type<FakeChatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "fake_chat"));

    public static final StreamCodec<ByteBuf, FakeChatPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FakeChatPacket::text,
            ByteBufCodecs.VAR_INT, FakeChatPacket::kind,
            FakeChatPacket::new);

    public static final int CHAT = 0;
    public static final int JOIN = 1;
    public static final int LEAVE = 2;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FakeChatPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.onFakeChat(pkt)));
    }
}
