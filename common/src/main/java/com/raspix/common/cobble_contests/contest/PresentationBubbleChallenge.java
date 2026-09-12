package com.raspix.common.cobble_contests.contest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic, server-owned bubble waves used by the presentation test.
 * The client receives only the unresolved bubbles in the current wave and
 * never chooses their type, position or score.
 */
public final class PresentationBubbleChallenge {
    public static final int POSITIVE_COUNT = 15;
    public static final int NEGATIVE_COUNT = 6;
    public static final int NEUTRAL_COUNT = 6;
    public static final int BUBBLE_COUNT = POSITIVE_COUNT + NEGATIVE_COUNT + NEUTRAL_COUNT;
    public static final int MAX_GAUGE = POSITIVE_COUNT;
    public static final int MIN_ACTIVE_BUBBLES = 1;
    public static final int MAX_ACTIVE_BUBBLES = 5;

    private static final long BASE_WAVE_LIFETIME_TICKS = 30L;
    private static final long EXTRA_TICKS_PER_BUBBLE = 6L;
    private static final int MINIMUM_DISTANCE_SQUARED = 13 * 13;

    private final BubbleType[] types = new BubbleType[BUBBLE_COUNT];
    private final int[] xPositions = new int[BUBBLE_COUNT];
    private final int[] yPositions = new int[BUBBLE_COUNT];
    private final int[][] packedWaves;

    public PresentationBubbleChallenge(long seed) {
        Random random = new Random(seed ^ 0x43E2D9A71C6B5F08L);
        List<BubbleType> shuffled = new ArrayList<>(BUBBLE_COUNT);
        add(shuffled, BubbleType.POSITIVE_HEART, POSITIVE_COUNT);
        add(shuffled, BubbleType.NEGATIVE_HEART, NEGATIVE_COUNT);
        add(shuffled, BubbleType.BORED, NEUTRAL_COUNT);
        Collections.shuffle(shuffled, random);

        List<int[]> waves = new ArrayList<>();
        int bubbleIndex = 0;
        while (bubbleIndex < BUBBLE_COUNT) {
            int remaining = BUBBLE_COUNT - bubbleIndex;
            int waveSize = Math.min(remaining,
                    MIN_ACTIVE_BUBBLES + random.nextInt(MAX_ACTIVE_BUBBLES));
            int[] wave = new int[waveSize];
            for (int slot = 0; slot < waveSize; slot++) {
                types[bubbleIndex] = shuffled.get(bubbleIndex);
                int[] position = createPosition(random, wave, slot);
                xPositions[bubbleIndex] = position[0];
                yPositions[bubbleIndex] = position[1];
                wave[slot] = pack(bubbleIndex);
                bubbleIndex++;
            }
            waves.add(wave);
        }
        packedWaves = waves.toArray(int[][]::new);
    }

    private static void add(List<BubbleType> target, BubbleType type, int count) {
        for (int index = 0; index < count; index++) {
            target.add(type);
        }
    }

    private static int[] createPosition(Random random, int[] currentWave, int populatedSlots) {
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = 12 + random.nextInt(77);
            int y = 24 + random.nextInt(48);
            if (isFarEnoughFromOtherBubbles(currentWave, populatedSlots, x, y)) {
                return new int[]{x, y};
            }
        }
        return new int[]{18 + populatedSlots * 16, 32 + (populatedSlots % 2) * 24};
    }

    private static boolean isFarEnoughFromOtherBubbles(int[] wave, int populatedSlots,
                                                         int x, int y) {
        for (int slot = 0; slot < populatedSlots; slot++) {
            int dx = unpackX(wave[slot]) - x;
            int dy = unpackY(wave[slot]) - y;
            if (dx * dx + dy * dy < MINIMUM_DISTANCE_SQUARED) {
                return false;
            }
        }
        return true;
    }

    private int pack(int bubbleIndex) {
        return types[bubbleIndex].ordinal()
                | (xPositions[bubbleIndex] << 2)
                | (yPositions[bubbleIndex] << 10)
                | (bubbleIndex << 18);
    }

    public int waveCount() {
        return packedWaves.length;
    }

    public int waveSize(int waveIndex) {
        return packedWaves[checkedWaveIndex(waveIndex)].length;
    }

    public int[] packedWave(int waveIndex) {
        int[] wave = packedWaves[checkedWaveIndex(waveIndex)];
        return Arrays.copyOf(wave, wave.length);
    }

    public long waveLifetimeTicks(int waveIndex) {
        return BASE_WAVE_LIFETIME_TICKS
                + EXTRA_TICKS_PER_BUBBLE * waveSize(waveIndex);
    }

    public long totalLifetimeTicks() {
        long total = 0L;
        for (int wave = 0; wave < waveCount(); wave++) {
            total += waveLifetimeTicks(wave);
        }
        return total;
    }

    public BubbleType typeAt(int bubbleIndex) {
        return types[checkedBubbleIndex(bubbleIndex)];
    }

    public int xAt(int bubbleIndex) {
        return xPositions[checkedBubbleIndex(bubbleIndex)];
    }

    public int yAt(int bubbleIndex) {
        return yPositions[checkedBubbleIndex(bubbleIndex)];
    }

    public static BubbleType unpackType(int packed) {
        int ordinal = packed & 3;
        return ordinal >= 0 && ordinal < BubbleType.values().length
                ? BubbleType.values()[ordinal]
                : BubbleType.BORED;
    }

    public static int unpackX(int packed) {
        return (packed >> 2) & 0xFF;
    }

    public static int unpackY(int packed) {
        return (packed >> 10) & 0xFF;
    }

    public static int unpackBubbleIndex(int packed) {
        return (packed >> 18) & 0x3F;
    }

    public static int apply(BubbleType type, int gauge) {
        int delta = switch (type) {
            case POSITIVE_HEART -> 1;
            case NEGATIVE_HEART -> -1;
            case BORED -> 0;
        };
        return Math.max(0, Math.min(MAX_GAUGE, gauge + delta));
    }

    public static int scoreForGauge(int gauge) {
        int clamped = Math.max(0, Math.min(MAX_GAUGE, gauge));
        return Math.round(clamped * 100.0F / MAX_GAUGE);
    }

    private int checkedWaveIndex(int waveIndex) {
        if (waveIndex < 0 || waveIndex >= packedWaves.length) {
            throw new IndexOutOfBoundsException("Presentation wave index " + waveIndex);
        }
        return waveIndex;
    }

    private static int checkedBubbleIndex(int bubbleIndex) {
        if (bubbleIndex < 0 || bubbleIndex >= BUBBLE_COUNT) {
            throw new IndexOutOfBoundsException("Presentation bubble index " + bubbleIndex);
        }
        return bubbleIndex;
    }

    public enum BubbleType {
        POSITIVE_HEART,
        NEGATIVE_HEART,
        BORED
    }
}
