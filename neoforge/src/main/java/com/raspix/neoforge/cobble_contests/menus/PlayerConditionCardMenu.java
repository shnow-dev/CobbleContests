package com.raspix.neoforge.cobble_contests.menus;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.raspix.neoforge.cobble_contests.events.ContestMoves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PlayerConditionCardMenu extends AbstractContainerMenu {

    private PlayerPartyStore playerPartyStore;
    private ContestMoves cMoves; //should be made somewhere else, but not sure where
    private ClientParty playerPartyClient;
    public PlayerConditionCardMenu(int i) {
        super(MenuInit.PLAYER_CONTEST_INFO_MENU.get(), i);
        //playerPartyClient = CobblemonClient.INSTANCE.getStorage().getMyParty();
        /**this.playerPartyStore = playerPartyStore;
        if(playerPartyStore == null){
            System.out.println("is null");
        }else {
            System.out.println("not null");
        }*/
    }

    public PlayerConditionCardMenu(int containerID, Inventory playerInv){
        super(MenuInit.PLAYER_CONTEST_INFO_MENU.get(), containerID);
        //PlayerPartyStore pps =  Cobblemon.INSTANCE.getStorage().getParty((ServerPlayer) playerInv.player);
        /**playerPartyClient = CobblemonClient.INSTANCE.getStorage().getMyParty();
        if(playerPartyClient == null){
            System.out.println("is null");
        }else {
            System.out.println("not null");
        }*/
        //createMenu();
    }

    protected PlayerConditionCardMenu(int containerID, Inventory playerInv, FriendlyByteBuf additionalData) {
        this(containerID, playerInv);
    }

    private void createMenu() {
        /**try {
            cMoves = new ContestMoves();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

    }

    @Override
    public ItemStack quickMoveStack(Player arg, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player arg) {
        return true;
    }

    public ClientParty getPartyClient(){
        return playerPartyClient;
    }

    public PlayerPartyStore getPartyStore(){
        return playerPartyStore;
    }

}
