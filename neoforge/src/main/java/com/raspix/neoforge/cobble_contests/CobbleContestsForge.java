package com.raspix.neoforge.cobble_contests;

import com.mojang.logging.LogUtils;
import com.raspix.neoforge.cobble_contests.blocks.BlockInit;
import com.raspix.neoforge.cobble_contests.blocks.entity.BlockEntityInit;
import com.raspix.neoforge.cobble_contests.events.JsonLoadMoves;
import com.raspix.neoforge.cobble_contests.items.ItemInit;
import com.raspix.neoforge.cobble_contests.menus.MenuInit;
import com.raspix.neoforge.cobble_contests.network.MessagesInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.RegisterCommandsEvent;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.ModLoadingContext;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.config.ModConfig;
//import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import net.minecraftforge.registries.DeferredRegister;
//import org.slf4j.Logger;
//import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;


/**TODO future:
 * -texture booth
 * -clean up all print statements
 * -make sure contest results are fully translatable
 *
 *
 * DONE:
 *  -make contests tell result
 *  -make poffins tell if full
 *  -figure out how to make hexagon translusent + fix color issue
 *  -make default values for creative poffins
 *  -make sure nature eating poffin bonus is properly applied
 *  -make sure poffins dissapear on use in survival
 *  -get tooltips to show up on poffinpot
 *  -fix up poffins with unclear colors
 *  -finish booth gui
 *
 *
 */


// hello

@Mod(CobbleContestsForge.MOD_ID)
public class CobbleContestsForge {

    public static final String MOD_ID = "cobble_contests";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    //public static ContestMoves contestMoves;
    public static final CobbleContestsDataProvider dataProvider = new CobbleContestsDataProvider();




    public CobbleContestsForge(IEventBus modEventBus, ModContainer modContainer){
        //CobbleContests.init();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::initialize);

        CREATIVE_MODE_TABS.register(modEventBus);


        BlockInit.BLOCKS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);

        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        MenuInit.MENU_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloads);
        //modEventBus.addListener();
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);



        /**PlatformEvents.SERVER_STARTED.subscribe(Priority.LOWEST, event ->{
            contestMoves = new ContestMoves();
            return null;
        });*/

    }

    @SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        //ExampleCommandRegistry.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    /**@SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        ExampleCommandRegistry.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }*/

    public void initialize(FMLCommonSetupEvent event){
        dataProvider.registerDefaults();
    }

    /**@EventBusSubscriber(bus = EventBusSubscriber.Bus.NEOFORGE)
    public static final class Registration {

        @SubscribeEvent
        public static void onCommandRegistration(final RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("test")
                    .executes(context -> {
                        Species species = PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.of("cobblemon:bidoof", ':'));
                        context.getSource().sendSystemMessage(
                                Component.literal("Got species: ")
                                        .withStyle(Style.EMPTY.withColor(0x03e3fc))
                                        .append(species.getTranslatedName())
                        );
                        return 0;
                    })
            );
        }

        //SimpleJsonResourceReloadListener
    }*/


    public void registerPayloads(final RegisterPayloadHandlersEvent event) {
        //CobbleContestsForge.LOGGER.info("Registering Messages XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        //System.out.println("HELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLllO registerPayloads");
        MessagesInit.register(event);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {

    }


    private void addCreative(BuildCreativeModeTabContentsEvent event) {}

    /**@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public class CommonModEvents{

        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event){
            event.enqueueWork(() -> {
                System.out.println("HELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLllO in commonSetup");
                //PacketHandler.register();

            });

        }
        @SubscribeEvent
        public void addJsonListeners(AddReloadListenerEvent event) {
            System.out.println("HELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLllO in addJsonListeners");
            event.addListener(JsonLoadMoves.instance);
        }

        @SubscribeEvent
        public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
            CobbleContestsForge.LOGGER.info("Registering Messages XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            System.out.println("HELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLllO registerPayloads");
            MessagesInit.register(event);
        }

    }*/



}
