package com.raspix.common.cobble_contests.contest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContestEvaluationTest {
    @Test
    void usesTheStricterQuadraticCurve() {
        assertEquals(0, ContestEvaluation.scoreForCondition(0));
        assertEquals(4, ContestEvaluation.scoreForCondition(50));
        assertEquals(15, ContestEvaluation.scoreForCondition(100));
        assertEquals(35, ContestEvaluation.scoreForCondition(150));
        assertEquals(62, ContestEvaluation.scoreForCondition(200));
        assertEquals(81, ContestEvaluation.scoreForCondition(230));
        assertEquals(100, ContestEvaluation.scoreForCondition(255));
    }

    @Test
    void clampsConditionValuesBeforeScoring() {
        assertEquals(0, ContestEvaluation.scoreForCondition(-10));
        assertEquals(100, ContestEvaluation.scoreForCondition(300));
    }
}
