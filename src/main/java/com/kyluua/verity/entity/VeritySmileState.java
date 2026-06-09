package com.kyluua.verity.entity;

import com.kyluua.verity.Verity;
import com.kyluua.verity.progression.CorruptionStage;
import net.minecraft.resources.ResourceLocation;

/**
 * The visual face state of the companion. Each state maps to a different texture
 * so Verity visibly degrades from <em>innocent &amp; happy</em> to
 * <em>corrupt &amp; evil</em> as corruption rises. This value is synced to clients
 * (see {@link VerityCompanionEntity}) so every player sees the same face.
 */
public enum VeritySmileState {

    /** Bright, cheerful, classic smiley (stage 1). */
    NORMAL("verity_smile_normal"),

    /** Crooked grin, the occasional eye twitch (stage 2). */
    UNSETTLING("verity_smile_unsettling"),

    /** Melting / cracked grin, colder hue (stage 3). */
    DISTORTED("verity_smile_distorted"),

    /** Jagged, red-shifted, openly wrong (stage 4). */
    HOSTILE("verity_smile_hostile");

    private final ResourceLocation texture;

    VeritySmileState(String textureName) {
        this.texture = ResourceLocation.fromNamespaceAndPath(
                Verity.MOD_ID, "textures/entity/" + textureName + ".png");
    }

    /** The entity texture for this face. */
    public ResourceLocation texture() {
        return texture;
    }

    /** Maps a corruption stage to the matching face. FINAL reuses HOSTILE for the brief pre-transform moment. */
    public static VeritySmileState forStage(CorruptionStage stage) {
        return switch (stage) {
            case FRIENDLY -> NORMAL;
            case UNSETTLING -> UNSETTLING;
            case PSYCHOLOGICAL -> DISTORTED;
            case HOSTILE, FINAL -> HOSTILE;
        };
    }
}
