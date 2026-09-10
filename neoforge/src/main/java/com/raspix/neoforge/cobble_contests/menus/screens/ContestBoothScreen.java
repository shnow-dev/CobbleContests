package com.raspix.neoforge.cobble_contests.menus.screens;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.raspix.common.cobble_contests.CobbleContests;
import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import com.raspix.neoforge.cobble_contests.menus.widgets.FixedImageButton;
import com.raspix.neoforge.cobble_contests.menus.widgets.PokemonContestBoothSlotButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText;

@OnlyIn(Dist.CLIENT)
public class ContestBoothScreen extends AbstractContainerScreen<ContestBoothMenu> {

    private final int STARTING_PAGE = 0; // The page everyone sees at the start
    private final int CONTEST_TYPE_SELECTION = 1; // The page the user sees if trying to host or run rank
    private final int CONTEST_WAITING_PAGE = 2; // The page all users see before the contest begins
    private final int POKEMON_SELECTION_PAGE = 3; // The page users trying to join a contest see when joining
    private final int CONTEST_LEVEL_SELECTION_PAGE = 4;
    private final int RESULTS_PAGE = 4;

    private int pageIndex;
    private int pokemonIndex;
    private int contestLevel;
    private int colorIndex;

    private int contestRunningType = -1; //0 is rank, 1 is host, 2 is participant

    private ClientParty clientParty;
    private List<Button> homeButtons;
    private List<Button> waitButtons;
    private List<Button> typeButtons;
    private List<Button> partyButtons;
    private Button confirmationButton;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleContests.MOD_ID, "textures/gui/contest_booth.png");
    private Inventory playerInv;
    private ContestBoothMenu contestInfoMenu;

    private UUID playerID;


    public ContestBoothScreen(ContestBoothMenu containerID, Inventory playerInv, Component title) {
        super(containerID, playerInv, title);
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = 288;//256;//291;//362;
        this.imageHeight = 224;//192;//194;
        this.playerInv = playerInv;
        this.contestInfoMenu = containerID;
        //PacketHandler.sendToServer(new SBInfoScreenParty(playerInv.player.getUUID()));
        //PacketDistributor.sendToServer(new SBWalletScreenParty(playerInv.player.getUUID()));
    }

    @Override
    protected void init() {
        super.init();
        pokemonIndex = 0;
        pageIndex = 0;
        colorIndex = -1;
        clientParty = CobblemonClient.INSTANCE.getStorage().getParty();
        playerID = playerInv.player.getUUID();

        createHomeButtons();
        createWaitingButtons();
        createTypeButtons();
        createPartyButtons();
        this.confirmationButton = this.addRenderableWidget(new FixedImageButton(this.leftPos + 80, this.topPos + 40, 64, 18, 289, 43, 18, TEXTURE, 1000, 750, btn -> {
            setContestLevel();
        }));

        setPageIndex(0);
    }

    private void createHomeButtons(){
        homeButtons = new ArrayList<>();
        this.homeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 110, this.topPos + 40, 64, 18, 289, 43, 18, TEXTURE, 1000, 750, btn -> {
            this.contestRunningType = 0;
            setPageIndex(CONTEST_TYPE_SELECTION);
        })));
        /**this.homeButtons.add(this.addRenderableWidget(new ImageButton(this.leftPos + 60, this.topPos + 80, 64, 18, 289, 43, 18, TEXTURE, 1000, 750, btn -> {
            setPageIndex(CONTEST_TYPE_SELECTION);
        })));
        this.homeButtons.add(this.addRenderableWidget(new ImageButton(this.leftPos + 130, this.topPos + 80, 64, 18, 353, 43, 18, TEXTURE, 1000, 750, btn -> {
            setPageIndex(POKEMON_SELECTION_PAGE);
        })));*/
    }

    private void createWaitingButtons(){
        waitButtons = new ArrayList<>();
        this.waitButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 110, this.topPos + 140, 64, 18, 289, 43, 18, TEXTURE, 1000, 750, btn -> {
            startContest();
            //start contest
            //setPageIndex(CONTEST_TYPE_SELECTION);
        })));
    }

    private void createTypeButtons(){
        typeButtons = new ArrayList<>();
        this.typeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 134, this.topPos + 49, 20, 20, 288, 0, 21, TEXTURE, 1000, 750, btn -> {
            setContestType(0);
        })));
        this.typeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 210, this.topPos + 78, 20, 20, 308, 0, 21, TEXTURE, 1000, 750, btn -> {
            setContestType(1);
        })));
        this.typeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 173, this.topPos + 155, 20, 20, 328, 0, 21, TEXTURE, 1000, 750, btn -> {
            setContestType(2);
        })));
        this.typeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 95, this.topPos + 155, 20, 20, 348, 0, 21, TEXTURE, 1000, 750, btn -> {
            setContestType(3);
        })));
        this.typeButtons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 58, this.topPos + 78, 20, 20, 368, 0, 21, TEXTURE, 1000, 750, btn -> {
            setContestType(4);
        })));
    }

    private void createPartyButtons(){
        partyButtons = new ArrayList<>();

        for(int i = 0; i < clientParty.getSlots().size(); i++){
            if(clientParty.get(i) != null) {
                int buttonX = this.leftPos + 39 + (73 * (i % 3));
                int buttonY = this.topPos + 36 + (81 * (i / 3));
                int finalI = i;
                this.partyButtons.add(this.addRenderableWidget(new PokemonContestBoothSlotButton(buttonX, buttonY, 64, 70, 418, 1, 72, TEXTURE, 1000, 750, btn -> {
                    //joinContestWithPokemon(finalI);
                    selectContestPokemon(finalI);
                }, clientParty.get(i))));
            }
        }
    }

    private void selectContestPokemon(int pokeIndex) {
        this.pokemonIndex = pokeIndex;
        setPageIndex(CONTEST_WAITING_PAGE);//CONTEST_LEVEL_SELECTION_PAGE);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int xMousePos, int yMousePos) {
        if(pageIndex == CONTEST_TYPE_SELECTION){
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 1000, 750);
        }else if(pageIndex == POKEMON_SELECTION_PAGE){
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 225, this.imageWidth, this.imageHeight, 1000, 750);
        }else{
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 450, this.imageWidth, this.imageHeight, 1000, 750);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int xMousePos, int yMousePos, float partialTick) { //
        super.render(guiGraphics, xMousePos, yMousePos, partialTick);
        if(pageIndex == CONTEST_WAITING_PAGE){
            /**drawScaledText(guiGraphics, Component.translatable(getContestResult()).getVisualOrderText(),
                    (Number) (this.leftPos + 50),
                    (Number) (this.topPos + 50),
                    0.5f, 0.5f, 1f, 0x00000000, true, false);*/
            drawScaledText(guiGraphics, Component.literal("Start Contest").getVisualOrderText(),
                    (Number) (this.leftPos + 144),
                    (Number) (this.topPos + 145),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.literal("Pokemon: " + clientParty.get(pokemonIndex).getDisplayName(false).getString()).getVisualOrderText(),
                    (Number) (this.leftPos + 40),
                    (Number) (this.topPos + 50),
                    1f, 1f, 1f, 0x00918b99, false, false);
            drawScaledText(guiGraphics, Component.literal("Contest Type: " + ContestBlockEntity.getContestTypeString1(colorIndex)).getVisualOrderText(),
                    (Number) (this.leftPos + 40),
                    (Number) (this.topPos + 70),
                    1f, 1f, 1f, 0x00918b99, false, false);
        }
        if(pageIndex == STARTING_PAGE){
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_text.start").getVisualOrderText(),
                    (Number) (this.leftPos + 143),
                    (Number) (this.topPos + 44),
                    1.5f, 1.5f, 1f, 0x00918b99, true, false);
        }
        if(pageIndex == CONTEST_TYPE_SELECTION){
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_text.select_type").getVisualOrderText(),
                    (Number) (this.leftPos + 145),
                    (Number) (this.topPos + 104),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_type.cool").getVisualOrderText(),
                    (Number) (this.leftPos + 143),
                    (Number) (this.topPos + 78),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_type.beauty").getVisualOrderText(),
                    (Number) (this.leftPos + 220),
                    (Number) (this.topPos + 106),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_type.cute").getVisualOrderText(),
                    (Number) (this.leftPos + 184),
                    (Number) (this.topPos + 184),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_type.smart").getVisualOrderText(),
                    (Number) (this.leftPos + 104),
                    (Number) (this.topPos + 184),
                    1f, 1f, 1f, 0x00918b99, true, false);
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_type.tough").getVisualOrderText(),
                    (Number) (this.leftPos + 68),
                    (Number) (this.topPos + 106),
                    1f, 1f, 1f, 0x00918b99, true, false);
        }
        if(pageIndex == POKEMON_SELECTION_PAGE){
            drawScaledText(guiGraphics, Component.translatable("cobble_contests.contest_text.pokemon_select").getVisualOrderText(),
                    (Number) (this.leftPos + 140),
                    (Number) (this.topPos + 10),
                    1f, 1f, 1f, 0x00000000, true, false);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics arg, int i, int j) {
    }

    private void joinContestWithPokemon(int index){
        pokemonIndex = index;
        setPageIndex(CONTEST_WAITING_PAGE);

        //should have packet here
    }

    private void setPageIndex(int index){
        pageIndex = index;
        for (Button homeButton : homeButtons) {
            homeButton.visible = index == STARTING_PAGE;
        }
        for (Button waitButton : waitButtons) {
            waitButton.visible = index == CONTEST_WAITING_PAGE;
        }
        for (Button typeButton : typeButtons) {
            typeButton.visible = index == CONTEST_TYPE_SELECTION;
        }
        for (Button partyButton : partyButtons) {
            partyButton.visible = index == POKEMON_SELECTION_PAGE;
        }

        confirmationButton.visible = index == CONTEST_LEVEL_SELECTION_PAGE;
    }

    private void setContestType(int type){
        colorIndex = type;
        setPageIndex(POKEMON_SELECTION_PAGE);
        /**if(menu.hostSelectType(playerID, type)){
            setPageIndex(CONTEST_WAITING_PAGE);
        }else {
            setPageIndex(STARTING_PAGE);
        }*/
        //should have packet type
    }

    private void setContestLevel(){
        //this.contestLevel = this;
        if(menu.hostSelectType(playerID, colorIndex)){
            setPageIndex(CONTEST_WAITING_PAGE);
        }else {
            setPageIndex(STARTING_PAGE);
        }
        //should have packet type
    }

    private void startContest(){
        menu.startContest(colorIndex, pokemonIndex, playerInv.player.getUUID());
        menu.startStatAssesment(playerInv.player.getUUID(), pokemonIndex, colorIndex);
        setPageIndex(STARTING_PAGE);
        // should have packet
    }

    private String getContestResult(){
        return menu.getContestResults();
        //should have packet
    }

}
