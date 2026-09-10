package com.raspix.neoforge.cobble_contests.network;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import com.raspix.neoforge.cobble_contests.pokemon.Ribbons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SBWalletScreenParty implements CustomPacketPayload {

    public static final Type<SBWalletScreenParty> PACKET_ID = new Type<>(MessagesInit.WALLET_ID_1);
    public static final StreamCodec<FriendlyByteBuf, SBWalletScreenParty> PACKET_CODEC = new StreamCodec<FriendlyByteBuf, SBWalletScreenParty>() {
        @Override
        public @NotNull SBWalletScreenParty decode(FriendlyByteBuf buf) {
            return new SBWalletScreenParty(FriendlyByteBuf.readUUID(buf));
        }

        @Override
        public void encode(FriendlyByteBuf buf, SBWalletScreenParty walletScreenParty) {
            FriendlyByteBuf.writeUUID(buf, walletScreenParty.getId());
        }
    };//StreamCodec.of((StreamEncoder)SBWalletScreenParty::new, SBWalletScreenParty::recieve).cast();
    private final UUID id;

    public static void handleDataOnMain(final SBWalletScreenParty data, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)
                || !serverPlayer.getUUID().equals(data.getId())) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);
        List<Pokemon> pokemon = party.toGappyList();
        for (int i = 0; i < 6; i++) {
            Pokemon partyMember = i < pokemon.size() ? pokemon.get(i) : null;
            if (partyMember != null) {
                CompoundTag persistentData = partyMember.getPersistentData();
                tag.put("poke" + i, persistentData.getCompound("CVs"));
                tag.put("poke" + i + "ribbons", persistentData.getCompound("Ribbons"));
            } else {
                tag.put("poke" + i, new CVs().saveToNBT());
                tag.put("poke" + i + "ribbons", new Ribbons().saveToNBT());
            }
        }
        PacketDistributor.sendToPlayer(serverPlayer, new CBWalletScreenParty(serverPlayer.getUUID(), tag));
    }

    public UUID getId(){
        return id;
    }

    public SBWalletScreenParty(UUID id){
        this.id = id;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public SBWalletScreenParty(FriendlyByteBuf buf) {
        id = buf.readUUID();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.id);
    }

    private static FriendlyByteBuf recieve(FriendlyByteBuf buf) {
        System.out.println("recieve in SBWalletScreenParty was called");
        if(buf == null){
            System.out.println("buf is null");
        }else {
            System.out.println("buf is not null");
            System.out.println("buf is " + buf);
        }
        return buf;
    }

    /**public void recieve(MinecraftServer server, Player player) {
        System.out.println("Recieving SBWallet");
        PlayerPartyStore pps = null;
        CompoundTag tag = new CompoundTag();
        try {
            pps = Cobblemon.INSTANCE.getStorage().getParty((ServerPlayer) player);
            List<Pokemon> poke = pps.toGappyList();
            for(int i = 0; i < 6; i++){
                if(poke.size() > i && poke.get(i) != null){
                    CompoundTag pers = poke.get(i).getPersistentData();
                    tag.put("poke" +i, pers.getCompound("CVs"));
                    tag.put("poke" +i + "ribbons", pers.getCompound("Ribbons"));
                }else{
                    tag.put("poke" +i, new CVs().saveToNBT());
                    tag.put("poke" +i + "ribbons", new Ribbons().saveToNBT());
                }

            }
            if (player != null && player instanceof ServerPlayer serverPlayer) {
                FriendlyByteBuf bufi = new FriendlyByteBuf(Unpooled.buffer());
                //bufi.writeUUID(id);
                bufi.writeNbt(tag);
                ServerPlayNetworking.send(serverPlayer, new CBWalletScreenParty(id, tag));//MessagesInit.WALLET_ID_2, bufi);//.sendToClient(new CBWalletScreenParty(id, tag), () -> serverPlayer);
            }

        } catch (NullPointerException e){

            System.out.println(e.getMessage());
        }
    }*/


    /**public static void recieve(MinecraftServer server, Player player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender sender) {
        System.out.println("Recieving SBWallet");
        PlayerPartyStore pps = null;
        CompoundTag tag = new CompoundTag();
        try {
            UUID id = buf.readUUID();
            pps = Cobblemon.INSTANCE.getStorage().getParty(id);
            List<Pokemon> poke = pps.toGappyList();
            for(int i = 0; i < 6; i++){
                if(poke.size() > i && poke.get(i) != null){
                    CompoundTag pers = poke.get(i).getPersistentData();
                    tag.put("poke" +i, pers.getCompound("CVs"));
                    tag.put("poke" +i + "ribbons", pers.getCompound("Ribbons"));
                }else{
                    tag.put("poke" +i, new CVs().saveToNBT());
                    tag.put("poke" +i + "ribbons", new Ribbons().saveToNBT());
                }

            }
            if (player != null && player instanceof ServerPlayer serverPlayer) {
                FriendlyByteBuf bufi = new FriendlyByteBuf(Unpooled.buffer());
                //bufi.writeUUID(id);
                bufi.writeNbt(tag);
                ServerPlayNetworking.send(serverPlayer, MessagesInit.WALLET_ID_2, bufi);//.sendToClient(new CBWalletScreenParty(id, tag), () -> serverPlayer);
            }

        } catch (NoPokemonStoreException e) {
            throw new RuntimeException(e);
        }catch (NullPointerException e){

        }
    }*/


    //public final UUID id;



    /**public SBWalletScreenParty(FriendlyByteBuf buf){
        this(buf.readUUID());
    }*/

    /**public static void sendToServer(String message) {
        ClientPlayNetworking.send(new ResourceLocation("modid", "custom_message"), new FriendlyByteBuf(Unpooled.buffer()).writeString(message));
    }

    public static void sendToClient(Player player, String message) {
        ServerPlayNetworking.send(player, new ResourceLocation("modid", "custom_message"), new FriendlyByteBuf(Unpooled.buffer()).writeString(message));
    }*/




    /**public static class Handler {
        public static boolean handle(SBWalletScreenParty packet, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            context.get().enqueueWork(() -> {
                Player player = ctx.getSender();// context.get().getSender();
                PlayerPartyStore pps = null;
                CompoundTag tag = new CompoundTag();
                try {
                    pps = Cobblemon.INSTANCE.getStorage().getParty(packet.id);
                    List<Pokemon> poke = pps.toGappyList();
                    for(int i = 0; i < 6; i++){
                        if(poke.size() > i && poke.get(i) != null){
                            CompoundTag pers = poke.get(i).getPersistentData();
                            tag.put("poke" +i, pers.getCompound("CVs"));
                            tag.put("poke" +i + "ribbons", pers.getCompound("Ribbons"));
                        }else{
                            tag.put("poke" +i, new CVs().saveToNBT());
                            tag.put("poke" +i + "ribbons", new Ribbons().saveToNBT());
                        }

                    }
                    if (player != null && player instanceof ServerPlayer serverPlayer) {
                        PacketHandler.sendToClient(new CBInfoScreenParty(packet.id, tag), () -> serverPlayer);

                    }

                } catch (NoPokemonStoreException e) {
                    throw new RuntimeException(e);
                }catch (NullPointerException e){

                }

            });
            return true;
        }

    }*/
}

// Define a custom packet class
/**public class CustomMessagePacket implements Packet {
    private String message;

    public CustomMessagePacket(String message) {
        this.message = message;
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void handle(PacketListener packetListener) {

    }


}*/
