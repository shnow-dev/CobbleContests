package com.raspix.neoforge.cobble_contests.menus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PlayerConditionCardMenu extends AbstractContainerMenu {

    public PlayerConditionCardMenu(int i) {
        super(MenuInit.PLAYER_CONTEST_INFO_MENU.get(), i);
    }

    public PlayerConditionCardMenu(int containerID, Inventory playerInv){
        super(MenuInit.PLAYER_CONTEST_INFO_MENU.get(), containerID);
    }

    protected PlayerConditionCardMenu(int containerID, Inventory playerInv, FriendlyByteBuf additionalData) {
        this(containerID, playerInv);
    }

    @Override
    public ItemStack quickMoveStack(Player arg, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player arg) {
        return true;
    }

}
