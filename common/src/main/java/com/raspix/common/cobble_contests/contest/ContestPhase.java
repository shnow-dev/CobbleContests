package com.raspix.common.cobble_contests.contest;

/**
 * Permanent phases of a Cobble Contest. Scores are always expressed on a
 * 0-100 scale before the phase weight is applied.
 */
public enum ContestPhase {
    PRESENTATION(5),
    EVALUATION(40),
    CAPABILITIES(15),
    CATEGORY(20),
    RHYTHM(15),
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
