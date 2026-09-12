package com.raspix.neoforge.cobble_contests.network;

import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SBRunContest implements CustomPacketPayload {

    public final UUID id;
    public final int index;
    public final BlockPos pos;
    public final int contestType;
    public final int contestLevel;



    public static final Type<SBRunContest> PACKET_ID = new Type<>(MessagesInit.RUN_CONTEST);
    public static final StreamCodec<FriendlyByteBuf, SBRunContest> PACKET_CODEC = new StreamCodec<FriendlyByteBuf, SBRunContest>() {
        @Override
        public @NotNull SBRunContest decode(FriendlyByteBuf buf) {
            return new SBRunContest(FriendlyByteBuf.readUUID(buf), buf.readInt(), FriendlyByteBuf.readBlockPos(buf), buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(FriendlyByteBuf buf, SBRunContest payload) {
            FriendlyByteBuf.writeUUID(buf, payload.getId());
            buf.writeInt(payload.getIndex());
            FriendlyByteBuf.writeBlockPos(buf, payload.getPos());
            buf.writeInt(payload.getContestType());
            buf.writeInt(payload.getContestLevel());
        }
    };

    public UUID getId(){
        return id;
    }

    public int getIndex(){
        return index;
    }

    public BlockPos getPos(){
        return pos;
    }

    public int getContestType(){
        return contestType;
    }

    public int getContestLevel(){
        return contestLevel;
    }

    public SBRunContest(UUID id, int index, BlockPos pos, int contestType, int contestLevel){
        this.id = id;
        this.index = index;
        this.pos = pos;
        this.contestType = contestType;
        this.contestLevel = contestLevel;
    }

    public SBRunContest(FriendlyByteBuf buf){
        this(buf.readUUID(), buf.readInt(), buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf){
        buf.writeUUID(this.id);
        buf.writeInt(this.index);
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.contestType);
        buf.writeInt(this.contestLevel);
    }

    public static SBRunContest decode(FriendlyByteBuf buf) {
        return new SBRunContest(buf.readUUID(), buf.readInt(), buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handleDataOnMain(final SBRunContest data, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !player.getUUID().equals(data.id)
                || data.index < 0 || data.index > 5
                || data.contestType < 0 || data.contestType > 4
                || data.contestLevel < 0 || data.contestLevel > 4
                || !(player.containerMenu instanceof com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu menu)
                || player.distanceToSqr(data.pos.getX() + 0.5, data.pos.getY() + 0.5, data.pos.getZ() + 0.5) > 64.0
                || !player.serverLevel().hasChunkAt(data.pos)) {
            return;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(data.pos);
        if (blockEntity instanceof ContestBlockEntity contestBlock
                && menu.getBlockEntity() == contestBlock) {
            contestBlock.startContestSession(
                    player, data.index, data.contestType, data.contestLevel
            );
        }
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
