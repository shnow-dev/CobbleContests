package com.raspix.common.cobble_contests.contest;

import java.util.Random;

/** Ordered path challenge drawn on a three-by-three stage grid. */
public final class GraceTracingChallenge {
    public static final int NODE_COUNT = 9;
    public static final int MIN_PATH_LENGTH = 4;
    public static final int MAX_PATH_LENGTH = 8;

    private static final int[][] PATHS = {
            {0, 1, 2, 5, 8, 7, 6, 3},
            {0, 3, 6, 7, 4, 1, 2, 5},
            {6, 3, 0, 4, 2, 5, 8, 7},
            {2, 1, 0, 3, 4, 5, 8, 7},
            {0, 4, 2, 5, 8, 7, 6, 3}
    };

    private final int[] path;

    public GraceTracingChallenge(long seed, int rank) {
        int length = Math.min(MAX_PATH_LENGTH, MIN_PATH_LENGTH + clampRank(rank));
        int[] selected = PATHS[new Random(seed).nextInt(PATHS.length)];
        path = new int[length];
        System.arraycopy(selected, 0, path, 0, length);
    }

    public int length() {
        return path.length;
    }

    public int nodeAt(int index) {
        if (index < 0 || index >= path.length) {
            throw new IllegalArgumentException("Grace path index is outside the challenge");
        }
        return path[index];
    }

    public int pointsPerNode() {
        return (int) Math.ceil(100.0D / path.length);
    }

    public int packedData() {
        int packed = 0;
        for (int index = 0; index < path.length; index++) {
            packed |= path[index] << (index * 4);
        }
        return packed;
    }

    public static int nodeFromPacked(int packed, int index) {
        if (index < 0 || index >= MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("Grace path index is outside the challenge");
        }
        return (packed >>> (index * 4)) & 15;
    }

    public static int lengthForRank(int rank) {
        return Math.min(MAX_PATH_LENGTH, MIN_PATH_LENGTH + clampRank(rank));
    }

    private static int clampRank(int rank) {
        return Math.max(0, Math.min(4, rank));
    }
}
