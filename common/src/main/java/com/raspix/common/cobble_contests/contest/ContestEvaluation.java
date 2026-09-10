package com.raspix.common.cobble_contests.contest;

/** Converts a Pokémon contest condition into the stricter evaluation score. */
public final class ContestEvaluation {
    public static final int MAX_CONDITION = 255;

    private ContestEvaluation() {
    }

    /**
     * Uses a quadratic curve so partially trained Pokémon earn substantially
     * less than fully prepared ones. The returned score is always 0-100.
     */
    public static int scoreForCondition(int condition) {
        int clamped = Math.max(0, Math.min(MAX_CONDITION, condition));
        double ratio = clamped / (double) MAX_CONDITION;
        return ContestScore.clamp((int) Math.round(ratio * ratio * 100.0D));
    }
}
