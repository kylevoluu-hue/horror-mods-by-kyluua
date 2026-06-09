package com.kyluua.verity.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * A single line Verity can say.
 *
 * @param text    the message, may contain tokens like {player}, {encounters}, {structure}
 * @param weight  relative likelihood of being chosen (higher = more common)
 * @param voice   optional sound id (under verity:) to play with the line; "" for silent
 * @param emotion tone tag (CHEERFUL/CALM/UNEASY/STRAINED/DISTORTED) used for pitch shaping
 */
public record DialogueEntry(String text, int weight, String voice, String emotion) {

    /** Parses one entry from a JSON object, applying sensible defaults. */
    public static DialogueEntry fromJson(JsonObject obj) {
        String text = GsonHelper.getAsString(obj, "text");
        int weight = GsonHelper.getAsInt(obj, "weight", 1);
        String voice = GsonHelper.getAsString(obj, "voice", "");
        String emotion = GsonHelper.getAsString(obj, "emotion", "CALM");
        return new DialogueEntry(text, Math.max(1, weight), voice, emotion);
    }
}
