package com.kyluua.verity.client;

import com.kyluua.verity.progression.CorruptionStage;

/**
 * A tiny client-side cache of the server's corruption value. The
 * {@link ScreenEffectHandler} and fog hooks read this every frame to scale their
 * intensity, instead of querying the server. Updated by
 * {@link ClientPacketHandlers#onCorruptionSync}.
 */
public final class ClientCorruptionState {

    private ClientCorruptionState() {}

    private static volatile int level = 0;
    private static volatile CorruptionStage stage = CorruptionStage.FRIENDLY;

    public static void set(int newLevel, CorruptionStage newStage) {
        level = newLevel;
        stage = newStage;
    }

    public static int level() {
        return level;
    }

    public static CorruptionStage stage() {
        return stage;
    }

    /** 0.0-1.0 normalised corruption, handy for effect strength. */
    public static float normalized() {
        return Math.min(1.0f, level / 100.0f);
    }
}
