package com.raspix.neoforge.cobble_contests.network;

import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.client.ClientPayloadHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

//https://fabricmc.net/2024/04/19/1205.html#:%7E:text=is%20already%20broken
//https://wiki.fabricmc.net/tutorial:networking#networking_in_1205
//

//@EventBusSubscriber(modid = CobbleContestsForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD)

//@EventBusSubscriber(modid = CobbleContestsForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class MessagesInit {
    public static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(CobbleContestsForge.MOD_ID, CobbleContestsForge.MOD_ID);
    public static final ResourceLocation WALLET_ID_1 = ResourceLocation.fromNamespaceAndPath(CobbleContestsForge.MOD_ID, "wallet_conditions");
    public static final ResourceLocation WALLET_ID_2 = ResourceLocation.fromNamespaceAndPath(CobbleContestsForge.MOD_ID, "wallet_conditions2");
    public static final ResourceLocation RUN_CONTEST = ResourceLocation.fromNamespaceAndPath(CobbleContestsForge.MOD_ID, "run_contest");


    //@SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CobbleContestsForge.MOD_ID).optional();
        registrar.playToServer(
                SBWalletScreenParty.PACKET_ID, SBWalletScreenParty.PACKET_CODEC,
                new MainThreadPayloadHandler<>(
                        SBWalletScreenParty::handleDataOnMain
                )
        );
        registrar.playToServer(
                SBRunContest.PACKET_ID, SBRunContest.PACKET_CODEC,
                new MainThreadPayloadHandler<>(
                        SBRunContest::handleDataOnMain
                )
        );

        registrar.playToClient( //.commonBidirectional
                CBWalletScreenParty.PACKET_ID, CBWalletScreenParty.PACKET_CODEC,
                new MainThreadPayloadHandler<>( //DirectionalPayloadHandler
                        ClientPayloadHandler::handleWalletParty
                )
        );

    }

    /**public static void registerC2SPackets() { // client to server

        //SERVERBOUND
        PayloadTypeRegistry.playC2S().register(SBWalletScreenParty.PACKET_ID, SBWalletScreenParty.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SBRunContest.PACKET_ID, SBRunContest.PACKET_CODEC);

        //CLIENTBOUND
        PayloadTypeRegistry.playS2C().register(CBWalletScreenParty.PACKET_ID, CBWalletScreenParty.PACKET_CODEC);


        ServerPlayNetworking.registerGlobalReceiver(SBWalletScreenParty.PACKET_ID, (payload, context) -> {
            payload.recieve(context.server(), context.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(SBRunContest.PACKET_ID, (payload, context) -> {
            payload.recieve(context.server(), context.player());
        });
    }

    public static void registerS2CPackets(){ //server to client
        ClientPlayNetworking.registerGlobalReceiver(CBWalletScreenParty.PACKET_ID, (payload, context) -> {
            // \] # written by cat (Parix), do not delete, she was helping
            payload.recieve(context.client());

        });

    }*/

    /**public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(WALLET_ID, (server, player, handler, buf, responseSender) -> {
            SBWalletScreenParty packet = SBWalletScreenParty.read(buf);//new SBWalletScreenParty();
            //packet.read(buf);
            server.execute(() -> packet.handle(server, player, handler, responseSender));
        });
    }*/




    // Register the packet
    /**public class ModInitializer implements ModInitializer {
        private static final Identifier CUSTOM_MESSAGE_PACKET_ID = new Identifier("modid", "custom_message");

        @Override
        public void onInitialize() {
            ServerPlayNetworking.registerGlobalReceiver(CUSTOM_MESSAGE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
                CustomMessagePacket packet = new CustomMessagePacket();
                packet.read(buf);
                server.execute(() -> packet.handle(server, player, handler, responseSender));
            });
        }
    }*/

}
