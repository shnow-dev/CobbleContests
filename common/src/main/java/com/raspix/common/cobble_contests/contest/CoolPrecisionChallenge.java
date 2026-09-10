package com.raspix.common.cobble_contests.contest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic target layout for the Cool/Sang-froid precision challenge.
 * Six regular targets and two special targets make a perfect score of 100;
 * four decoys punish careless shots.
 */
public final class CoolPrecisionChallenge {
    public static final int TARGET_COUNT = 12;

    private final TargetType[] targets;

    public CoolPrecisionChallenge(long seed) {
        List<TargetType> generated = new ArrayList<>(TARGET_COUNT);
        generated.addAll(Collections.nCopies(6, TargetType.REGULAR));
        generated.addAll(Collections.nCopies(2, TargetType.SPECIAL));
        generated.addAll(Collections.nCopies(4, TargetType.DECOY));
        Collections.shuffle(generated, new Random(seed));
        this.targets = generated.toArray(TargetType[]::new);
    }

    public TargetType targetAt(int index) {
        if (index < 0 || index >= targets.length) {
            throw new IllegalArgumentException("Target index is outside the challenge");
        }
        return targets[index];
    }

    public int packedTargets() {
        int packed = 0;
        for (int index = 0; index < targets.length; index++) {
            packed |= targets[index].networkId() << (index * 2);
        }
        return packed;
    }

    public boolean allPositiveTargetsResolved(int resolvedMask) {
        for (int index = 0; index < targets.length; index++) {
            if (targets[index] != TargetType.DECOY && (resolvedMask & (1 << index)) == 0) {
                return false;
            }
        }
        return true;
    }

    public enum TargetType {
        REGULAR(0, 10),
        SPECIAL(1, 20),
        DECOY(2, -10);

        private final int networkId;
        private final int points;

        TargetType(int networkId, int points) {
            this.networkId = networkId;
            this.points = points;
        }

        public int networkId() {
            return networkId;
        }

        public int points() {
            return points;
        }

        public static TargetType fromNetworkId(int id) {
            return switch (id) {
                case 0 -> REGULAR;
                case 1 -> SPECIAL;
                case 2 -> DECOY;
                default -> throw new IllegalArgumentException("Unknown target type " + id);
            };
        }
    }
}
