package com.raspix.neoforge.cobble_contests.items;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.raspix.neoforge.cobble_contests.menus.PlayerConditionCardMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ContestWallet extends Item {
    public ContestWallet(Properties arg) {
        super(arg);
    }

    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        pPlayer.openItemGui(itemstack, pHand);
        if(!pLevel.isClientSide()){
            pPlayer.openMenu(getMenuProvider(pPlayer));
            //NetworkHooks.openScreen((ServerPlayer) pPlayer, getMenuProvider(pPlayer));
            //PacketHandler.sendToClient(new CBInfoScreenParty(pPlayer.getUUID()), (() -> (ServerPlayer) pPlayer));
            //PacketDistributor.sendToPlayer((ServerPlayer) pPlayer, new CBWalletScreenParty(pPlayer.getUUID(), null));
            //PacketDistributor.sendToServer(new CBWalletScreenParty(pPlayer.getUUID(), null));

        }else {

        }

        pPlayer.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    public MenuProvider getMenuProvider(Player player1) {
        PlayerPartyStore playerPartyStore = null;
        /**try {
            playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player1.getUUID());
            System.out.println("test 3");
        }catch (NoPokemonStoreException e){
            System.out.println("Oopsie tootsie");
        }*/
        //return new SimpleMenuProvider((containerId, playerInventory, player) -> new PlayerContestInfoMenu(containerId, playerInventory), this.getDisplayName()); //
        return new SimpleMenuProvider((containerId, playerInventory, player) -> new PlayerConditionCardMenu(containerId, playerInventory), this.getDisplayName()); //
    }

    public Component getDisplayName() {
        return Component.translatable("Contest Info");
        /**return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName()).withStyle((p_185975_) -> {
         return p_185975_.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID());
         });*/
    }

}
