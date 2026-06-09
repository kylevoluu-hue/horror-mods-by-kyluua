package com.kyluua.verity.progression;

/**
 * The five narrative stages of Verity, derived from the hidden 0-100 corruption
 * level. The thresholds are intentionally weighted so the early, friendly stages
 * last the longest and the horror builds gradually.
 *
 * <p>Stage is a pure function of the corruption level, so it never needs to be
 * stored separately - {@link #fromLevel(int)} is the single source of truth.</p>
 */
public enum CorruptionStage {

    /** Stage 1 - genuinely helpful, cheerful, smiling normally. */
    FRIENDLY(0),

    /** Stage 2 - still helpful but watching, appearing behind you, knowing too much. */
    UNSETTLING(25),

    /** Stage 3 - psychological horror: fake chat, hallucinations, whispers, staring. */
    PSYCHOLOGICAL(50),

    /** Stage 4 - hostile: distorted face, darkness, rare jumpscares, paranoia. */
    HOSTILE(75),

    /** Final stage - the transformation into the humanoid boss. */
    FINAL(95);

    /** Inclusive lower bound (0-100) at which this stage begins. */
    public final int threshold;

    CorruptionStage(int threshold) {
        this.threshold = threshold;
    }

    /** Resolves the active stage for a given corruption level. */
    public static CorruptionStage fromLevel(int level) {
        CorruptionStage current = FRIENDLY;
        for (CorruptionStage stage : values()) {
            if (level >= stage.threshold) {
                current = stage;
            }
        }
        return current;
    }

    /** Stage index 1-5, handy for display and weighting maths. */
    public int displayNumber() {
        return ordinal() + 1;
    }

    /** True once Verity has crossed from "helpful" into openly horror territory. */
    public boolean isHorror() {
        return ordinal() >= PSYCHOLOGICAL.ordinal();
    }

    /**
     * 0.0-1.0 progress within the current stage, used to ramp effects smoothly
     * (e.g. fog density) instead of snapping at each threshold.
     */
    public float progressWithin(int level) {
        int next = (this == FINAL) ? 100 : values()[ordinal() + 1].threshold;
        int span = Math.max(1, next - threshold);
        return Math.min(1.0f, (level - threshold) / (float) span);
    }
}
