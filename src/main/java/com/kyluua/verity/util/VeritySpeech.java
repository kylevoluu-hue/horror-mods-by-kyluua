package com.kyluua.verity.util;

import com.kyluua.verity.Verity;
import com.kyluua.verity.dialogue.DialogueEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;

/**
 * Helper for making Verity "speak": pushes a styled chat line to a player and plays
 * the matching voice line. Centralised so every system (box opening, dialogue,
 * scares) presents Verity's voice consistently.
 */
public final class VeritySpeech {

    private VeritySpeech() {}

    /** Verity's chat name prefix - yellow while friendly. */
    private static Component prefix(boolean corrupted) {
        return Component.literal("Verity")
                .withStyle(corrupted ? ChatFormatting.DARK_RED : ChatFormatting.YELLOW)
                .append(Component.literal(" » ").withStyle(ChatFormatting.GRAY));
    }

    /**
     * Sends a dialogue line to one player and plays its voice line aloud at that
     * player's position (so nearby players hear Verity talking too).
     *
     * @param corrupted whether to style the name as corrupted (stage 3+)
     */
    public static void speak(ServerPlayer player, DialogueEntry line, boolean corrupted) {
        player.sendSystemMessage(prefix(corrupted).copy().append(
                Component.literal(line.text()).withStyle(corrupted ? ChatFormatting.RED : ChatFormatting.WHITE)));

        SoundEvent voice = lookupSound(line.voice());
        if (voice != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    voice, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    /** Sends a raw (non-dialogue) line from Verity to one player. */
    public static void say(ServerPlayer player, String text, boolean corrupted) {
        player.sendSystemMessage(prefix(corrupted).copy().append(
                Component.literal(text).withStyle(corrupted ? ChatFormatting.RED : ChatFormatting.WHITE)));
    }

    /** Resolves a {@code verity:<name>} sound by id, or null if blank/unknown. */
    @Nullable
    public static SoundEvent lookupSound(@Nullable String name) {
        if (name == null || name.isBlank()) return null;
        return BuiltInRegistries.SOUND_EVENT.getOptional(
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, name)).orElse(null);
    }
}
