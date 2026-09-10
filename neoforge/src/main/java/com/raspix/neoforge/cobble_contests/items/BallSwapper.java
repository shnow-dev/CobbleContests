package com.raspix.neoforge.cobble_contests.items;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.advancement.CobblemonCriteria;
import com.cobblemon.mod.common.advancement.criterion.PokemonInteractContext;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.callback.PartySelectCallbacks;
import com.cobblemon.mod.common.api.item.PokemonSelectingItem;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.item.battle.BagItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.raspix.neoforge.cobble_contests.items.ItemInit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class BallSwapper extends Item implements PokemonSelectingItem {

    private PokeBall ballType;
    private String ballCode = "Ball";


    public BallSwapper(Properties arg) {
        super(arg);
    }


    @Nullable
    @Override
    public BagItem getBagItem() {
        return null;
    }

    @Override
    public void applyToBattlePokemon(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull BattlePokemon battlePokemon) {

    }

    @Nullable
    @Override
    public InteractionResultHolder<ItemStack> applyToPokemon(@NotNull ServerPlayer serverPlayer, @NotNull ItemStack itemStack, @NotNull Pokemon pokemon) {
        PokeBall newBall;
        CustomData dat = itemStack.get(DataComponents.CUSTOM_DATA);
        if(dat == null || !dat.contains(ballCode)){
            newBall = CobblemonItems.POKE_BALL.getPokeBall();
            //System.out.println("missing CVs, generated");
        }else {
            String ballLoc = dat.getUnsafe().getString(ballCode);
            newBall = Optional.ofNullable(PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(ballLoc))).orElse(PokeBalls.INSTANCE.getPOKE_BALL());
            //newBall = ball;//CVs.getFromTag(tag.getCompound("Ball"));
            //System.out.println(newBall.toString());
        }

        PokeBall oldBall = pokemon.getCaughtBall();

        if(newBall != oldBall){

            pokemon.setCaughtBall(newBall);
            //in release version clear ball wand's ball
            return InteractionResultHolder.success(itemStack);
        }else {
            serverPlayer.displayClientMessage(Component.translatable("cobble_contests.ball_swapper.same_ball", pokemon.getDisplayName(false).getString()).withStyle(ChatFormatting.GRAY), false);
        }
        return InteractionResultHolder.fail(itemStack);
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
        ItemStack itemStackOffhand = serverPlayer.getOffhandItem();
        Level level = serverPlayer.level();
        if(swapBall(itemStack, itemStackOffhand, level, serverPlayer.position())){
            return InteractionResultHolder.success(itemStack);
        }else {
            return interactGeneral(serverPlayer, itemStack);//super.useOn(context);
        }
        //InteractionResultHolder<ItemStack> result = interactGeneral(serverPlayer, itemStack);//use(serverPlayer, itemStack);
        //return result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide()){
            return super.useOn(context);
        }
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        ItemStack itemStackOffhand = context.getPlayer().getOffhandItem();
        Level level = context.getLevel();

        if(swapBall(itemStack, itemStackOffhand, level, player.position())){
            return InteractionResult.SUCCESS;
        }else {
            return interactGeneral(player, itemStack).getResult();//super.useOn(context);
        }
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
                                        pk.getSpecies().resourceIdentifier, ItemInit.BALL_SWAPPER.getId()));// Registries.ITEM.getId(itemStack.getItem())  itemStack.getItem().
                    }
                    return null;
                }
        );
        return InteractionResultHolder.success(itemStack);
    }

    /**
     * returns true if there was a ball in offhand
     * @param thisWand
     * @param newBallStack
     * @param level
     * @param pos
     * @return
     */
    private boolean swapBall(ItemStack thisWand, ItemStack newBallStack, Level level, Vec3 pos){
        if(newBallStack.getItem() instanceof PokeBallItem pb){

            CustomData dat = thisWand.get(DataComponents.CUSTOM_DATA);
            if(dat != null && dat.contains(ballCode)){
                String ballLoc = dat.getUnsafe().getString(ballCode);
                PokeBall ball = Optional.ofNullable(PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(ballLoc))).orElse(PokeBalls.INSTANCE.getPOKE_BALL());
                //drop stored ball
                ItemEntity oldBall = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(ball.item()));
                oldBall.setPickUpDelay(40);
                oldBall.setDeltaMovement(oldBall.getDeltaMovement().multiply(0, 1, 0));

                level.addFreshEntity(oldBall);
            }
            CompoundTag ballTag = new CompoundTag();
            ballTag.putString(ballCode, pb.getPokeBall().getName().toString());// set new ball
            CustomData.set(DataComponents.CUSTOM_DATA, thisWand, ballTag);
            //dat.
            //thisWand.setTag(ballTag);
            newBallStack.shrink(1); //take from stack
            return true;
        }else {
            return false;
        }
        //return false;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
        //public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(itemStack, tooltipContext, list, tooltipFlag);
        CustomData dat = itemStack.get(DataComponents.CUSTOM_DATA);
        if(dat != null) {
            CompoundTag nbt = dat.getUnsafe();
            for (String tagInfo : nbt.getAllKeys()) {
                if (tagInfo.contains(ballCode)) {
                    String ballLoc = nbt.getString(ballCode);
                    PokeBall ball = Optional.ofNullable(PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(ballLoc))).orElse(PokeBalls.INSTANCE.getPOKE_BALL());
                    try {
                        list.add(Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.ball_type", ball.stack(1).getDisplayName()).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
                    } catch (ClassCastException e) {
                    }
                }
            }
        }else {
            list.add(Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.ball_type", Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.empty").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY)); //Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.empty").getString()
        }
        list.add(Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.instructions1").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        list.add(Component.translatable("tooltip.cobble_contests.ball_swapper.tooltip.instructions2").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));

    }
}
