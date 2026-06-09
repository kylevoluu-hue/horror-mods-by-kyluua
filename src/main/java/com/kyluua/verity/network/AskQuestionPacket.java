package com.kyluua.verity.network;

import com.kyluua.verity.Verity;
import com.kyluua.verity.dialogue.DialogueEntry;
import com.kyluua.verity.dialogue.DialogueManager;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import com.kyluua.verity.util.VeritySpeech;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.handling.IPayloadContext;

/**
 * Client -> server. The player asked Verity something through the chat UI. The
 * server picks a stage-appropriate, weighted, anti-repeat answer, charges a little
 * corruption for the interaction, and speaks the reply back.
 */
public record AskQuestionPacket(String question) implements CustomPacketPayload {

    public static final Type<AskQuestionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "ask_question"));

    public static final StreamCodec<ByteBuf, AskQuestionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AskQuestionPacket::question,
            AskQuestionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server-side handler - safe to reference server/common classes only. */
    public static void handle(AskQuestionPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            CorruptionData data = CorruptionData.get(level);
            data.addInteraction(1.0D);        // talking to Verity feeds the corruption
            data.recordEncounter(player);     // and Verity remembers you

            CorruptionStage stage = data.getStage();
            DialogueEntry reply = DialogueManager.get().pickForStage(stage, player, data);
            if (reply != null) {
                VeritySpeech.speak(player, reply, stage.isHorror());
            }
        });
    }
}
