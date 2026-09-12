package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationBubbleChallengeTest {
    @Test
    void wavesHaveTheDesignedDistributionAndStablePacking() {
        PresentationBubbleChallenge challenge = new PresentationBubbleChallenge(42L);
        EnumMap<PresentationBubbleChallenge.BubbleType, Integer> counts =
                new EnumMap<>(PresentationBubbleChallenge.BubbleType.class);
        boolean[] seen = new boolean[PresentationBubbleChallenge.BUBBLE_COUNT];
        int total = 0;

        for (int wave = 0; wave < challenge.waveCount(); wave++) {
            int[] packedWave = challenge.packedWave(wave);
            assertTrue(packedWave.length >= PresentationBubbleChallenge.MIN_ACTIVE_BUBBLES);
            assertTrue(packedWave.length <= PresentationBubbleChallenge.MAX_ACTIVE_BUBBLES);
            for (int packed : packedWave) {
                int index = PresentationBubbleChallenge.unpackBubbleIndex(packed);
                assertFalse(seen[index]);
                seen[index] = true;
                total++;
                counts.merge(PresentationBubbleChallenge.unpackType(packed), 1, Integer::sum);
                assertEquals(challenge.typeAt(index), PresentationBubbleChallenge.unpackType(packed));
                assertEquals(challenge.xAt(index), PresentationBubbleChallenge.unpackX(packed));
                assertEquals(challenge.yAt(index), PresentationBubbleChallenge.unpackY(packed));
                assertTrue(challenge.xAt(index) >= 12 && challenge.xAt(index) <= 88);
                assertTrue(challenge.yAt(index) >= 24 && challenge.yAt(index) <= 71);
            }
        }

        assertEquals(PresentationBubbleChallenge.BUBBLE_COUNT, total);
        assertEquals(PresentationBubbleChallenge.POSITIVE_COUNT,
                counts.get(PresentationBubbleChallenge.BubbleType.POSITIVE_HEART).intValue());
        assertEquals(PresentationBubbleChallenge.NEGATIVE_COUNT,
                counts.get(PresentationBubbleChallenge.BubbleType.NEGATIVE_HEART).intValue());
        assertEquals(PresentationBubbleChallenge.NEUTRAL_COUNT,
                counts.get(PresentationBubbleChallenge.BubbleType.BORED).intValue());
    }

    @Test
    void gaugeAndScoreAreClamped() {
        assertEquals(1, PresentationBubbleChallenge.apply(
                PresentationBubbleChallenge.BubbleType.POSITIVE_HEART, 0));
        assertEquals(0, PresentationBubbleChallenge.apply(
                PresentationBubbleChallenge.BubbleType.NEGATIVE_HEART, 0));
        assertEquals(7, PresentationBubbleChallenge.apply(
                PresentationBubbleChallenge.BubbleType.BORED, 7));
        assertEquals(15, PresentationBubbleChallenge.apply(
                PresentationBubbleChallenge.BubbleType.POSITIVE_HEART, 15));
        assertEquals(100, PresentationBubbleChallenge.scoreForGauge(15));
        assertEquals(0, PresentationBubbleChallenge.scoreForGauge(-5));
    }
}
