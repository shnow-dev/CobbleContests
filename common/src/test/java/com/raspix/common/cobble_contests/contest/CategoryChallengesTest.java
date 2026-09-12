package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryChallengesTest {
    @Test
    void beautyAlwaysProvidesThreeMatchingElementsAndThreeDecoys() {
        BeautyCompositionChallenge challenge = new BeautyCompositionChallenge(17L);
        int matches = 0;
        int packed = challenge.packedData();

        assertEquals(challenge.theme(), BeautyCompositionChallenge.themeFromPacked(packed));
        for (int index = 0; index < BeautyCompositionChallenge.OPTION_COUNT; index++) {
            if (challenge.matchesTheme(index)) {
                matches++;
            }
            assertEquals(challenge.optionAt(index),
                    BeautyCompositionChallenge.optionFromPacked(packed, index));
        }
        assertEquals(BeautyCompositionChallenge.PICK_COUNT, matches);
    }

    @Test
    void gracePathLengthScalesWithRankAndPackingIsLossless() {
        for (int rank = 0; rank <= 4; rank++) {
            GraceTracingChallenge challenge = new GraceTracingChallenge(33L, rank);
            Set<Integer> uniqueNodes = new HashSet<>();
            assertEquals(4 + rank, challenge.length());
            for (int index = 0; index < challenge.length(); index++) {
                assertEquals(challenge.nodeAt(index),
                        GraceTracingChallenge.nodeFromPacked(challenge.packedData(), index));
                assertTrue(uniqueNodes.add(challenge.nodeAt(index)));
            }
        }
    }

    @Test
    void smartSequenceScalesWithRankAndAvoidsImmediateRepeats() {
        SmartMemoryChallenge challenge = new SmartMemoryChallenge(55L, 4);
        assertEquals(SmartMemoryChallenge.MAX_SEQUENCE_LENGTH, challenge.length());
        for (int index = 0; index < challenge.length(); index++) {
            assertEquals(challenge.symbolAt(index),
                    SmartMemoryChallenge.symbolFromPacked(challenge.packedData(), index));
            if (index > 0) {
                assertNotEquals(challenge.symbolAt(index - 1), challenge.symbolAt(index));
            }
        }
    }

    @Test
    void toughThreatsScaleWithRankAndAvoidTheSameLaneTwice() {
        ToughProtectionChallenge challenge = new ToughProtectionChallenge(77L, 4);
        assertEquals(ToughProtectionChallenge.MAX_THREAT_COUNT, challenge.length());
        for (int index = 0; index < challenge.length(); index++) {
            assertEquals(challenge.laneAt(index),
                    ToughProtectionChallenge.laneFromPacked(challenge.packedData(), index));
            if (index > 0) {
                assertNotEquals(challenge.laneAt(index - 1), challenge.laneAt(index));
            }
        }
    }
}
