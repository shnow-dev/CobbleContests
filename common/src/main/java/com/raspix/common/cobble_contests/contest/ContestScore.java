package com.raspix.common.cobble_contests.contest;

import java.util.Arrays;

/** Stores and combines the six contest phase scores. */
public final class ContestScore {
    private static final int SCORED_PHASE_COUNT = 6;

    private final int[] phaseScores = new int[SCORED_PHASE_COUNT];

    public void set(ContestPhase phase, int score) {
        requireScoredPhase(phase);
        phaseScores[phase.ordinal()] = clamp(score);
    }

    public int get(ContestPhase phase) {
        requireScoredPhase(phase);
        return phaseScores[phase.ordinal()];
    }

    public int total() {
        int weightedTotal = 0;
        for (ContestPhase phase : ContestPhase.values()) {
            if (phase.isScored()) {
                weightedTotal += get(phase) * phase.weight();
            }
        }
        return Math.round(weightedTotal / 100.0F);
    }

    public int[] snapshot() {
        return Arrays.copyOf(phaseScores, phaseScores.length);
    }

    public static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static void requireScoredPhase(ContestPhase phase) {
        if (phase == null || !phase.isScored()) {
            throw new IllegalArgumentException("A scored contest phase is required");
        }
    }
}
