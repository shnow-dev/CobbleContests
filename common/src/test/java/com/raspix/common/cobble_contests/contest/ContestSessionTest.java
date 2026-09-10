package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestSessionTest {
    @Test
    void completesAllSixPhasesInOrder() {
        ContestSession session = new ContestSession(
                UUID.randomUUID(), 0, 0, 0, 80,
                List.of(
                        new ContestSession.MoveOption("tackle", 4, true),
                        new ContestSession.MoveOption("growl", 2, false),
                        new ContestSession.MoveOption("agility", 3, true),
                        new ContestSession.MoveOption("protect", 2, false)
                ),
                99L, 0L
        );

        assertEquals(ContestPhase.PRESENTATION, session.phase());
        assertTrue(session.act(0, 5L));
        assertTrue(session.act(1, 10L));
        assertTrue(session.act(2, 15L));
        assertEquals(ContestPhase.CAPABILITIES, session.phase());
        assertEquals(80, session.phaseScores()[ContestPhase.EVALUATION.ordinal()]);

        assertTrue(session.act(0, 20L));
        assertTrue(session.act(2, 21L));
        assertTrue(session.act(1, 22L));
        assertEquals(ContestPhase.CATEGORY, session.phase());

        CoolPrecisionChallenge challenge = new CoolPrecisionChallenge(99L);
        long tick = 23L;
        for (int index = 0; index < CoolPrecisionChallenge.TARGET_COUNT; index++) {
            if (challenge.targetAt(index) != CoolPrecisionChallenge.TargetType.DECOY) {
                assertTrue(session.act(index, tick++));
            }
        }
        assertEquals(ContestPhase.RHYTHM, session.phase());

        for (int beat = 0; beat < ContestSession.RHYTHM_ACTIONS; beat++) {
            assertTrue(session.act(beat, session.timingTargetAt()));
        }
        assertEquals(ContestPhase.FINALE, session.phase());
        assertTrue(session.act(0, session.timingTargetAt()));

        assertEquals(ContestPhase.RESULTS, session.phase());
        assertTrue(session.isFinished());
        assertTrue(session.totalScore() >= 0 && session.totalScore() <= 100);
    }

    @Test
    void timeoutAdvancesTheContestInsteadOfLeavingItBlocked() {
        ContestSession session = new ContestSession(
                UUID.randomUUID(), 0, 0, 0, 0, List.of(), 1L, 0L
        );

        assertTrue(session.tick(301L));
        assertEquals(ContestPhase.CAPABILITIES, session.phase());
    }
}
