package com.raspix.common.cobble_contests.contest;

import java.util.Random;

/** Increasing symbol sequence used by the Intelligence memory puzzle. */
public final class SmartMemoryChallenge {
    public static final int SYMBOL_COUNT = 4;
    public static final int MIN_SEQUENCE_LENGTH = 4;
    public static final int MAX_SEQUENCE_LENGTH = 8;

    private final int[] sequence;

    public SmartMemoryChallenge(long seed, int rank) {
        Random random = new Random(seed);
        sequence = new int[lengthForRank(rank)];
        int previous = -1;
        for (int index = 0; index < sequence.length; index++) {
            int symbol = random.nextInt(SYMBOL_COUNT);
            if (symbol == previous) {
                symbol = (symbol + 1 + random.nextInt(SYMBOL_COUNT - 1)) % SYMBOL_COUNT;
            }
            sequence[index] = symbol;
            previous = symbol;
        }
    }

    public int length() {
        return sequence.length;
    }

    public int symbolAt(int index) {
        if (index < 0 || index >= sequence.length) {
            throw new IllegalArgumentException("Memory sequence index is outside the challenge");
        }
        return sequence[index];
    }

    public int pointsPerSymbol() {
        return (int) Math.ceil(100.0D / sequence.length);
    }

    public int packedData() {
        int packed = 0;
        for (int index = 0; index < sequence.length; index++) {
            packed |= sequence[index] << (index * 2);
        }
        return packed;
    }

    public static int symbolFromPacked(int packed, int index) {
        if (index < 0 || index >= MAX_SEQUENCE_LENGTH) {
            throw new IllegalArgumentException("Memory sequence index is outside the challenge");
        }
        return (packed >>> (index * 2)) & 3;
    }

    public static int lengthForRank(int rank) {
        return Math.min(MAX_SEQUENCE_LENGTH, MIN_SEQUENCE_LENGTH + Math.max(0, Math.min(4, rank)));
    }
}
