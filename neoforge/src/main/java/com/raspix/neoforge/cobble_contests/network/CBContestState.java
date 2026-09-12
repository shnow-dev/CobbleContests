package com.raspix.neoforge.cobble_contests.network;

import com.raspix.common.cobble_contests.contest.ContestSession;
import com.raspix.common.cobble_contests.contest.PresentationBubbleChallenge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

/** Complete client view of a server-owned contest session. */
public final class CBContestState implements CustomPacketPayload {
    public static final Type<CBContestState> PACKET_ID = new Type<>(MessagesInit.CONTEST_STATE);
    public static final StreamCodec<FriendlyByteBuf, CBContestState> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull CBContestState decode(FriendlyByteBuf buf) {
            UUID playerId = buf.readUUID();
            UUID sessionId = buf.readUUID();
            BlockPos pos = buf.readBlockPos();
            int category = buf.readInt();
            int rank = buf.readInt();
            int phase = buf.readInt();
            int remainingTicks = buf.readInt();
            int timingTicks = buf.readInt();
            int progress = buf.readInt();
            int packedTargets = buf.readInt();
            int presentationTargetCount = buf.readVarInt();
            if (presentationTargetCount < 0
                    || presentationTargetCount > PresentationBubbleChallenge.MAX_ACTIVE_BUBBLES) {
                throw new IllegalArgumentException(
                        "Invalid presentation target count " + presentationTargetCount
                );
            }
            int[] presentationTargets = new int[presentationTargetCount];
            for (int index = 0; index < presentationTargets.length; index++) {
                presentationTargets[index] = buf.readInt();
            }
            int resolvedTargets = buf.readInt();
            int stateVersion = buf.readInt();
            int totalScore = buf.readInt();
            int requiredScore = buf.readInt();
            boolean finished = buf.readBoolean();
            boolean won = buf.readBoolean();
            boolean presentationPreview = buf.readBoolean();
            int[] scores = new int[6];
            for (int index = 0; index < scores.length; index++) {
                scores[index] = buf.readInt();
            }
            String[] moveNames = new String[4];
            for (int index = 0; index < moveNames.length; index++) {
                moveNames[index] = buf.readUtf(64);
            }
            return new CBContestState(playerId, sessionId, pos, category, rank, phase,
                    remainingTicks, timingTicks, progress, packedTargets, presentationTargets,
                    resolvedTargets,
                    stateVersion, totalScore, requiredScore, finished, won, presentationPreview,
                    scores, moveNames);
        }

        @Override
        public void encode(FriendlyByteBuf buf, CBContestState payload) {
            buf.writeUUID(payload.playerId);
            buf.writeUUID(payload.sessionId);
            buf.writeBlockPos(payload.pos);
            buf.writeInt(payload.category);
            buf.writeInt(payload.rank);
            buf.writeInt(payload.phase);
            buf.writeInt(payload.remainingTicks);
            buf.writeInt(payload.timingTicks);
            buf.writeInt(payload.progress);
            buf.writeInt(payload.packedTargets);
            buf.writeVarInt(payload.presentationTargets.length);
            for (int presentationTarget : payload.presentationTargets) {
                buf.writeInt(presentationTarget);
            }
            buf.writeInt(payload.resolvedTargets);
            buf.writeInt(payload.stateVersion);
            buf.writeInt(payload.totalScore);
            buf.writeInt(payload.requiredScore);
            buf.writeBoolean(payload.finished);
            buf.writeBoolean(payload.won);
            buf.writeBoolean(payload.presentationPreview);
            for (int score : payload.scores) {
                buf.writeInt(score);
            }
            for (String moveName : payload.moveNames) {
                buf.writeUtf(moveName, 64);
            }
        }
    };

    private final UUID playerId;
    private final UUID sessionId;
    private final BlockPos pos;
    private final int category;
    private final int rank;
    private final int phase;
    private final int remainingTicks;
    private final int timingTicks;
    private final int progress;
    private final int packedTargets;
    private final int[] presentationTargets;
    private final int resolvedTargets;
    private final int stateVersion;
    private final int totalScore;
    private final int requiredScore;
    private final boolean finished;
    private final boolean won;
    private final boolean presentationPreview;
    private final int[] scores;
    private final String[] moveNames;

    public CBContestState(UUID playerId, UUID sessionId, BlockPos pos, int category, int rank,
                          int phase, int remainingTicks, int timingTicks, int progress,
                          int packedTargets, int[] presentationTargets, int resolvedTargets,
                          int stateVersion,
                          int totalScore, int requiredScore, boolean finished, boolean won,
                          boolean presentationPreview, int[] scores, String[] moveNames) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.pos = pos.immutable();
        this.category = category;
        this.rank = rank;
        this.phase = phase;
        this.remainingTicks = Math.max(0, remainingTicks);
        this.timingTicks = timingTicks;
        this.progress = progress;
        this.packedTargets = packedTargets;
        this.presentationTargets = Arrays.copyOf(
                presentationTargets, Math.min(
                        presentationTargets.length,
                        PresentationBubbleChallenge.MAX_ACTIVE_BUBBLES
                )
        );
        this.resolvedTargets = resolvedTargets;
        this.stateVersion = stateVersion;
        this.totalScore = totalScore;
        this.requiredScore = requiredScore;
        this.finished = finished;
        this.won = won;
        this.presentationPreview = presentationPreview;
        this.scores = Arrays.copyOf(scores, 6);
        this.moveNames = Arrays.copyOf(moveNames, 4);
    }

    public static CBContestState from(ContestSession session, BlockPos pos, long gameTime,
                                      int requiredScore) {
        return new CBContestState(
                session.playerId(), session.sessionId(), pos, session.category(), session.rank(),
                session.phase().ordinal(), (int) Math.max(0L, session.phaseEndsAt() - gameTime),
                (int) (session.timingTargetAt() - gameTime), session.progress(),
                session.packedTargets(), session.presentationTargets(), session.resolvedTargets(),
                session.stateVersion(),
                session.totalScore(), requiredScore, session.isFinished(),
                !session.presentationOnly() && session.isFinished()
                        && session.totalScore() >= requiredScore,
                session.presentationOnly(),
                session.phaseScores(), session.moveNames()
        );
    }

    public UUID playerId() { return playerId; }
    public UUID sessionId() { return sessionId; }
    public BlockPos pos() { return pos; }
    public int category() { return category; }
    public int rank() { return rank; }
    public int phase() { return phase; }
    public int remainingTicks() { return remainingTicks; }
    public int timingTicks() { return timingTicks; }
    public int progress() { return progress; }
    public int packedTargets() { return packedTargets; }
    public int[] presentationTargets() {
        return Arrays.copyOf(presentationTargets, presentationTargets.length);
    }
    public int resolvedTargets() { return resolvedTargets; }
    public int stateVersion() { return stateVersion; }
    public int totalScore() { return totalScore; }
    public int requiredScore() { return requiredScore; }
    public boolean finished() { return finished; }
    public boolean won() { return won; }
    public boolean presentationPreview() { return presentationPreview; }
    public int[] scores() { return Arrays.copyOf(scores, scores.length); }
    public String[] moveNames() { return Arrays.copyOf(moveNames, moveNames.length); }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
