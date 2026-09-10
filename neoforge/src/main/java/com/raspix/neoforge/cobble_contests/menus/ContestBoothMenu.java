package com.raspix.neoforge.cobble_contests.menus;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;

import com.raspix.neoforge.cobble_contests.blocks.BlockInit;
import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import com.raspix.neoforge.cobble_contests.network.SBRunContest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class ContestBoothMenu extends AbstractContainerMenu {

    private final ContestBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private PlayerPartyStore playerPartyStore;
    private ClientParty playerPartyClient;

    //server constructor
    public ContestBoothMenu(int containerID, Inventory playerInv, BlockEntity blockEntity){
        super(MenuInit.CONTEST_MENU.get(), containerID);
        if(blockEntity instanceof ContestBlockEntity be){
            this.blockEntity = be;
        }else {
            throw new IllegalStateException("Incorrect block entity class (%s) passed into ContestMenu"
                    .formatted(blockEntity.getClass().getCanonicalName()));
        }
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        //try {
            UUID id = playerInv.player.getUUID();
            //playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(id);
            playerPartyClient = CobblemonClient.INSTANCE.getStorage().getParty();
        /**}catch (NoPokemonStoreException e){
            System.out.println("you failed");
        }*/


        //System.out.println("hewwo");
        createTestMenu();

    }

    private void createTestMenu() {


    }

    //client constructor
    protected ContestBoothMenu(int containerID, Inventory playerInv, FriendlyByteBuf additionalData) {
        this(containerID, playerInv, playerInv.player.level().getBlockEntity(additionalData.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player arg, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, BlockInit.CONTEST_BOOTH.get());
    }

    public ContestBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean playerStartHosting(UUID id){
        return this.blockEntity.tryHosting(id);
    }

    public boolean hostSelectType(UUID id, int type){
        return this.blockEntity.setContestType(id, type);
    }

    public PlayerPartyStore getPartyStore(){
        return playerPartyStore;
    }

    public ClientParty getPartyClient(){
        return playerPartyClient;
    }

    public void startContest(int color, int pokemonIdx, UUID player){

        this.blockEntity.tryHosting(player);
        this.blockEntity.startContest(color, player);
        this.blockEntity.addContestant(player, pokemonIdx);
        System.out.println(this.blockEntity.getCurrentContestInfo());
    }

    public String getContestResults(){
        return this.blockEntity.getContestResults();
    }

    public void startStatAssesment(UUID player, int pokemonIdx, int contestType){
        //PacketHandler.sendToServer(new CBERunContest(player, pokemonIdx, blockEntity.getBlockPos(), contestType, 0));
        PacketDistributor.sendToServer(new SBRunContest(player, pokemonIdx, blockEntity.getBlockPos(), contestType, 0));
    }
}
