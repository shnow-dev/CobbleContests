package com.raspix.neoforge.cobble_contests.items;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.advancement.CobblemonCriteria;
import com.cobblemon.mod.common.advancement.criterion.PokemonInteractContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.callback.PartySelectCallbacks;
import com.cobblemon.mod.common.api.cooking.Flavour;
import com.cobblemon.mod.common.api.item.PokemonSelectingItem;
import com.cobblemon.mod.common.api.reactive.SimpleObservable;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.item.battle.BagItem;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.DataKeys;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PoffinItem extends CobblemonItem implements PokemonSelectingItem {


    public static final String cvsKey = "CVs";
    private final String coolKey = "cool";
    private int mainFlavour = -1;
    private int secFlavour = -1;

    public PoffinItem(Properties properties) {
        super(properties);
        this.mainFlavour = -1;
        this.secFlavour = -1;
    }

    public PoffinItem(Properties properties, int mainFlavour, int secFlavour) {
        super(properties);
        this.mainFlavour = mainFlavour;
        this.secFlavour = secFlavour;
    }

    private void setBasicFlavours(){
    }

    @Nullable
    @Override
    public BagItem getBagItem() {
        return new BagItem() {

            @Override
            public @NotNull Item getReturnItem() {
                return null;
            }

            @NotNull
            @Override
            public String getItemName() {
                return "item.cobblemon.poffin";
            }

            @Override
            public boolean canUse(@NotNull ItemStack itemStack, @NotNull PokemonBattle pokemonBattle, @NotNull BattlePokemon battlePokemon) {
                return true;
            }

            @NotNull
            @Override
            public String getShowdownInput(@NotNull BattleActor battleActor, @NotNull BattlePokemon battlePokemon, @Nullable String s) {
                return null;
            }

            /**@Override
            public boolean canStillUse(@NotNull ServerPlayer serverPlayer, @NotNull PokemonBattle pokemonBattle, @NotNull BattleActor battleActor, @NotNull BattlePokemon battlePokemon, @NotNull ItemStack itemStack) {
                return  itemStack.getCount() > 0 && canUse(pokemonBattle, battlePokemon) && battleActor.canFitForcedAction();
            }*/
        };
    }

    @Override
    public void applyToBattlePokemon(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull BattlePokemon battlePokemon) {

    }

    @Nullable
    @Override
    public InteractionResultHolder<ItemStack> applyToPokemon(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull Pokemon pokemon) {
        CVs cvs;
        CompoundTag tag = pokemon.getPersistentData();
        if(tag.getCompound(cvsKey) == null){
            cvs = CVs.createNewCVs();
        }else {
            cvs = CVs.getFromTag(tag.getCompound(cvsKey));
            System.out.println(cvs.getAsString());
        }

        if(cvs.getSheen() < 255 || itemStack.getItem() == ItemInit.FOUL_POFFIN.get()) {
            // spicy, dry, sweet, bitter, sour, sheen
            int[] Flavours = {0, 0, 0, 0, 0, 0};
            if(itemStack.has(DataComponents.CUSTOM_DATA) && itemStack.get(DataComponents.CUSTOM_DATA).contains("Flavours")) {
                CompoundTag poffinTag = itemStack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Flavours");
                try {
                    Flavours[0] = poffinTag.getInt("spicy");
                    Flavours[1] = poffinTag.getInt("dry");
                    Flavours[2] = poffinTag.getInt("sweet");
                    Flavours[3] = poffinTag.getInt("bitter");
                    Flavours[4] = poffinTag.getInt("sour");
                    Flavours[5] = poffinTag.getInt("sheen");
                } catch (ClassCastException e) {

                }
            }else {
                Flavours = getBaseFlavours();
            }
            System.out.println(itemStack.getTags().toList().size());
            for(TagKey key : itemStack.getTags().toList()){
                System.out.println(key);
            }
            System.out.println("Applying: " + Arrays.toString(Flavours));

            Nature nature = pokemon.getNature();
            int disliked = getIndexFromFlavour(nature.getDislikedFlavour());
            int liked = getIndexFromFlavour(nature.getFavouriteFlavour());
            float valMultiplier = 1.0f;
            if(this.mainFlavour == liked && mainFlavour >= 0 && mainFlavour != secFlavour){ //liked and has 2 Flavours
                System.out.println("liked Flavour");
                valMultiplier = 1.1f;
                pokemon.incrementFriendship(5, true);
            }else if(this.mainFlavour == disliked && mainFlavour >= 0 && mainFlavour != secFlavour){ //disliked and has 2 Flavours
                System.out.println("disliked Flavour");
                valMultiplier = 0.9f;
                pokemon.decrementFriendship(1, true);
            }else if(this.mainFlavour == -1 && this.secFlavour == -1){ //no Flavour so foul
                pokemon.decrementFriendship(20, true);
            }else {
                pokemon.incrementFriendship(1, true); //no opinion on Flavour
            }

            // need to change this so that all get buff if the primary Flavour of the poffin is fav/hated
            for (int i = 0; i < 5; i++) {
                int valBonus = (int) ((float)Flavours[i] * valMultiplier);
                cvs.increaseCVFromFlavorIndex(i, valBonus);
            }
            cvs.increaseSheen(Flavours[5]);

            Map<String, CompoundTag> myData = new HashMap<String, CompoundTag>() {};
            myData.put(cvsKey, cvs.saveToNBT());
            saveCVs(pokemon, myData);

            if (!serverPlayer.isCreative()) {
                itemStack.shrink(1);
            }
            return InteractionResultHolder.success(itemStack);
        }else {
            serverPlayer.displayClientMessage(Component.literal(pokemon.getDisplayName(false).getString() + " already has max sheen and can not eat any more").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return InteractionResultHolder.fail(itemStack);
    }

    private void saveMyData(final Pokemon pokemon, final Map<String, Integer> myData) {

        final CompoundTag tag = pokemon.getPersistentData();
        myData.forEach((key, value) -> {
            tag.put(key, IntTag.valueOf(value));
        });
        // basically a vanilla "markAsDirty"
        /**if (pokemon.getChangeObservable() instanceof SimpleObservable<Pokemon>) { //TODO
            ((SimpleObservable<Pokemon>) pokemon.getChangeObservable()).emit(pokemon);
        }else {
            System.out.println("error, not simple observable (PoffinItem)");
        }*/
    }

    private void saveCVs(final Pokemon pokemon, final Map<String, CompoundTag> myData) {
        final CompoundTag tag = pokemon.getPersistentData();
        myData.forEach((key, value) -> {
            tag.put(key, value);
        });
        // basically a vanilla "markAsDirty"
        /**if (pokemon.getChangeObservable() instanceof SimpleObservable<Pokemon>) { //TODO
            ((SimpleObservable<Pokemon>) pokemon.getChangeObservable()).emit(pokemon);
        }else {
            System.out.println("error, not simple observable (PoffinItem)");
        }*/
    }

    private void getMyData(Pokemon pokemon, String key){

    }

    private int getIndexFromFlavour(Flavour Flavour){
        int idx = -1;
        if(Flavour != null){
            switch (Flavour) {
                case SPICY:
                    idx = 0;
                    break;
                case DRY:
                    idx = 1;
                    break;
                case SWEET:
                    idx = 2;
                    break;
                case BITTER:
                    idx = 3;
                    break;
                case SOUR:
                    idx = 4;
                    break;
                default:
                    System.out.println("Unknown Flavour.");
                    break;
            }
        }
        return idx;
    }


    @Override
    public boolean canUseOnBattlePokemon(ItemStack itemstack, @NotNull BattlePokemon battlePokemon) {
        return false;
    }

    @Override
    public boolean canUseOnPokemon(ItemStack itemstack, @NotNull Pokemon pokemon) {
        return true;
    }


    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> interactGeneralBattle(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull BattleActor battleActor) {
        return null;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> interactWithSpecificBattle(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull BattlePokemon battlePokemon) {
        return null;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack) {
        InteractionResultHolder<ItemStack> result = interactGeneral(serverPlayer, itemStack);//use(serverPlayer, itemStack);
        return result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide()){
            return super.useOn(context);
        }
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        ItemStack itemStack = context.getItemInHand();



        return interactGeneral(player, itemStack).getResult();//super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level arg, Player player, InteractionHand hand) {
        if(player instanceof ServerPlayer){
            this.use((ServerPlayer) player, player.getItemInHand(hand));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> interactGeneral(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack) {
        List<Pokemon> pokeList1 = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer).toGappyList();
        if(pokeList1.isEmpty()){
            return InteractionResultHolder.fail(itemStack);
        }
        List<Pokemon> pokeList = new ArrayList<Pokemon>();
        for (Pokemon poke: pokeList1) {
            if (poke != null){
                pokeList.add(poke);
            }
        }
        PartySelectCallbacks.INSTANCE.createFromPokemon(
                serverPlayer,
                pokeList,
                pk -> canUseOnPokemon(itemStack, pk),
                pk -> {
                    if (true) {
                        applyToPokemon(serverPlayer, itemStack, pk);
                        CobblemonCriteria.POKEMON_INTERACT.trigger(serverPlayer,
                                new PokemonInteractContext(
                                        pk.getSpecies().resourceIdentifier, ItemInit.POFFIN_DOUGH_BASE.getId()));// Registries.ITEM.getId(itemStack.getItem())  itemStack.getItem().
                    }
                    return null;
                }
        );
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, tooltipContext, list, tooltipFlag);
        if(itemStack.has(DataComponents.CUSTOM_DATA) && itemStack.get(DataComponents.CUSTOM_DATA).contains("Flavours")) {
            CompoundTag poffinTag = itemStack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Flavours");
            try {
                int spicy = poffinTag.getInt("spicy");
                int dry = poffinTag.getInt("dry");
                int sweet = poffinTag.getInt("sweet");
                int bitter = poffinTag.getInt("bitter");
                int sour = poffinTag.getInt("sour");
                int smoothness = poffinTag.getInt("sheen");
                list.add(Component.translatable("tooltip.cobble_contests.poffin_item.tooltip.poffin_stats", spicy, dry, sweet, bitter, sour, smoothness).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.LIGHT_PURPLE));
            } catch (ClassCastException e) {
            }
        }else {
            int[] baseFlavours = getBaseFlavours();
            list.add(Component.translatable("tooltip.cobble_contests.poffin_item.tooltip.poffin_stats", baseFlavours[0], baseFlavours[1], baseFlavours[2], baseFlavours[3], baseFlavours[4], baseFlavours[5]).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * For poffins that were not assigned data, like those in creative
     * @return
     */
    private int[] getBaseFlavours(){
        int[] baseFlavours = {0, 0, 0, 0, 0, 20};
        if(mainFlavour >= 0 && mainFlavour <6 && secFlavour == -1){ // only one Flavour
            baseFlavours[mainFlavour] = 15;
        }else if(mainFlavour >= 0 && mainFlavour <6){ //main Flavour with sec
            baseFlavours[mainFlavour] = 10;
        }
        if(secFlavour >= 0 && secFlavour <6){ //sec Flavour
            baseFlavours[secFlavour] = 5;
        }
        if(mainFlavour == -1 && secFlavour == -1){ //foul
            baseFlavours = new int[]{-10, -10, -10, -10, -10, -30};
        }
        return baseFlavours;
    }
}
