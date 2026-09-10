package com.raspix.common.cobble_contests.contest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative state machine for one contest entry. The Minecraft
 * integration owns persistence and networking; this class owns all scoring
 * and phase transitions.
 */
public final class ContestSession {
    public static final int COOL_CATEGORY = 0;
    public static final int BEAUTY_CATEGORY = 1;
    public static final int GRACE_CATEGORY = 2;
    public static final int SMART_CATEGORY = 3;
    public static final int TOUGH_CATEGORY = 4;
    public static final int PRESENTATION_ACTIONS = 3;
    public static final int CAPABILITY_ACTIONS = 3;
    public static final int RHYTHM_ACTIONS = 5;

    private static final long PRESENTATION_DURATION = 20L * 15L;
    private static final long EVALUATION_DURATION = 20L * 2L;
    private static final long CAPABILITIES_DURATION = 20L * 20L;
    private static final long CATEGORY_DURATION = 20L * 20L;
    private static final long RHYTHM_DURATION = 20L * 15L;
    private static final long FINALE_DURATION = 20L * 5L;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID playerId;
    private final int partySlot;
    private final int category;
    private final int rank;
    private final int evaluationScore;
    private final MoveOption[] moves = new MoveOption[4];
    private final ContestScore scores = new ContestScore();
    private final CoolPrecisionChallenge precisionChallenge;
    private final BeautyCompositionChallenge beautyChallenge;
    private final GraceTracingChallenge graceChallenge;
    private final SmartMemoryChallenge smartChallenge;
    private final ToughProtectionChallenge toughChallenge;

    private ContestPhase phase = ContestPhase.PRESENTATION;
    private long phaseStartedAt;
    private long phaseEndsAt;
    private long timingTargetAt;
    private int progress;
    private int accumulatedScore;
    private int resolvedTargets;
    private int stateVersion;
    private int lastMoveIndex = -1;

    public ContestSession(UUID playerId, int partySlot, int category, int rank,
                          int evaluationScore, List<MoveOption> moves, long seed, long gameTime) {
        this.playerId = playerId;
        this.partySlot = partySlot;
        this.category = category;
        this.rank = rank;
        this.evaluationScore = ContestScore.clamp(evaluationScore);
        for (int index = 0; index < this.moves.length; index++) {
            this.moves[index] = index < moves.size() ? moves.get(index) : MoveOption.missing();
        }

        CoolPrecisionChallenge precision = null;
        BeautyCompositionChallenge beauty = null;
        GraceTracingChallenge grace = null;
        SmartMemoryChallenge smart = null;
        ToughProtectionChallenge tough = null;
        switch (category) {
            case COOL_CATEGORY -> precision = new CoolPrecisionChallenge(seed);
            case BEAUTY_CATEGORY -> beauty = new BeautyCompositionChallenge(seed);
            case GRACE_CATEGORY -> grace = new GraceTracingChallenge(seed, rank);
            case SMART_CATEGORY -> smart = new SmartMemoryChallenge(seed, rank);
            case TOUGH_CATEGORY -> tough = new ToughProtectionChallenge(seed, rank);
            default -> throw new IllegalArgumentException("Unknown contest category " + category);
        }
        precisionChallenge = precision;
        beautyChallenge = beauty;
        graceChallenge = grace;
        smartChallenge = smart;
        toughChallenge = tough;
        beginPhase(ContestPhase.PRESENTATION, gameTime);
    }

    public boolean act(int value, long gameTime) {
        if (phase == ContestPhase.RESULTS || gameTime > phaseEndsAt) {
            return false;
        }

        return switch (phase) {
            case PRESENTATION -> timedAction(PRESENTATION_ACTIONS, value, gameTime);
            case CAPABILITIES -> capabilityAction(value, gameTime);
            case CATEGORY -> categoryAction(value, gameTime);
            case RHYTHM -> rhythmAction(value, gameTime);
            case FINALE -> timedAction(1, value, gameTime);
            case EVALUATION, RESULTS -> false;
        };
    }

    public boolean tick(long gameTime) {
        if (phase == ContestPhase.RESULTS || gameTime <= phaseEndsAt) {
            return false;
        }
        finishCurrentPhase(gameTime);
        return true;
    }

    private boolean timedAction(int requiredActions, int value, long gameTime) {
        if (value != progress) {
            return false;
        }
        accumulatedScore += reactionScore(gameTime - phaseStartedAt);
        progress++;
        if (progress >= requiredActions) {
            finishCurrentPhase(gameTime);
        } else {
            phaseStartedAt = gameTime;
            stateVersion++;
        }
        return true;
    }

    private boolean capabilityAction(int moveIndex, long gameTime) {
        if (moveIndex < 0 || moveIndex >= moves.length || moves[moveIndex].empty()) {
            return false;
        }
        MoveOption move = moves[moveIndex];
        int score = move.appeal() * 14;
        if (move.matchesCategory()) {
            score += 20;
        }
        if (moveIndex == lastMoveIndex) {
            score -= 25;
        }
        accumulatedScore += ContestScore.clamp(score);
        lastMoveIndex = moveIndex;
        progress++;
        if (progress >= CAPABILITY_ACTIONS) {
            finishCurrentPhase(gameTime);
        } else {
            stateVersion++;
        }
        return true;
    }

    private boolean precisionAction(int targetIndex, long gameTime) {
        if (targetIndex < 0 || targetIndex >= CoolPrecisionChallenge.TARGET_COUNT) {
            return false;
        }
        int bit = 1 << targetIndex;
        if ((resolvedTargets & bit) != 0) {
            return false;
        }
        resolvedTargets |= bit;
        accumulatedScore += precisionChallenge.targetAt(targetIndex).points();
        stateVersion++;
        if (precisionChallenge.allPositiveTargetsResolved(resolvedTargets)) {
            finishCurrentPhase(gameTime);
        }
        return true;
    }

    private boolean categoryAction(int value, long gameTime) {
        return switch (category) {
            case COOL_CATEGORY -> precisionAction(value, gameTime);
            case BEAUTY_CATEGORY -> beautyAction(value, gameTime);
            case GRACE_CATEGORY -> graceAction(value, gameTime);
            case SMART_CATEGORY -> smartAction(value, gameTime);
            case TOUGH_CATEGORY -> toughAction(value, gameTime);
            default -> false;
        };
    }

    private boolean beautyAction(int optionIndex, long gameTime) {
        if (optionIndex < 0 || optionIndex >= BeautyCompositionChallenge.OPTION_COUNT) {
            return false;
        }
        int bit = 1 << optionIndex;
        if ((resolvedTargets & bit) != 0) {
            return false;
        }
        resolvedTargets |= bit;
        accumulatedScore += beautyChallenge.pointsForOption(optionIndex);
        progress++;
        stateVersion++;
        if (progress >= BeautyCompositionChallenge.PICK_COUNT) {
            finishCurrentPhase(gameTime);
        }
        return true;
    }

    private boolean graceAction(int nodeIndex, long gameTime) {
        if (nodeIndex < 0 || nodeIndex >= GraceTracingChallenge.NODE_COUNT) {
            return false;
        }
        if (nodeIndex == graceChallenge.nodeAt(progress)) {
            accumulatedScore += graceChallenge.pointsPerNode();
            resolvedTargets |= 1 << nodeIndex;
            progress++;
        } else {
            accumulatedScore -= 10;
        }
        stateVersion++;
        if (progress >= graceChallenge.length()) {
            finishCurrentPhase(gameTime);
        }
        return true;
    }

    private boolean smartAction(int symbol, long gameTime) {
        if (symbol < 0 || symbol >= SmartMemoryChallenge.SYMBOL_COUNT
                || gameTime - phaseStartedAt < 20L * 3L) {
            return false;
        }
        if (symbol == smartChallenge.symbolAt(progress)) {
            accumulatedScore += smartChallenge.pointsPerSymbol();
        }
        progress++;
        stateVersion++;
        if (progress >= smartChallenge.length()) {
            finishCurrentPhase(gameTime);
        }
        return true;
    }

    private boolean toughAction(int lane, long gameTime) {
        if (lane < 0 || lane >= ToughProtectionChallenge.LANE_COUNT) {
            return false;
        }
        if (lane == toughChallenge.laneAt(progress)) {
            accumulatedScore += reactionScore(gameTime - phaseStartedAt);
        }
        progress++;
        stateVersion++;
        if (progress >= toughChallenge.length()) {
            finishCurrentPhase(gameTime);
        } else {
            phaseStartedAt = gameTime;
        }
        return true;
    }

    private boolean rhythmAction(int beatIndex, long gameTime) {
        if (beatIndex != progress) {
            return false;
        }
        long distance = Math.abs(gameTime - timingTargetAt);
        accumulatedScore += distance <= 2 ? 100 : distance <= 5 ? 75 : distance <= 8 ? 50 : 20;
        progress++;
        if (progress >= RHYTHM_ACTIONS) {
            finishCurrentPhase(gameTime);
        } else {
            timingTargetAt = gameTime + 20L;
            stateVersion++;
        }
        return true;
    }

    private void finishCurrentPhase(long gameTime) {
        switch (phase) {
            case PRESENTATION -> scores.set(phase, accumulatedScore / PRESENTATION_ACTIONS);
            case CAPABILITIES -> scores.set(phase, accumulatedScore / CAPABILITY_ACTIONS);
            case CATEGORY -> scores.set(phase, categoryScore());
            case RHYTHM -> scores.set(phase, accumulatedScore / RHYTHM_ACTIONS);
            case FINALE -> scores.set(phase, accumulatedScore);
            case EVALUATION, RESULTS -> {
            }
        }

        ContestPhase next = switch (phase) {
            case PRESENTATION -> ContestPhase.EVALUATION;
            case EVALUATION -> ContestPhase.CAPABILITIES;
            case CAPABILITIES -> ContestPhase.CATEGORY;
            case CATEGORY -> ContestPhase.RHYTHM;
            case RHYTHM -> ContestPhase.FINALE;
            case FINALE, RESULTS -> ContestPhase.RESULTS;
        };
        beginPhase(next, gameTime);
    }

    private void beginPhase(ContestPhase next, long gameTime) {
        phase = next;
        if (phase == ContestPhase.EVALUATION) {
            scores.set(ContestPhase.EVALUATION, evaluationScore);
        }
        phaseStartedAt = gameTime;
        progress = 0;
        accumulatedScore = 0;
        lastMoveIndex = -1;
        timingTargetAt = gameTime + 20L;
        phaseEndsAt = switch (phase) {
            case PRESENTATION -> gameTime + PRESENTATION_DURATION;
            case CAPABILITIES -> gameTime + CAPABILITIES_DURATION;
            case CATEGORY -> gameTime + CATEGORY_DURATION;
            case RHYTHM -> gameTime + RHYTHM_DURATION;
            case FINALE -> gameTime + FINALE_DURATION;
            case EVALUATION -> gameTime + EVALUATION_DURATION;
            case RESULTS -> gameTime;
        };
        stateVersion++;
    }

    private static int reactionScore(long elapsedTicks) {
        if (elapsedTicks <= 10) {
            return 100;
        }
        if (elapsedTicks <= 20) {
            return 80;
        }
        if (elapsedTicks <= 40) {
            return 60;
        }
        return 30;
    }

    private int categoryScore() {
        if (category == TOUGH_CATEGORY) {
            return accumulatedScore / toughChallenge.length();
        }
        return accumulatedScore;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID playerId() {
        return playerId;
    }

    public int partySlot() {
        return partySlot;
    }

    public int category() {
        return category;
    }

    public int rank() {
        return rank;
    }

    public ContestPhase phase() {
        return phase;
    }

    public long phaseEndsAt() {
        return phaseEndsAt;
    }

    public long timingTargetAt() {
        return timingTargetAt;
    }

    public int progress() {
        return progress;
    }

    public int resolvedTargets() {
        return resolvedTargets;
    }

    public int packedTargets() {
        return switch (category) {
            case COOL_CATEGORY -> precisionChallenge.packedTargets();
            case BEAUTY_CATEGORY -> beautyChallenge.packedData();
            case GRACE_CATEGORY -> graceChallenge.packedData();
            case SMART_CATEGORY -> smartChallenge.packedData();
            case TOUGH_CATEGORY -> toughChallenge.packedData();
            default -> 0;
        };
    }

    public int categoryActionCount() {
        return switch (category) {
            case COOL_CATEGORY -> CoolPrecisionChallenge.TARGET_COUNT;
            case BEAUTY_CATEGORY -> BeautyCompositionChallenge.PICK_COUNT;
            case GRACE_CATEGORY -> graceChallenge.length();
            case SMART_CATEGORY -> smartChallenge.length();
            case TOUGH_CATEGORY -> toughChallenge.length();
            default -> 0;
        };
    }

    public int stateVersion() {
        return stateVersion;
    }

    public int[] phaseScores() {
        return scores.snapshot();
    }

    public int totalScore() {
        return scores.total();
    }

    public String[] moveNames() {
        return Arrays.stream(moves).map(MoveOption::name).toArray(String[]::new);
    }

    public boolean isFinished() {
        return phase == ContestPhase.RESULTS;
    }

    public record MoveOption(String name, int appeal, boolean matchesCategory, boolean empty) {
        public MoveOption(String name, int appeal, boolean matchesCategory) {
            this(name, Math.max(0, Math.min(6, appeal)), matchesCategory, false);
        }

        public static MoveOption missing() {
            return new MoveOption("", 0, false, true);
        }
    }
}
