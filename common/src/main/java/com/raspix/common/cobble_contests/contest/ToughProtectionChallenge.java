package com.raspix.common.cobble_contests.contest;

import java.util.Random;

/** Deterministic sequence of threats aimed at three protected objects. */
public final class ToughProtectionChallenge {
    public static final int LANE_COUNT = 3;
    public static final int MIN_THREAT_COUNT = 4;
    public static final int MAX_THREAT_COUNT = 8;

    private final int[] threats;

    public ToughProtectionChallenge(long seed, int rank) {
        Random random = new Random(seed);
        threats = new int[lengthForRank(rank)];
        int previous = -1;
        for (int index = 0; index < threats.length; index++) {
            int lane = random.nextInt(LANE_COUNT);
            if (lane == previous) {
                lane = (lane + 1 + random.nextInt(LANE_COUNT - 1)) % LANE_COUNT;
            }
            threats[index] = lane;
            previous = lane;
        }
    }

    public int length() {
        return threats.length;
    }

    public int laneAt(int index) {
        if (index < 0 || index >= threats.length) {
            throw new IllegalArgumentException("Threat index is outside the challenge");
        }
        return threats[index];
    }

    public int packedData() {
        int packed = 0;
        for (int index = 0; index < threats.length; index++) {
            packed |= threats[index] << (index * 2);
        }
        return packed;
    }

    public static int laneFromPacked(int packed, int index) {
        if (index < 0 || index >= MAX_THREAT_COUNT) {
            throw new IllegalArgumentException("Threat index is outside the challenge");
        }
        return (packed >>> (index * 2)) & 3;
    }

    public static int lengthForRank(int rank) {
        return Math.min(MAX_THREAT_COUNT, MIN_THREAT_COUNT + Math.max(0, Math.min(4, rank)));
    }
}
