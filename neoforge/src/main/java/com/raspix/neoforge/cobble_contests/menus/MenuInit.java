package com.raspix.neoforge.cobble_contests.menus;

import com.raspix.common.cobble_contests.CobbleContests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
//import net.minecraftforge.common.extensions.IForgeMenuType;
//import net.minecraftforge.registries.DeferredRegister;
//import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegistryObject;

public class MenuInit {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, CobbleContests.MOD_ID);

    public static final Supplier<MenuType<ContestBoothMenu>> CONTEST_MENU = MENU_TYPES.register("contest_menu", () -> IMenuTypeExtension.create(ContestBoothMenu::new));
    public static final Supplier<MenuType<PoffinPotMenu>> POFFIN_POT_MENU = MENU_TYPES.register("poffin_pot_menu", () -> IMenuTypeExtension.create(PoffinPotMenu::new));
    public static final Supplier<MenuType<PlayerConditionCardMenu>> PLAYER_CONTEST_INFO_MENU = MENU_TYPES.register("player_contest_info_menu", () -> new MenuType<>(PlayerConditionCardMenu::new, FeatureFlags.DEFAULT_FLAGS));

}
