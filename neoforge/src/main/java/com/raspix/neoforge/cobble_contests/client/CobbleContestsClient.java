package com.raspix.neoforge.cobble_contests.client;

import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.blocks.BlockInit;
import com.raspix.neoforge.cobble_contests.menus.MenuInit;
import com.raspix.neoforge.cobble_contests.menus.screens.ContestBoothScreen;
import com.raspix.neoforge.cobble_contests.menus.screens.PlayerConditionCardScreen;
import com.raspix.neoforge.cobble_contests.menus.screens.PoffinPotScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Physical-client-only registrations. Never reference this class from common setup. */
@EventBusSubscriber(modid = CobbleContestsForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CobbleContestsClient {
    private CobbleContestsClient() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(BlockInit.CONTEST_BOOTH.get(), RenderType.translucent()));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuInit.CONTEST_MENU.get(), ContestBoothScreen::new);
        event.register(MenuInit.PLAYER_CONTEST_INFO_MENU.get(), PlayerConditionCardScreen::new);
        event.register(MenuInit.POFFIN_POT_MENU.get(), PoffinPotScreen::new);
    }
}
