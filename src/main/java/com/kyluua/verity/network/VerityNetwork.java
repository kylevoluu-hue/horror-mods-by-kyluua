package com.kyluua.verity.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Central networking hub. Registers every payload on the mod event bus and exposes
 * small, well-named send helpers so the rest of the mod never touches
 * {@link PacketDistributor} directly.
 *
 * <p>Direction is enforced at registration time: S->C payloads use
 * {@code playToClient}, the one C->S payload uses {@code playToServer}. Handlers run
 * on the main thread, and the S->C handlers defer to client-only classes via
 * {@code DistExecutor} so a dedicated server never loads rendering code.</p>
 */
public final class VerityNetwork {

    private VerityNetwork() {}

    /** Bump this string when the packet formats change in a breaking way. */
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Server -> client
        registrar.playToClient(CorruptionSyncPacket.TYPE, CorruptionSyncPacket.STREAM_CODEC, CorruptionSyncPacket::handle);
        registrar.playToClient(ScreenEffectPacket.TYPE, ScreenEffectPacket.STREAM_CODEC, ScreenEffectPacket::handle);
        registrar.playToClient(HallucinationPacket.TYPE, HallucinationPacket.STREAM_CODEC, HallucinationPacket::handle);
        registrar.playToClient(FakeChatPacket.TYPE, FakeChatPacket.STREAM_CODEC, FakeChatPacket::handle);
        registrar.playToClient(WhisperPacket.TYPE, WhisperPacket.STREAM_CODEC, WhisperPacket::handle);

        // Client -> server
        registrar.playToServer(AskQuestionPacket.TYPE, AskQuestionPacket.STREAM_CODEC, AskQuestionPacket::handle);
    }

    // =========================================================================
    //  Send helpers
    // =========================================================================

    /** Sends a payload to one player. */
    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /** Broadcasts a payload to everyone on the server (server-wide events). */
    public static void toAll(MinecraftServer server, CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
