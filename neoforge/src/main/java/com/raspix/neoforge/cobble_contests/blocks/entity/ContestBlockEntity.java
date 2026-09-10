package com.raspix.neoforge.cobble_contests.blocks.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.reactive.SimpleObservable;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import com.raspix.neoforge.cobble_contests.pokemon.Ribbons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContestBlockEntity extends BlockEntity implements MenuProvider {

    private static final Component TITLE = Component.translatable("container." + CobbleContestsForge.MOD_ID + ".contest_block");

    private boolean isHost;
    private UUID hostId;
    private int contestType;
    private final Map<UUID, ContestParticipation> participants = new HashMap<>();



    public ContestBlockEntity(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    public ContestBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityInit.CONTEST_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    /**@Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
    }*/

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerID, @NotNull Inventory playerInv, Player player) {
        return new ContestBoothMenu(containerID, playerInv, this);
    }

    public boolean tryHosting(UUID id){
        if(!isHost){
            this.hostId = id;
            this.isHost = true;
            return true;
        }else {
            return false;
        }
    }

    public boolean setContestType(UUID id, int type){
        if(this.isHost && Objects.equals(this.hostId, id)){
            contestType = type;
            return true;
        }else if(!this.isHost){
            tryHosting(id);
            contestType = type;
            return true;
        }else {
            return false;
        }
    }

    private void removeHost(){
        this.hostId = null;
        this.isHost = false;
    }

    public void startContest(int color, UUID player){

    }

    public void addContestant(UUID player, int pokemonIdx){

    }

    public String getContestResults(){
        return "cobble_contests.contest_type.cool";
    }

    public void clearContest(){
        participants.clear();
        hostId = null;
        isHost = false;
        contestType = 0;
    }

    public void kickContestent(UUID id){
        if(participants.containsKey(id)){
            participants.remove(id);
        }
    }


    public class ContestParticipation{
        private UUID id;
        private int pokemonIdx;

        public ContestParticipation(UUID id, int pokemonIdx){
            this.id = id;
            this.pokemonIdx = pokemonIdx;
        }
    }

    public String getCurrentContestInfo(){
        String result = "";
        if (isHost) {
            Player host = level == null ? null : level.getPlayerByUUID(hostId);
            result += "Host: " + (host == null ? hostId : host.getDisplayName().getString());
            result += ", Type: " + contestType;
        }else {
            result += "No current host";
        }
        return result;
    }

    public void runStubContest(){
        if (level != null && !level.isClientSide) {
            //System.out.println("Running contest");
        }
    }

    public void runStatAssesment(UUID id, int pokeIdx, int contestType, int contestLevel1, ServerPlayer player){
        if (player == null || !player.getUUID().equals(id) || pokeIdx < 0 || pokeIdx > 5
                || contestType < 0 || contestType > 4 || contestLevel1 < 0 || contestLevel1 > 4) {
            return;
        }
        //String contestOutput = "";
        Component componentOutput;
        Pokemon poke = Cobblemon.INSTANCE.getStorage().getParty(player).get(pokeIdx);
        if (poke == null) {
            player.displayClientMessage(Component.translatable("cobble_contests.error.invalid_pokemon"), false);
            return;
        }
        CompoundTag ribbonTag = poke.getPersistentData().getCompound("Ribbons");
        String pokeName = poke.getDisplayName(false).getString();
        int contestLevel = getNextContestLevel(ribbonTag, contestType);
        if(contestLevel < 5) {
            boolean result = runContest(poke, contestType, contestLevel);
            if (result) {
                componentOutput = Component.translatable("cobble_contests.contest_result.won_ranked", pokeName, getContestLevelString(contestLevel), getContestTypeString(contestType)).withStyle(ChatFormatting.LIGHT_PURPLE);
                //contestOutput = pokeName + " Won the " + getContestLevelString(contestLevel) + " " + getContestTypeString(contestType) + " Contest";
            } else {
                componentOutput = Component.translatable("cobble_contests.contest_result.lost_ranked", pokeName, getContestLevelString(contestLevel), getContestTypeString(contestType)).withStyle(ChatFormatting.LIGHT_PURPLE);
                //contestOutput = pokeName + " Lost the " + getContestLevelString(contestLevel) + " " + getContestTypeString(contestType) + " Contest";
            }
        }else{
            componentOutput = Component.translatable("cobble_contests.contest_result.maxed_ranked", pokeName, getContestTypeString(contestType)).withStyle(ChatFormatting.LIGHT_PURPLE);
            //contestOutput = pokeName + " has already beaten all " + getContestTypeString(contestType) + " Contests";
        }
        if (!player.level().isClientSide()) {
            player.displayClientMessage(componentOutput, false);
            //sPlayer.displayClientMessage(Component.literal(contestOutput).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }


        //Objects.requireNonNull().sendSystemMessage(Component.literal(contestOutput));


    }

    public String getContestTypeString(int contestType){
        return switch (contestType) {
            case 0 -> "Cool";
            case 1 -> "Beauty";
            case 2 -> "Cute";
            case 3 -> "Smart";
            case 4 -> "Tough";
            default -> "ERROR";
        };
    }

    public static String getContestTypeString1(int contestType){
        return switch (contestType) {
            case 0 -> "Cool";
            case 1 -> "Beauty";
            case 2 -> "Cute";
            case 3 -> "Smart";
            case 4 -> "Tough";
            default -> "ERROR";
        };
    }

    public String getContestLevelString(int contestLevel){
        return switch (contestLevel) {
            case 0 -> "Normal";
            case 1 -> "Super";
            case 2 -> "Hyper";
            case 3 -> "Ultra";
            case 4 -> "Master";
            default -> "ERROR";
        };
    }

    private int getNextContestLevel(CompoundTag ribbonTag, int contestType) {
        Ribbons ribbons = Ribbons.getFromTag(ribbonTag);
        return ribbons.getNextContestLevel(contestType);
    }

    private boolean runContest(Pokemon poke, int contestType, int contestLevel) {
        boolean result = false;
        CVs cvs = CVs.getFromTag(poke.getPersistentData().getCompound("CVs"));
        Ribbons ribbons = Ribbons.getFromTag(poke.getPersistentData().getCompound("Ribbons"));
        switch (contestType) {
            case 0:
                if(runAppContest(poke, contestLevel, cvs.getCool())) {
                    ribbons.setRankedCool(contestLevel, true);
                    result = true;
                }
                break;
            case 1:
                if(runAppContest(poke, contestLevel, cvs.getBeauty())) {
                    ribbons.setRankedBeauty(contestLevel, true);
                    result = true;
                }
                break;
            case 2:
                if(runAppContest(poke, contestLevel, cvs.getCute())) {
                    ribbons.setRankedCute(contestLevel, true);
                    result = true;
                }
                break;
            case 3:
                if(runAppContest(poke, contestLevel, cvs.getSmart())) {
                    ribbons.setRankedSmart(contestLevel, true);
                    result = true;
                }
                break;
            case 4:
                if(runAppContest(poke, contestLevel, cvs.getTough())) {
                    ribbons.setRankedTough(contestLevel, true);
                    result = true;
                }
                break;
            default:
                break;
        }
        Map<String, CompoundTag> myData = new HashMap<String, CompoundTag>() {};
        myData.put("Ribbons", ribbons.saveToNBT());
        saveRibbons(poke, myData);
        return result;
    }

    private void saveRibbons(final Pokemon pokemon, final Map<String, CompoundTag> myData) {
        final CompoundTag tag = pokemon.getPersistentData();
        myData.forEach((key, value) -> {
            tag.put(key, value);
        });
        // basically a vanilla "markAsDirty"
        /**if (pokemon.getChangeObservable() instanceof SimpleObservable<Pokemon>) { //TODO
            ((SimpleObservable<Pokemon>) pokemon.getChangeObservable()).emit(pokemon);
        }else {
            System.out.println("error, not simple observable (ContestBlockEntity)");
        }*/
    }


    private int[] thresholds = {5, 40, 100, 175, 245};
    private boolean runAppContest(Pokemon poke, int level, int typeVal){
        boolean result = false;
        if (level < 5 &&
                typeVal >= thresholds[level]){
            result = true;
        }
        return result;
    }

    private boolean runBeautyContest(Pokemon poke, int level) {
        boolean result = false;
        CVs cvs = CVs.getFromTag(poke.getPersistentData().getCompound("CVs"));
        if (cvs.getBeauty() >= thresholds[level]){
            result = true;
            Ribbons ribbons = Ribbons.getFromTag(poke.getPersistentData().getCompound("Ribbons"));
            ribbons.setRankedBeauty(level, true);
            Map<String, CompoundTag> myData = new HashMap<String, CompoundTag>() {};
            //CompoundTag dat = cvs.saveToNBT();
            myData.put("Ribbons", ribbons.saveToNBT());
            saveRibbons(poke, myData);

        }
        return result;
    }



}
