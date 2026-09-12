package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(ContestPhase.EVALUATION, session.phase());
        assertTrue(session.tick(56L));
        assertEquals(ContestPhase.CAPABILITIES, session.phase());
        assertEquals(80, session.phaseScores()[ContestPhase.EVALUATION.ordinal()]);

        assertTrue(session.act(0, 60L));
        assertTrue(session.act(2, 61L));
        assertTrue(session.act(1, 62L));
        assertEquals(ContestPhase.CATEGORY, session.phase());

        CoolPrecisionChallenge challenge = new CoolPrecisionChallenge(99L);
        long tick = 63L;
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
        assertEquals(ContestPhase.EVALUATION, session.phase());
        assertTrue(session.tick(342L));
        assertEquals(ContestPhase.CAPABILITIES, session.phase());
    }

    @Test
    void presentationPreviewStopsWithoutRunningOrRewardingLaterPhases() {
        long seed = 42L;
        ContestSession session = new ContestSession(
                UUID.randomUUID(), 0, ContestSession.COOL_CATEGORY, 0, 80,
                List.of(), seed, 0L, true
        );
        while (session.phase() == ContestPhase.PRESENTATION) {
            int wave = session.progress();
            long deadline = session.timingTargetAt();
            for (int packed : session.presentationTargets()) {
                if (PresentationBubbleChallenge.unpackType(packed)
                        == PresentationBubbleChallenge.BubbleType.POSITIVE_HEART) {
                    assertTrue(session.act(
                            PresentationBubbleChallenge.unpackBubbleIndex(packed),
                            Math.max(41L, deadline - 1L)
                    ));
                }
            }
            if (session.phase() == ContestPhase.PRESENTATION && session.progress() == wave) {
                assertTrue(session.tick(deadline));
            }
        }

        assertEquals(ContestPhase.RESULTS, session.phase());
        assertEquals(100, session.phaseScores()[ContestPhase.PRESENTATION.ordinal()]);
        assertEquals(PresentationBubbleChallenge.MAX_GAUGE, session.resolvedTargets());
        assertEquals(0, session.phaseScores()[ContestPhase.EVALUATION.ordinal()]);
        assertTrue(session.presentationOnly());
    }

    @Test
    void presentationPreviewOnlyAcceptsTheCurrentBubbleWhileItIsVisible() {
        ContestSession session = new ContestSession(
                UUID.randomUUID(), 0, ContestSession.COOL_CATEGORY, 0, 80,
                List.of(), 42L, 0L, true
        );

        assertFalse(session.act(0, 39L));
        assertFalse(session.act(99, 41L));
        int currentBubble = PresentationBubbleChallenge.unpackBubbleIndex(
                session.presentationTargets()[0]
        );
        assertFalse(session.act(currentBubble, session.timingTargetAt()));
        assertEquals(0, session.progress());

        assertTrue(session.tick(session.timingTargetAt()));
        assertEquals(1, session.progress());
    }

    @Test
    void beautyCompositionUsesTheServerTheme() {
        long seed = 17L;
        ContestSession session = advanceToCategory(ContestSession.BEAUTY_CATEGORY, 0, seed);
        BeautyCompositionChallenge challenge = new BeautyCompositionChallenge(seed);
        long tick = 50L;
        for (int index = 0; index < BeautyCompositionChallenge.OPTION_COUNT; index++) {
            if (challenge.matchesTheme(index)) {
                assertTrue(session.act(index, tick++));
            }
        }
        assertEquals(ContestPhase.RHYTHM, session.phase());
        assertEquals(100, session.phaseScores()[ContestPhase.CATEGORY.ordinal()]);
    }

    @Test
    void graceTracingRequiresTheOrderedPath() {
        long seed = 33L;
        int rank = 2;
        ContestSession session = advanceToCategory(ContestSession.GRACE_CATEGORY, rank, seed);
        GraceTracingChallenge challenge = new GraceTracingChallenge(seed, rank);

        assertTrue(session.act((challenge.nodeAt(0) + 1) % GraceTracingChallenge.NODE_COUNT, 50L));
        assertEquals(0, session.progress());
        for (int index = 0; index < challenge.length(); index++) {
            assertTrue(session.act(challenge.nodeAt(index), 51L + index));
        }
        assertEquals(ContestPhase.RHYTHM, session.phase());
        assertTrue(session.phaseScores()[ContestPhase.CATEGORY.ordinal()] < 100);
    }

    @Test
    void intelligenceHidesAValidatedMemorySequenceBehindThePreview() {
        long seed = 55L;
        int rank = 1;
        ContestSession session = advanceToCategory(ContestSession.SMART_CATEGORY, rank, seed);
        SmartMemoryChallenge challenge = new SmartMemoryChallenge(seed, rank);

        assertFalse(session.act(challenge.symbolAt(0), 100L));
        for (int index = 0; index < challenge.length(); index++) {
            assertTrue(session.act(challenge.symbolAt(index), 110L + index));
        }
        assertEquals(ContestPhase.RHYTHM, session.phase());
        assertEquals(100, session.phaseScores()[ContestPhase.CATEGORY.ordinal()]);
    }

    @Test
    void toughnessScoresCorrectAndFastProtections() {
        long seed = 77L;
        int rank = 3;
        ContestSession session = advanceToCategory(ContestSession.TOUGH_CATEGORY, rank, seed);
        ToughProtectionChallenge challenge = new ToughProtectionChallenge(seed, rank);
        long tick = 50L;
        for (int index = 0; index < challenge.length(); index++) {
            assertTrue(session.act(challenge.laneAt(index), tick));
            tick += 5L;
        }
        assertEquals(ContestPhase.RHYTHM, session.phase());
        assertEquals(100, session.phaseScores()[ContestPhase.CATEGORY.ordinal()]);
    }

    private static ContestSession advanceToCategory(int category, int rank, long seed) {
        ContestSession session = new ContestSession(
                UUID.randomUUID(), 0, category, rank, 80,
                List.of(new ContestSession.MoveOption("tackle", 4, true)), seed, 0L
        );
        assertTrue(session.act(0, 1L));
        assertTrue(session.act(1, 2L));
        assertTrue(session.act(2, 3L));
        assertTrue(session.tick(44L));
        assertTrue(session.act(0, 45L));
        assertTrue(session.act(0, 46L));
        assertTrue(session.act(0, 47L));
        assertEquals(ContestPhase.CATEGORY, session.phase());
        return session;
    }
}
