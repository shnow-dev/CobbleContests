package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContestScoreTest {
    @Test
    void usesTheApprovedWeights() {
        ContestScore score = new ContestScore();
        score.set(ContestPhase.PRESENTATION, 80);
        score.set(ContestPhase.EVALUATION, 100);
        score.set(ContestPhase.CAPABILITIES, 60);
        score.set(ContestPhase.CATEGORY, 90);
        score.set(ContestPhase.RHYTHM, 70);
        score.set(ContestPhase.FINALE, 100);

        assertEquals(81, score.total());
    }

    @Test
    void clampsEveryPhaseToTheZeroToOneHundredRange() {
        ContestScore score = new ContestScore();
        score.set(ContestPhase.PRESENTATION, -30);
        score.set(ContestPhase.EVALUATION, 150);

        assertEquals(0, score.get(ContestPhase.PRESENTATION));
        assertEquals(100, score.get(ContestPhase.EVALUATION));
    }
}
