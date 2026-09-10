package com.raspix.neoforge.cobble_contests.blocks.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.google.common.collect.Maps;
import com.raspix.common.cobble_contests.CobbleContests;
import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.blocks.PoffinPot;
import com.raspix.neoforge.cobble_contests.items.ItemInit;
import com.raspix.neoforge.cobble_contests.menus.PoffinPotMenu;
import com.raspix.neoforge.cobble_contests.util.TagsInit;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel;

public class PoffinPotBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, MenuProvider {
    protected static final int BERRY_INPUT1 = 0;
    protected static final int BERRY_INPUT2 = 1;
    protected static final int BERRY_INPUT3 = 2;
    protected static final int BERRY_INPUT4 = 3;
    protected static final int DOUGH_INPUT = 4;
    protected static final int SLOT_FUEL = 5;
    protected static final int POFFIN_SLOT_RESULT = 6;

    private static final int[] SLOTS_4_UP = new int[]{0, 1, 2, 3, 4}; //input
    private static final int[] SLOTS_4_DOWN = new int[]{6, 5};  //output + bucket fuel
    private static final int[] SLOTS_4_SIDES = new int[]{5}; //fuel

    private static final Component TITLE = Component.translatable("container." + CobbleContestsForge.MOD_ID + ".poffin_pot");

    public PoffinPotBlockEntity(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    protected NonNullList<ItemStack> items = NonNullList.withSize(7, ItemStack.EMPTY);

    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;

    public PoffinPotBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityInit.POFFIN_POT_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.items = nonNullList;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, Player player) {
        System.out.println("UNO");
        return new PoffinPotMenu(id, inv, this, this, this.dataAccess);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory arg) {
        System.out.println("DUOS");
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty((ServerPlayer) arg.player);
        return new PoffinPotMenu(i, arg, this, this, this.dataAccess);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, PoffinPotBlockEntity pBlockEntity) {
        ItemStack berries1 = pBlockEntity.items.get(BERRY_INPUT1);
        ItemStack berries2 = pBlockEntity.items.get(BERRY_INPUT2);
        ItemStack berries3 = pBlockEntity.items.get(BERRY_INPUT3);
        ItemStack berries4 = pBlockEntity.items.get(BERRY_INPUT4);
        ItemStack dough = pBlockEntity.items.get(DOUGH_INPUT);
        ItemStack fuel = pBlockEntity.items.get(SLOT_FUEL);
        ItemStack resultPoffin = pBlockEntity.items.get(POFFIN_SLOT_RESULT);
        boolean flag = pBlockEntity.isLit();
        boolean flag1 = false;
        if (pBlockEntity.isLit()) {
            --pBlockEntity.litTime;
        }
        ItemStack itemstack = pBlockEntity.items.get(SLOT_FUEL);
        //boolean ore = !pBlockEntity.items.get(0).isEmpty();
        boolean isFuel = !itemstack.isEmpty();
        boolean isBerry = (!berries1.isEmpty() || !berries2.isEmpty() || !berries3.isEmpty() || !berries4.isEmpty());
        if ((pBlockEntity.isLit() || isFuel) && isBerry && !dough.isEmpty()) {
            ItemStack resultItem = getPoffinResult(berries1, berries2, berries3, berries4);
            int i = pBlockEntity.getMaxStackSize();
            if (!pBlockEntity.isLit() && pBlockEntity.canBurn(pLevel.registryAccess(), resultItem, pBlockEntity.items, i)) {
                pBlockEntity.litTime = pBlockEntity.getBurnDuration(itemstack);
                pBlockEntity.litDuration = pBlockEntity.litTime;
                if (pBlockEntity.isLit()) {
                    flag1 = true;
                    if (itemstack.hasCraftingRemainingItem()) {
                        pBlockEntity.items.set(SLOT_FUEL, itemstack.getCraftingRemainingItem());
                        System.out.println("Remaining time: " + itemstack.getCraftingRemainingItem());
                    }else if (isFuel) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            pBlockEntity.items.set(SLOT_FUEL, itemstack.getCraftingRemainingItem());
                        }
                    }
                }
            }
            if (pBlockEntity.isLit() && pBlockEntity.canBurn(pLevel.registryAccess(), resultItem, pBlockEntity.items, i)) {
                ++pBlockEntity.cookingProgress;
                if (pBlockEntity.cookingProgress == pBlockEntity.cookingTotalTime) {
                    pBlockEntity.cookingProgress = 0;
                    pBlockEntity.cookingTotalTime = getTotalCookTime(pLevel, pBlockEntity);
                    if (pBlockEntity.burn(pLevel.registryAccess(), resultItem, pBlockEntity.items, i)) {

                    }
                    flag1 = true;
                }
            } else {
                pBlockEntity.cookingProgress = 0;
            }
        } else if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
        }
        if (flag != pBlockEntity.isLit()) {
            flag1 = true;
            pState = pState.setValue(PoffinPot.LIT, Boolean.valueOf(pBlockEntity.isLit()));
            pLevel.setBlock(pPos, pState, 3);
        }
        if (flag1) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private static ItemStack getPoffinResult(ItemStack berries1, ItemStack berries2, ItemStack berries3, ItemStack berries4){
        //TODO: if any berries are the same, return randomized foul poffin
        if(isDuplicateBerry(berries1, berries2, berries3, berries4)){
            ItemStack resultPoffin = new ItemStack(ItemInit.FOUL_POFFIN.get());
            CompoundTag tag = resultPoffin.has(DataComponents.CUSTOM_DATA)? resultPoffin.get(DataComponents.CUSTOM_DATA).copyTag(): new CompoundTag();

            CompoundTag poffinTag = new CompoundTag();
            poffinTag.putInt("spicy", -10);
            poffinTag.putInt("dry", -10);
            poffinTag.putInt("sweet", -10);
            poffinTag.putInt("bitter", -10);
            poffinTag.putInt("sour", -10);
            poffinTag.putInt("sheen", -30);

            tag.put("Flavors", poffinTag);
            CustomData.set(DataComponents.CUSTOM_DATA, resultPoffin, tag);
            return resultPoffin;
        }
        //boolean isBerryDupe = false;
        int[] berryTotal = new int[]{0, 0, 0, 0, 0, 0};
        int numBerries = 0;
        if(!berries1.isEmpty()){
            berryTotal = addValues(berryTotal.clone(), berryFlavors.get(berries1.getItem()));
            numBerries += 1;
        }
        if(!berries2.isEmpty()){
            berryTotal = addValues(berryTotal.clone(), berryFlavors.get(berries2.getItem()));
            numBerries += 1;
        }
        if(!berries3.isEmpty()){
            berryTotal = addValues(berryTotal.clone(), berryFlavors.get(berries3.getItem()));
            numBerries += 1;
        }
        if(!berries4.isEmpty()){
            berryTotal = addValues(berryTotal.clone(), berryFlavors.get(berries4.getItem()));
            numBerries += 1;
        }

        int[] berryCalc = berryTotal.clone();
        int negs = 0; //how many values are negative
        for (int i = 0; i < 5; i++){ //does not include sheen
            int value = berryTotal[i] - berryTotal[(i+1 > 4? 0: i+1)];
            if(value < 0){
                negs += 1;
            }
            berryCalc[i] = value;
        }
        for(int i = 0; i < 5; i++){ //setting bounds & negs
            berryCalc[i] = Math.min(Math.max(0, berryCalc[i]- negs) , 99);
        }
        berryCalc[5] = (berryCalc[5]/numBerries) - numBerries; //calculate smoothness
        int mainFlavor = 0;
        int secondaryFlavor = -1;

        for(int i = 0; i < 5; i++){
            if(berryCalc[i] > berryCalc[mainFlavor]){
                if(berryCalc[mainFlavor] > 0){
                    secondaryFlavor = mainFlavor;
                }
                mainFlavor = i;
            }else if((mainFlavor != i) && ((berryCalc[i] > 0 && secondaryFlavor == -1) ||
                    (secondaryFlavor > 0  && berryCalc[i] > berryCalc[secondaryFlavor] && berryCalc[i] > 0))){
                secondaryFlavor = i;
            }
        }

        ItemStack poffin = getPoffinType(mainFlavor, secondaryFlavor);
        CompoundTag compound;
        if (!poffin.has(DataComponents.CUSTOM_DATA)) {
            compound = new CompoundTag();

            //poffin.setTag(compound);
        }else{
            compound = poffin.get(DataComponents.CUSTOM_DATA).copyTag();
        }
        CompoundTag poffinTag = new CompoundTag();
        poffinTag.putInt("spicy", berryCalc[0]);
        poffinTag.putInt("dry", berryCalc[1]);
        poffinTag.putInt("sweet", berryCalc[2]);
        poffinTag.putInt("bitter", berryCalc[3]);
        poffinTag.putInt("sour", berryCalc[4]);
        poffinTag.putInt("sheen", berryCalc[5]);

        compound.put("Flavors", poffinTag);
        CustomData.set(DataComponents.CUSTOM_DATA, poffin, compound);

        //assign nbt data
        return poffin;
    }

    public static ItemStack getPoffinType(int mainFlavor, int secFlavor){
        switch (mainFlavor){
            case 0:
                switch (secFlavor){
                    case 1:
                        return new ItemStack(ItemInit.SPICY_DRY_POFFIN.get());
                    case 2:
                        return new ItemStack(ItemInit.SPICY_SWEET_POFFIN.get());
                    case 3:
                        return new ItemStack(ItemInit.SPICY_BITTER_POFFIN.get());
                    case 4:
                        return new ItemStack(ItemInit.SPICY_SOUR_POFFIN.get());
                    default:
                        return new ItemStack(ItemInit.SPICY_POFFIN.get());
                }
            case 1:
                switch (secFlavor){
                    case 0:
                        return new ItemStack(ItemInit.DRY_SPICY_POFFIN.get());
                    case 2:
                        return new ItemStack(ItemInit.DRY_SWEET_POFFIN.get());
                    case 3:
                        return new ItemStack(ItemInit.DRY_BITTER_POFFIN.get());
                    case 4:
                        return new ItemStack(ItemInit.DRY_SOUR_POFFIN.get());
                    default:
                        return new ItemStack(ItemInit.DRY_POFFIN.get());
                }
            case 2:
                switch (secFlavor){
                    case 0:
                        return new ItemStack(ItemInit.SWEET_SPICY_POFFIN.get());
                    case 1:
                        return new ItemStack(ItemInit.SWEET_DRY_POFFIN.get());
                    case 3:
                        return new ItemStack(ItemInit.SWEET_BITTER_POFFIN.get());
                    case 4:
                        return new ItemStack(ItemInit.SWEET_SOUR_POFFIN.get());
                    default:
                        return new ItemStack(ItemInit.SWEET_POFFIN.get());
                }
            case 3:
                switch (secFlavor){
                    case 0:
                        return new ItemStack(ItemInit.BITTER_SPICY_POFFIN.get());
                    case 1:
                        return new ItemStack(ItemInit.BITTER_DRY_POFFIN.get());
                    case 2:
                        return new ItemStack(ItemInit.BITTER_SWEET_POFFIN.get());
                    case 4:
                        return new ItemStack(ItemInit.BITTER_SOUR_POFFIN.get());
                    default:
                        return new ItemStack(ItemInit.BITTER_POFFIN.get());
                }
            case 4:
                switch (secFlavor){
                    case 0:
                        return new ItemStack(ItemInit.SOUR_SPICY_POFFIN.get());
                    case 1:
                        return new ItemStack(ItemInit.SOUR_DRY_POFFIN.get());
                    case 2:
                        return new ItemStack(ItemInit.SOUR_SWEET_POFFIN.get());
                    case 3:
                        return new ItemStack(ItemInit.SOUR_BITTER_POFFIN.get());
                    default:
                        return new ItemStack(ItemInit.SOUR_POFFIN.get());
                }
            default:
                return new ItemStack(ItemInit.FOUL_POFFIN.get());
        }
    }

    public static boolean isDuplicateBerry(ItemStack berries1, ItemStack berries2, ItemStack berries3, ItemStack berries4){
        boolean firstBerry = !berries1.isEmpty() && ((!berries2.isEmpty() && berries1.getItem().equals(berries2.getItem())) ||
                (!berries3.isEmpty() && berries1.getItem().equals(berries3.getItem())) || (!berries4.isEmpty() && berries1.getItem().equals(berries4.getItem())));
        boolean secBerry = !berries2.isEmpty() && ((!berries3.isEmpty() && berries2.getItem().equals(berries3.getItem())) || (!berries4.isEmpty() && berries2.getItem().equals(berries4.getItem())));
        boolean thirdBerry = !berries3.isEmpty() && (!berries4.isEmpty() && berries3.getItem().equals(berries4.getItem()));

        return firstBerry || secBerry || thirdBerry;
    }

    private static int[] addValues(int[] total, int[] adding){
        int[] result = new int[]{0, 0, 0, 0, 0, 0};
        for(int i = 0; i < 6; i++){
            result[i] = total[i] + adding[i];
        }
        return result;
    }

    public int[] getBerryFlavor(Item berry){
        if(berryFlavors.containsKey(berry)){
            return berryFlavors.get(berry);
        }else {
            System.out.println("Invalid flavor entry for berry item used, defaulting to 0");
            return new int[]{0, 0, 0, 0, 0};
        }
    }

    //https://bulbapedia.bulbagarden.net/wiki/Flavor
    public static Map<Item, int[]> berryFlavors = new HashMap<Item, int[]>() {{
        put(CobblemonItems.CHERI_BERRY, new int[]{10, 0, 0, 0, 0, 25});
        put(CobblemonItems.CHESTO_BERRY, new int[]{0, 10, 0, 0, 0, 25});
        put(CobblemonItems.PECHA_BERRY, new int[]{0, 0, 10, 0, 0, 25});
        put(CobblemonItems.RAWST_BERRY, new int[]{0, 0, 0, 10, 0, 25});
        put(CobblemonItems.ASPEAR_BERRY, new int[]{0, 0, 0, 0, 10, 25});
        put(CobblemonItems.LEPPA_BERRY, new int[]{10, 0, 10, 10, 10, 20});
        put(CobblemonItems.ORAN_BERRY, new int[]{10, 10, 0, 10, 10, 20});
        put(CobblemonItems.PERSIM_BERRY, new int[]{10, 10, 0, 10, 10, 20});
        put(CobblemonItems.LUM_BERRY, new int[]{10, 10, 10, 0, 10, 20});
        put(CobblemonItems.SITRUS_BERRY, new int[]{10, 10, 10, 10, 0, 20});
        put(CobblemonItems.FIGY_BERRY, new int[]{15, 0, 0, 0, 0, 25});
        put(CobblemonItems.WIKI_BERRY, new int[]{0, 15, 0, 0, 0, 25});
        put(CobblemonItems.MAGO_BERRY, new int[]{0, 0, 15, 0, 0, 25});
        put(CobblemonItems.AGUAV_BERRY, new int[]{0, 0, 0, 15, 0, 25});
        put(CobblemonItems.IAPAPA_BERRY, new int[]{0, 0, 0, 0, 15, 25});
        put(CobblemonItems.RAZZ_BERRY, new int[]{10, 10, 0, 0, 0, 20});
        put(CobblemonItems.BLUK_BERRY, new int[]{0, 10, 10, 0, 0, 20});
        put(CobblemonItems.NANAB_BERRY, new int[]{0, 0, 10, 10, 0, 20});
        put(CobblemonItems.WEPEAR_BERRY, new int[]{0, 0, 0, 10, 10, 20});
        put(CobblemonItems.PINAP_BERRY, new int[]{10, 0, 0, 0, 10, 20});
        put(CobblemonItems.POMEG_BERRY, new int[]{10, 0, 10, 10, 0, 20});
        put(CobblemonItems.KELPSY_BERRY, new int[]{0, 10, 0, 10, 10, 20});
        put(CobblemonItems.QUALOT_BERRY, new int[]{10, 0, 10, 0, 10, 20});
        put(CobblemonItems.HONDEW_BERRY, new int[]{10, 10, 0, 10, 0, 20});
        put(CobblemonItems.GREPA_BERRY, new int[]{0, 10, 10, 0, 10, 20});
        put(CobblemonItems.TAMATO_BERRY, new int[]{20, 10, 0, 0, 0, 30});
        put(CobblemonItems.CORNN_BERRY, new int[]{0, 20, 10, 0, 0, 30});
        put(CobblemonItems.MAGOST_BERRY, new int[]{0, 0, 20, 10, 0, 30});
        put(CobblemonItems.RABUTA_BERRY, new int[]{0, 0, 0, 20, 10, 30});
        put(CobblemonItems.NOMEL_BERRY, new int[]{10, 0, 0, 0, 20, 30});
        put(CobblemonItems.SPELON_BERRY, new int[]{30, 10, 0, 0, 0, 35});
        put(CobblemonItems.PAMTRE_BERRY, new int[]{0, 30, 10, 0, 0, 35});
        put(CobblemonItems.WATMEL_BERRY, new int[]{0, 0, 30, 10, 0, 35});
        put(CobblemonItems.DURIN_BERRY, new int[]{0, 0, 0, 30, 10, 35});
        put(CobblemonItems.BELUE_BERRY, new int[]{10, 0, 0, 0, 30, 35});
        put(CobblemonItems.OCCA_BERRY, new int[]{15, 0, 10, 0, 0, 30});
        put(CobblemonItems.PASSHO_BERRY, new int[]{0, 15, 0, 10, 0, 30});
        put(CobblemonItems.WACAN_BERRY, new int[]{0, 0, 15, 0, 10, 30});
        put(CobblemonItems.RINDO_BERRY, new int[]{10, 0, 0, 15, 0, 30});
        put(CobblemonItems.YACHE_BERRY, new int[]{0, 10, 0, 0, 15, 30});
        put(CobblemonItems.CHOPLE_BERRY, new int[]{15, 0, 0, 10, 0, 30});
        put(CobblemonItems.KEBIA_BERRY, new int[]{0, 15, 0, 0, 10, 30});
        put(CobblemonItems.SHUCA_BERRY, new int[]{10, 0, 15, 0, 0, 30});
        put(CobblemonItems.COBA_BERRY, new int[]{0, 10, 0, 15, 0, 30});
        put(CobblemonItems.PAYAPA_BERRY, new int[]{0, 0, 10, 0, 15, 30});
        put(CobblemonItems.TANGA_BERRY, new int[]{20, 0, 0, 0, 10, 35});
        put(CobblemonItems.CHARTI_BERRY, new int[]{10, 20, 0, 0, 0, 35});
        put(CobblemonItems.KASIB_BERRY, new int[]{0, 10, 20, 0, 0, 35});
        put(CobblemonItems.HABAN_BERRY, new int[]{0, 0, 10, 20, 0, 35});
        put(CobblemonItems.COLBUR_BERRY, new int[]{0, 0, 0, 10, 20, 35});
        put(CobblemonItems.BABIRI_BERRY, new int[]{25, 10, 0, 0, 0, 35});
        put(CobblemonItems.CHILAN_BERRY, new int[]{0, 25, 10, 0, 0, 35});
        put(CobblemonItems.LIECHI_BERRY, new int[]{30, 10, 30, 0, 0, 40});
        put(CobblemonItems.GANLON_BERRY, new int[]{0, 30, 10, 30, 0, 40});
        put(CobblemonItems.SALAC_BERRY, new int[]{0, 0, 30, 10, 30, 40});
        put(CobblemonItems.PETAYA_BERRY, new int[]{30, 0, 0, 30, 10, 40});
        put(CobblemonItems.APICOT_BERRY, new int[]{10, 30, 0, 0, 30, 40});
        put(CobblemonItems.LANSAT_BERRY, new int[]{30, 10, 30, 10, 30, 50});
        put(CobblemonItems.STARF_BERRY, new int[]{30, 10, 30, 10, 30, 50});
        put(CobblemonItems.ENIGMA_BERRY, new int[]{40, 10, 0, 0, 0, 60});
        put(CobblemonItems.MICLE_BERRY, new int[]{0, 40, 10, 0, 0, 60});
        put(CobblemonItems.CUSTAP_BERRY, new int[]{0, 0, 40, 10, 0, 60});
        put(CobblemonItems.JABOCA_BERRY, new int[]{0, 0, 0, 40, 10, 60});
        put(CobblemonItems.ROWAP_BERRY, new int[]{10, 0, 0, 0, 40, 60});
        put(CobblemonItems.ROSELI_BERRY, new int[]{0, 0, 25, 10, 0, 35});
        put(CobblemonItems.KEE_BERRY, new int[]{30, 30, 10, 10, 10, 50}); //does not have an actual hardness value
        put(CobblemonItems.MARANGA_BERRY, new int[]{10, 10, 30, 30, 10, 50}); //does not have an actual hardness value
        put(CobblemonItems.TOUGA_BERRY, new int[]{40, 0, 0, 0, 0, 65}); //based on gen III which might need to be changed for balance
        put(CobblemonItems.HOPO_BERRY, new int[]{0, 0, 30, 0, 30, 85}); //does not have actual flavor or hardness, based on gen III Topo berry
    }};

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        Iterator var1 = this.items.iterator();
        ItemStack itemstack;
        do {
            if (!var1.hasNext()) {
                return true;
            }
            itemstack = (ItemStack)var1.next();
        } while(itemstack.isEmpty());
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return i >= 0 && i < this.items.size() ? (ItemStack)this.items.get(i) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return ContainerHelper.removeItem(this.items, i, j);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(this.items, i);
    }

    @Override
    public boolean stillValid(Player arg) {
        return Container.stillValidBlockEntity(this, arg);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    return PoffinPotBlockEntity.this.litTime;
                case 1:
                    return PoffinPotBlockEntity.this.litDuration;
                case 2:
                    return PoffinPotBlockEntity.this.cookingProgress;
                case 3:
                    return PoffinPotBlockEntity.this.cookingTotalTime;
                default:
                    return 0;
            }
        }

        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    PoffinPotBlockEntity.this.litTime = p_58434_;
                    break;
                case 1:
                    PoffinPotBlockEntity.this.litDuration = p_58434_;
                    break;
                case 2:
                    PoffinPotBlockEntity.this.cookingProgress = p_58434_;
                    break;
                case 3:
                    PoffinPotBlockEntity.this.cookingTotalTime = p_58434_;
            }

        }

        public int getCount() {
            return 4;
        }
    };

    public void loadAdditional(CompoundTag pTag, HolderLookup.Provider provider) {
        super.loadAdditional(pTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, provider);
        this.litTime = pTag.getInt("litTime");
        this.cookingProgress = pTag.getInt("CookTime");
        this.cookingTotalTime = pTag.getInt("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));

    }

    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider provider) {
        super.saveAdditional(pTag, provider);
        pTag.putInt("litTime", this.litTime);
        pTag.putInt("CookTime", this.cookingProgress);
        pTag.putInt("CookTimeTotal", this.cookingTotalTime);
        ContainerHelper.saveAllItems(pTag, this.items, provider);
    }

    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }
        if (pIndex == 0 && !flag) {
            this.cookingTotalTime = getTotalCookTime(this.level, this);
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    private boolean canBurn(RegistryAccess pRegistryAccess, ItemStack resultItem, NonNullList<ItemStack> pInventory, int pMaxStackSize) {
        ItemStack berries1 = pInventory.get(BERRY_INPUT1);
        ItemStack berries2 = pInventory.get(BERRY_INPUT2);
        ItemStack berries3 = pInventory.get(BERRY_INPUT3);
        ItemStack berries4 = pInventory.get(BERRY_INPUT4);
        ItemStack dough = pInventory.get(DOUGH_INPUT);
        if (!(berries1.isEmpty() && berries2.isEmpty() && berries3.isEmpty() && berries4.isEmpty()) && !dough.isEmpty()) {
            ItemStack itemstack = resultItem;
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack resultStack = pInventory.get(6);
                if (resultStack.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItem(resultStack, itemstack)) {
                    return false;
                } else if (resultStack.getCount() + itemstack.getCount() <= pMaxStackSize && resultStack.getCount() + itemstack.getCount() <= resultStack.getMaxStackSize()) { // Forge fix: make furnace respect stack sizes in furnace recipes
                    return true;
                } else {
                    return resultStack.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Forge fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    private boolean burn(RegistryAccess pRegistryAccess, ItemStack resultItem, NonNullList<ItemStack> pInventory, int pMaxStackSize) {
        if (resultItem != null && this.canBurn(pRegistryAccess, resultItem, pInventory, pMaxStackSize)) {
            ItemStack berry1 = pInventory.get(BERRY_INPUT1);
            ItemStack berry2 = pInventory.get(BERRY_INPUT2);
            ItemStack berry3 = pInventory.get(BERRY_INPUT3);
            ItemStack berry4 = pInventory.get(BERRY_INPUT4);
            ItemStack dough = pInventory.get(DOUGH_INPUT);
            ItemStack itemstack2 = pInventory.get(POFFIN_SLOT_RESULT);
            if (itemstack2.isEmpty()) {
                pInventory.set(POFFIN_SLOT_RESULT, resultItem.copy());
            } else if (itemstack2.is(resultItem.getItem())) {
                itemstack2.grow(resultItem.getCount());
            }

            if(!berry1.isEmpty()){
                berry1.shrink(1);
            }
            if(!berry2.isEmpty()){
                berry2.shrink(1);
            }
            if(!berry3.isEmpty()){
                berry3.shrink(1);
            }
            if(!berry4.isEmpty()){
                berry4.shrink(1);
            }
            dough.shrink(1);

            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack pFuel) {
        if (pFuel.isEmpty()) {
            return 0;
        } else {
            Item item = pFuel.getItem();
            //return net.minecraftforge.common.ForgeHooks.getBurnTime(pFuel, RecipeType.SMELTING);
            return (Integer)getFuel().getOrDefault(item, 0);
        }
    }

    private static int getTotalCookTime(Level pLevel, PoffinPotBlockEntity pBlockEntity) {
        return 200;
    }

    @Override
    public int[] getSlotsForFace(Direction arg) {
        if (arg == Direction.DOWN) {
            return SLOTS_4_DOWN;
        } else {
            return arg == Direction.UP ? SLOTS_4_UP : SLOTS_4_SIDES;
        }
    }

    public boolean canPlaceItem(int i, ItemStack arg) {
        //return true;
        if (i == 6) { //result
            return false;
        } else if (i == 5) { //fuel
            ItemStack itemstack = (ItemStack) this.items.get(5);
            return getBurnDuration(itemstack) > 0; //return ForgeHooks.getBurnTime(arg, RecipeType.SMELTING) > 0;
        }else if(i >= 0 && i<= 3){ //berries
            return arg.is(TagsInit.Items.COBBLEMON_BERRIES);
        }else if(i == 4){ //dough
            return arg.is(ItemInit.POFFIN_DOUGH_BASE.get());
        } else {
            return false;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack arg, @Nullable Direction arg2) {
        System.out.println("Direction: " + arg2.toString());
        if (i == 6) { //result
            return false;
        } else if (i == 5 && arg2 == Direction.NORTH) { //fuel
            ItemStack itemstack = (ItemStack) this.items.get(5);
            return true;//!arg.isEmpty() || ForgeHooks.getBurnTime(arg, RecipeType.SMELTING) > 0;
        }else if(i >= 0 && i<= 3 && arg2 == Direction.UP){ //berries
            return arg.is(TagsInit.Items.COBBLEMON_BERRIES);
        }else if(i == 4 && arg2 == Direction.UP){ //dough
            return arg.is(ItemInit.POFFIN_DOUGH_BASE);
        } else {
            return false;
        }


        //return this.canPlaceItem(i, arg);
    }


    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack arg, Direction arg2) {
        if (arg2 == Direction.DOWN && i == 5) {
            return arg.is(Items.WATER_BUCKET) || arg.is(Items.BUCKET);
        }else if(arg2 == Direction.DOWN && i == 6){
            return true;
        } else {
            return false;
        }
    }

}
