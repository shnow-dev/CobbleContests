package com.raspix.neoforge.cobble_contests.network;

import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** One user interaction within an active contest phase. */
public final class SBContestAction implements CustomPacketPayload {
    public static final Type<SBContestAction> PACKET_ID = new Type<>(MessagesInit.CONTEST_ACTION);
    public static final StreamCodec<FriendlyByteBuf, SBContestAction> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SBContestAction decode(FriendlyByteBuf buf) {
            return new SBContestAction(buf.readUUID(), buf.readBlockPos(),
                    buf.readInt(), buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(FriendlyByteBuf buf, SBContestAction payload) {
            buf.writeUUID(payload.sessionId);
            buf.writeBlockPos(payload.pos);
            buf.writeInt(payload.phase);
            buf.writeInt(payload.stateVersion);
            buf.writeInt(payload.value);
        }
    };

    private final UUID sessionId;
    private final BlockPos pos;
    private final int phase;
    private final int stateVersion;
    private final int value;

    public SBContestAction(UUID sessionId, BlockPos pos, int phase, int stateVersion, int value) {
        this.sessionId = sessionId;
        this.pos = pos.immutable();
        this.phase = phase;
        this.stateVersion = stateVersion;
        this.value = value;
    }

    public static void handleDataOnMain(SBContestAction data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ContestBoothMenu menu)
                || player.distanceToSqr(data.pos.getX() + 0.5, data.pos.getY() + 0.5,
                data.pos.getZ() + 0.5) > 64.0
                || !player.serverLevel().hasChunkAt(data.pos)) {
            return;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(data.pos);
        if (blockEntity instanceof ContestBlockEntity contestBlock
                && menu.getBlockEntity() == contestBlock) {
            contestBlock.handleContestAction(
                    player, data.sessionId, data.phase, data.stateVersion, data.value
            );
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
