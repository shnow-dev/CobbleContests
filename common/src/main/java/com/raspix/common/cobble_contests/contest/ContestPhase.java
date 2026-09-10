package com.raspix.common.cobble_contests.contest;

/**
 * Permanent phases of a Cobble Contest. Scores are always expressed on a
 * 0-100 scale before the phase weight is applied.
 */
public enum ContestPhase {
    PRESENTATION(5),
    EVALUATION(20),
    CAPABILITIES(25),
    CATEGORY(25),
    RHYTHM(20),
    FINALE(5),
    RESULTS(0);

    private final int weight;

    ContestPhase(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean isScored() {
        return this != RESULTS;
    }
}
