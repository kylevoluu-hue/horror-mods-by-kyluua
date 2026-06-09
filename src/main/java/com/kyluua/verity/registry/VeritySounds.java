package com.kyluua.verity.registry;

import com.kyluua.verity.Verity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

/**
 * All sound events used by Verity.
 *
 * <p>Sounds are grouped by purpose. Crucially, voice lines are <b>tagged per
 * stage</b> so the dialogue system can pick audio whose <em>emotion</em> matches
 * Verity's current corruption:</p>
 * <ul>
 *   <li>{@code voice_letmeout} - the very first, excited "Hey, let me out!".</li>
 *   <li>{@code voice_friendly_*} - cheerful, warm, genuinely helpful.</li>
 *   <li>{@code voice_unsettling_*} - still friendly but subtly off / too knowing.</li>
 *   <li>{@code voice_psych_*} - strained, hesitant, half-whispered.</li>
 *   <li>{@code voice_hostile_*} - distorted, layered, wrong.</li>
 * </ul>
 *
 * <p>The actual {@code .ogg} files are placeholders in this first version; the
 * required recordings and their voice direction are documented in
 * {@code docs/ASSETS.md}. Paths here are mapped to files via {@code sounds.json}.</p>
 */
public final class VeritySounds {

    private VeritySounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Verity.MOD_ID);

    // --- Intro ---------------------------------------------------------------
    public static final Supplier<SoundEvent> VOICE_LETMEOUT = register("voice_letmeout");

    // --- Stage voice sets (multiple takes per stage for variety) -------------
    public static final Supplier<SoundEvent> VOICE_FRIENDLY_1 = register("voice_friendly_1");
    public static final Supplier<SoundEvent> VOICE_FRIENDLY_2 = register("voice_friendly_2");
    public static final Supplier<SoundEvent> VOICE_UNSETTLING_1 = register("voice_unsettling_1");
    public static final Supplier<SoundEvent> VOICE_UNSETTLING_2 = register("voice_unsettling_2");
    public static final Supplier<SoundEvent> VOICE_PSYCH_1 = register("voice_psych_1");
    public static final Supplier<SoundEvent> VOICE_PSYCH_2 = register("voice_psych_2");
    public static final Supplier<SoundEvent> VOICE_HOSTILE_1 = register("voice_hostile_1");
    public static final Supplier<SoundEvent> VOICE_HOSTILE_2 = register("voice_hostile_2");

    // --- Atmosphere ----------------------------------------------------------
    public static final Supplier<SoundEvent> WHISPER = register("whisper");
    public static final Supplier<SoundEvent> AMBIENT_DRONE = register("ambient_drone");
    public static final Supplier<SoundEvent> AMBIENT_DISTORTED = register("ambient_distorted");
    public static final Supplier<SoundEvent> HEARTBEAT = register("heartbeat");

    // --- Scares --------------------------------------------------------------
    public static final Supplier<SoundEvent> JUMPSCARE_STINGER = register("jumpscare_stinger");
    public static final Supplier<SoundEvent> GLITCH = register("glitch");

    // --- Boss ----------------------------------------------------------------
    public static final Supplier<SoundEvent> BOSS_SPAWN = register("boss_spawn");
    public static final Supplier<SoundEvent> BOSS_MUSIC = register("boss_music");
    public static final Supplier<SoundEvent> BOSS_ROAR = register("boss_roar");

    /** Helper: registers a SoundEvent under {@code verity:<name>}. */
    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, name)));
    }
}
