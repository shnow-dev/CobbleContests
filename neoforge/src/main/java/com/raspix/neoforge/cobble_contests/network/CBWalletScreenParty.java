package com.raspix.neoforge.cobble_contests.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CBWalletScreenParty implements CustomPacketPayload {

    public final UUID id;
    CompoundTag tag;

    public static final Type<CBWalletScreenParty> PACKET_ID = new Type<>(MessagesInit.WALLET_ID_2);
    public static final StreamCodec<FriendlyByteBuf, CBWalletScreenParty> PACKET_CODEC = new StreamCodec<FriendlyByteBuf, CBWalletScreenParty>() {
        @Override
        public @NotNull CBWalletScreenParty decode(FriendlyByteBuf buf) {
            CompoundTag compoundTag = new CompoundTag();
            return new CBWalletScreenParty(FriendlyByteBuf.readUUID(buf), FriendlyByteBuf.readNbt(buf));
        }

        @Override
        public void encode(FriendlyByteBuf buf, CBWalletScreenParty walletScreenParty) {
            FriendlyByteBuf.writeUUID(buf, walletScreenParty.getId());
            FriendlyByteBuf.writeNbt(buf, walletScreenParty.getTag());
        }
    };

    public UUID getId(){
        return id;
    }

    public CompoundTag getTag(){
        return tag;
    }

    /**public void recieve(Minecraft minecraft){
        System.out.println("Recieving CBWallet");
        if(Minecraft.getInstance().screen instanceof PlayerConditionCardScreen screen){
            //CompoundTag tag = buf.readNbt();
            screen.setCVs(this.tag);
            screen.setRibbons(this.tag);
        }
    }*/


    /**public static void recieve(Minecraft minecraft, ClientPacketListener clientPacketListener, FriendlyByteBuf buf, PacketSender packetSender) {
        System.out.println("Recieving CBWallet");
        if(Minecraft.getInstance().screen instanceof PlayerContestInfoScreen screen){
            CompoundTag tag = buf.readNbt();
            screen.setCVs(tag);
            screen.setRibbons(tag);
        }


    }*/

    public CBWalletScreenParty(UUID id, CompoundTag tag){
        this.id = id;
        this.tag = tag;
    }

    public CBWalletScreenParty(FriendlyByteBuf buf){
        this(buf.readUUID(), buf.readNbt());
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }


}
