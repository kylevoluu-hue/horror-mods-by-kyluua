package com.kyluua.verity.client;

import com.kyluua.verity.VerityConfig;
import com.kyluua.verity.entity.HallucinationEntity;
import com.kyluua.verity.network.CorruptionSyncPacket;
import com.kyluua.verity.network.FakeChatPacket;
import com.kyluua.verity.network.HallucinationPacket;
import com.kyluua.verity.network.ScreenEffectPacket;
import com.kyluua.verity.network.WhisperPacket;
import com.kyluua.verity.progression.CorruptionStage;
import com.kyluua.verity.registry.VerityEntities;
import com.kyluua.verity.registry.VeritySounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * All client-side reactions to Verity's network packets. Every method here runs on
 * the client main thread (the packet handlers enqueue work first) and is reached
 * only on the physical client - the dedicated server never loads this class.
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    /** Counter for synthetic, client-only entity ids (negative so they never clash with server ids). */
    private static final AtomicInteger FAKE_ID = new AtomicInteger(-2_000_000);

    // --- Corruption sync -----------------------------------------------------
    public static void onCorruptionSync(CorruptionSyncPacket pkt) {
        ClientCorruptionState.set(pkt.level(), CorruptionStage.values()[pkt.stage()]);
    }

    // --- Screen effects ------------------------------------------------------
    public static void onScreenEffect(ScreenEffectPacket pkt) {
        if (VerityConfig.CLIENT.screenEffectIntensity.get() <= 0.0) return;
        ScreenEffectHandler.trigger(pkt.effect(), pkt.intensity(), pkt.durationTicks());
        // Jumpscares pair the flash with a stinger.
        if (pkt.effect() == ScreenEffectPacket.JUMPSCARE) {
            playLocal(VeritySounds.JUMPSCARE_STINGER.get(), 1.0F, 1.0F);
        }
    }

    // --- Hallucinations ------------------------------------------------------
    public static void onHallucination(HallucinationPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !VerityConfig.SERVER.enableHallucinations.get()) return;

        HallucinationEntity ghost = VerityEntities.HALLUCINATION.get().create(level);
        if (ghost == null) return;
        ghost.setLifespan(pkt.lifespan());
        ghost.moveTo(pkt.x(), pkt.y(), pkt.z(), 0.0F, 0.0F);
        // Inject directly into the client level under a unique negative id so only
        // this player ever sees it.
        int id = FAKE_ID.getAndDecrement();
        ghost.setId(id);
        level.putNonPlayerEntity(id, ghost);
    }

    // --- Fake chat / join / leave -------------------------------------------
    public static void onFakeChat(FakeChatPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        Component msg = switch (pkt.kind()) {
            case FakeChatPacket.JOIN -> Component.translatable("multiplayer.player.joined", pkt.text())
                    .withStyle(ChatFormatting.YELLOW);
            case FakeChatPacket.LEAVE -> Component.translatable("multiplayer.player.left", pkt.text())
                    .withStyle(ChatFormatting.YELLOW);
            default -> Component.literal(pkt.text());
        };
        mc.gui.getChat().addMessage(msg);
    }

    // --- Whispers ------------------------------------------------------------
    public static void onWhisper(WhisperPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SoundEvent sound = whisperFor(pkt.soundId());
        mc.level.playLocalSound(pkt.x(), pkt.y(), pkt.z(), sound, SoundSource.AMBIENT,
                (float) (pkt.volume() * VerityConfig.CLIENT.horrorVolume.get()), pkt.pitch(), false);
    }

    // --- helpers -------------------------------------------------------------
    private static SoundEvent whisperFor(int id) {
        return switch (id) {
            case 1 -> VeritySounds.AMBIENT_DRONE.get();
            case 2 -> VeritySounds.AMBIENT_DISTORTED.get();
            case 3 -> VeritySounds.HEARTBEAT.get();
            case 4 -> VeritySounds.GLITCH.get();
            default -> VeritySounds.WHISPER.get();
        };
    }

    private static void playLocal(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float v = (float) (volume * VerityConfig.CLIENT.horrorVolume.get());
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                sound, SoundSource.MASTER, v, pitch, false);
    }
}
