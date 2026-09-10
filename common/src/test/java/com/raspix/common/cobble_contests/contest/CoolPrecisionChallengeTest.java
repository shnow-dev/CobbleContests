package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoolPrecisionChallengeTest {
    @Test
    void alwaysContainsTheExpectedPermanentTargetMix() {
        CoolPrecisionChallenge challenge = new CoolPrecisionChallenge(42L);
        int regular = 0;
        int special = 0;
        int decoy = 0;
        int positiveMask = 0;

        for (int index = 0; index < CoolPrecisionChallenge.TARGET_COUNT; index++) {
            switch (challenge.targetAt(index)) {
                case REGULAR -> regular++;
                case SPECIAL -> special++;
                case DECOY -> decoy++;
            }
            if (challenge.targetAt(index) != CoolPrecisionChallenge.TargetType.DECOY) {
                positiveMask |= 1 << index;
            }
        }

        assertEquals(6, regular);
        assertEquals(2, special);
        assertEquals(4, decoy);
        assertFalse(challenge.allPositiveTargetsResolved(0));
        assertTrue(challenge.allPositiveTargetsResolved(positiveMask));
    }

    @Test
    void targetPackingCanBeDecodedWithoutLosingInformation() {
        CoolPrecisionChallenge challenge = new CoolPrecisionChallenge(123L);
        int packed = challenge.packedTargets();

        for (int index = 0; index < CoolPrecisionChallenge.TARGET_COUNT; index++) {
            int networkId = (packed >> (index * 2)) & 3;
            assertEquals(challenge.targetAt(index),
                    CoolPrecisionChallenge.TargetType.fromNetworkId(networkId));
        }
    }
}
