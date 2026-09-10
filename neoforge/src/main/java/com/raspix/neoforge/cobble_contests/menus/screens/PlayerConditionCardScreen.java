package com.raspix.neoforge.cobble_contests.menus.screens;

import com.cobblemon.mod.common.api.cooking.Flavour;
import com.cobblemon.mod.common.api.gui.ColourLibrary;
import com.cobblemon.mod.common.api.gui.MultiLineLabelK;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.trade.ModelWidget;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.raspix.common.cobble_contests.CobbleContests;
import com.raspix.neoforge.cobble_contests.CobbleContestsMoves;
import com.raspix.neoforge.cobble_contests.events.ContestMoves;
import com.raspix.neoforge.cobble_contests.menus.PlayerConditionCardMenu;
import com.raspix.neoforge.cobble_contests.menus.widgets.FixedImageButton;
import com.raspix.neoforge.cobble_contests.menus.widgets.WalletPokemonSlotButton;
import com.raspix.neoforge.cobble_contests.network.SBWalletScreenParty;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import com.raspix.neoforge.cobble_contests.pokemon.Ribbons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

import static com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt.drawProfilePokemon;
import static com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText;
import static com.cobblemon.mod.common.util.LocalizationUtilsKt.lang;
import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;

@OnlyIn(Dist.CLIENT)
public class PlayerConditionCardScreen extends AbstractContainerScreen<PlayerConditionCardMenu> {

    private static final int BASE_WIDTH = 349;
    private static final int BASE_HEIGHT = 205;
    private static final int MAX_STATS = 255;

    private static int PORTRAIT_DIAMETER = 25;
    private int pokemonIndex;
    private int pageIndex;
    private List<CVs> cvList;
    private List<Ribbons> ribbonList;

    private List<Button> buttons;

    private ModelWidget modelWidget;

    private ResourceLocation baseResource = cobblemonResource("textures/gui/summary/summary_base.png");
    private ResourceLocation portraitBackgroundResource = cobblemonResource("textures/gui/summary/portrait_background.png");
    private ResourceLocation typeSpacerResource = cobblemonResource("textures/gui/summary/type_spacer.png");
    private ResourceLocation typeSpacerDoubleResource = cobblemonResource("textures/gui/summary/type_spacer_double.png");
    private ResourceLocation sideSpacerResource = cobblemonResource("textures/gui/summary/summary_side_spacer.png");
    private ResourceLocation evolveButtonResource = cobblemonResource("textures/gui/summary/summary_evolve_button.png");
    ResourceLocation iconShinyResource = cobblemonResource("textures/gui/summary/icon_shiny.png");
    private ResourceLocation slotResource = cobblemonResource("textures/gui/summary/summary_party_slot.png");
    private ResourceLocation slotFaintedResource = cobblemonResource("textures/gui/summary/summary_party_slot_fainted.png");
    private ResourceLocation slotEmptyResource = cobblemonResource("textures/gui/summary/summary_party_slot_empty.png");
    ResourceLocation genderIconMale = cobblemonResource("textures/gui/party/party_gender_male.png");
    ResourceLocation genderIconFemale = cobblemonResource("textures/gui/party/party_gender_female.png");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleContests.MOD_ID, "textures/gui/contest_profile.png");
    private static final ResourceLocation MOVE_PANELS = ResourceLocation.fromNamespaceAndPath(CobbleContests.MOD_ID, "textures/gui/move_panels.png");
    private static final ResourceLocation RANK_RIBBONS = ResourceLocation.fromNamespaceAndPath(CobbleContests.MOD_ID, "textures/gui/badges.png");
    private Inventory playerInv;
    private PlayerPartyStore playerPartyStore;
    private ClientParty clientParty;

    private PlayerConditionCardMenu contestInfoMenu;


    public PlayerConditionCardScreen(PlayerConditionCardMenu containerID, Inventory playerInv, Component title) {
        super(containerID, playerInv, title);
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = 291;//362;
        this.imageHeight = 194;
        this.playerInv = playerInv;
        this.contestInfoMenu = containerID;
        //PacketHandler.sendToServer(new SBInfoScreenParty(playerInv.player.getUUID()));
        PacketDistributor.sendToServer(new SBWalletScreenParty(playerInv.player.getUUID()));
    }

    @Override
    protected void init() {
        super.init();
        buttons = new ArrayList<>();
        this.buttons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 12, this.topPos - 13, 17, 14, 292, 0, 16, TEXTURE, 1000, 750, btn -> {
            setPageIndex(0);
        })));
        this.buttons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 30, this.topPos - 13, 17, 14, 309, 0, 16, TEXTURE, 1000, 750, btn -> {
            setPageIndex(1);
        })));
        this.buttons.add(this.addRenderableWidget(new FixedImageButton(this.leftPos + 48, this.topPos -13, 17, 14, 326, 0, 16, TEXTURE, 1000, 750, btn -> {
            setPageIndex(2);
        })));
        pokemonIndex = 0;
        pageIndex = 0;

        renderParty();
        setSelectedModel();

    }

    private void setSelectedModel() {
        if(clientParty != null && clientParty.getSlots().size() > 0 && clientParty.get(pokemonIndex) != null){
            Pokemon poke = clientParty.get(pokemonIndex);
            modelWidget = new ModelWidget(
                    this.leftPos + 6 + 15,
                    this.topPos + 27 -5,
                    66,
                    66,
                    poke.asRenderablePokemon(),
                    2F,
                    325F,
                    -10.0
            );
        }else {
            modelWidget = null;
        }
    }

    private void renderParty(){
        int startingX = this.leftPos - 42;
        int xOffset = 0;
        int startingY = this.topPos + 10;
        int yOffset = 30;

        clientParty = CobblemonClient.INSTANCE.getStorage().getParty();
        List<Pokemon> partyPoke = this.clientParty.getSlots();//playerPartyStore.toGappyList();// contestInfoMenu.getPartyStore().toGappyList(); //
        for(int i = 0; i < 6; i++){
            Pokemon poke = partyPoke.get(i);
            int finalI = i;
            this.buttons.add(this.addRenderableWidget(new WalletPokemonSlotButton(startingX + (i * xOffset),
                    startingY + (i * yOffset),
                    46, 27, 0, 0, 27, getSlotTexture(poke), 46, 54, btn -> {
                    setPokemonPage(finalI);
                }, poke)));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int xMousePos, int yMousePos) {
        //renderBackground(guiGraphics);
        if(pageIndex == 1){ // ribbon page
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 194, this.imageWidth, this.imageHeight, 1000, 750);
        }else if(pageIndex == 0) { //stat page
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 1000, 750);
        }else{
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 388, this.imageWidth, this.imageHeight, 1000, 750);
        }

    }

    private ResourceLocation getSlotTexture(Pokemon pokemon){
        if (pokemon != null) {
            if (pokemon.isFainted()) return slotFaintedResource;
            return slotResource;
        }
        return slotEmptyResource;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int xMousePos, int yMousePos, float partialTick) {
        //this.renderBackground(guiGraphics);
        super.render(guiGraphics, xMousePos, yMousePos, partialTick);
        renderTooltip(guiGraphics, xMousePos, yMousePos);

        if(pageIndex == 0){ //stat page
            if(clientParty != null && clientParty.getSlots().size() > 0 && clientParty.get(pokemonIndex) != null && cvList != null){
                Pokemon poke = clientParty.get(pokemonIndex);
                assert poke != null;

                drawStatHexagon(new Vector3f(45f/255f, 237f/255f, 96f/255f), cvList.get(pokemonIndex), guiGraphics);
                writeFlavours(guiGraphics, poke);
                drawFriendshipHeart(guiGraphics, poke);
            }
        }else if (pageIndex == 1){// moves page
            if(clientParty != null && clientParty.getSlots().size() > 0 && clientParty.get(pokemonIndex) != null){
                Pokemon poke = clientParty.get(pokemonIndex);
                drawContestStats(guiGraphics, poke);
            }
        }else if(pageIndex == 2){ // ribbon page
            if(clientParty != null && clientParty.getSlots().size() > 0 && clientParty.get(pokemonIndex) != null){
                drawCoolContestRibbons(guiGraphics);
                drawBeautyContestRibbons(guiGraphics);
                drawCuteContestRibbons(guiGraphics);
                drawSmartContestRibbons(guiGraphics);
                drawToughContestRibbons(guiGraphics);
            }
        }
        if(modelWidget != null) {
            modelWidget.visible = true;
            modelWidget.render(guiGraphics, xMousePos, yMousePos, partialTick);
        }
        PoseStack poses = guiGraphics.pose();
    }

    private void drawFriendshipHeart(GuiGraphics guiGraphics, Pokemon poke) {
        int friendship = poke.getFriendship();
        int heartPixels = (int)((((float)friendship)/255f)*33f);

        guiGraphics.blit(TEXTURE, this.leftPos + 33, this.topPos + 95 + 33 - heartPixels, 293, 67 + (33 - heartPixels), 37, heartPixels + 1, 1000, 750);
        //guiGraphics.blit(TEXTURE, this.leftPos + 34, this.topPos + 96, 293, 67, 37, 34, 1000, 750);
    }

    public void drawCoolContestRibbons(GuiGraphics guiGraphics){
        int yPos = this.topPos + 97;
        int xPos = this.leftPos + 19;
        if(this.ribbonList != null && this.ribbonList.size() == 6){
            if(this.ribbonList.get(pokemonIndex).getCoolRanked(0)){
                guiGraphics.blit(RANK_RIBBONS, xPos, yPos, 0, 0, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCoolRanked(1)){
                guiGraphics.blit(RANK_RIBBONS, xPos +11, yPos, 16, 0, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCoolRanked(2)){
                guiGraphics.blit(RANK_RIBBONS, xPos +27, yPos, 32, 0, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCoolRanked(3)){
                guiGraphics.blit(RANK_RIBBONS, xPos +45, yPos, 48, 0, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCoolRanked(4)){
                guiGraphics.blit(RANK_RIBBONS, xPos +63, yPos, 64, 0, 16, 16, 80, 80);
            }

        }
    }

    public void drawBeautyContestRibbons(GuiGraphics guiGraphics){
        int yPos = this.topPos + 97 + 18;
        int xPos = this.leftPos + 19;
        if(this.ribbonList != null && this.ribbonList.size() == 6){
            if(this.ribbonList.get(pokemonIndex).getBeautyRanked(0)){
                guiGraphics.blit(RANK_RIBBONS, xPos, yPos, 0, 16, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getBeautyRanked(1)){
                guiGraphics.blit(RANK_RIBBONS, xPos +11, yPos, 16, 16, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getBeautyRanked(2)){
                guiGraphics.blit(RANK_RIBBONS, xPos +27, yPos, 32, 16, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getBeautyRanked(3)){
                guiGraphics.blit(RANK_RIBBONS, xPos +45, yPos, 48, 16, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getBeautyRanked(4)){
                guiGraphics.blit(RANK_RIBBONS, xPos +63, yPos, 64, 16, 16, 16, 80, 80);
            }

        }
    }

    public void drawCuteContestRibbons(GuiGraphics guiGraphics){
        int yPos = this.topPos + 97 + 36;
        int xPos = this.leftPos + 19;
        if(this.ribbonList != null && this.ribbonList.size() == 6){
            if(this.ribbonList.get(pokemonIndex).getCuteRanked(0)){
                guiGraphics.blit(RANK_RIBBONS, xPos, yPos, 0, 32, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCuteRanked(1)){
                guiGraphics.blit(RANK_RIBBONS, xPos +11, yPos, 16, 32, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCuteRanked(2)){
                guiGraphics.blit(RANK_RIBBONS, xPos +27, yPos, 32, 32, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCuteRanked(3)){
                guiGraphics.blit(RANK_RIBBONS, xPos +45, yPos, 48, 32, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getCuteRanked(4)){
                guiGraphics.blit(RANK_RIBBONS, xPos +63, yPos, 64, 32, 16, 16, 80, 80);
            }

        }
    }

    public void drawSmartContestRibbons(GuiGraphics guiGraphics){
        int yPos = this.topPos + 97 + 54;
        int xPos = this.leftPos + 19;
        if(this.ribbonList != null && this.ribbonList.size() == 6){
            if(this.ribbonList.get(pokemonIndex).getSmartRanked(0)){
                guiGraphics.blit(RANK_RIBBONS, xPos, yPos, 0, 48, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getSmartRanked(1)){
                guiGraphics.blit(RANK_RIBBONS, xPos +11, yPos, 16, 48, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getSmartRanked(2)){
                guiGraphics.blit(RANK_RIBBONS, xPos +27, yPos, 32, 48, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getSmartRanked(3)){
                guiGraphics.blit(RANK_RIBBONS, xPos +45, yPos, 48, 48, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getSmartRanked(4)){
                guiGraphics.blit(RANK_RIBBONS, xPos +63, yPos, 64, 48, 16, 16, 80, 80);
            }

        }
    }

    public void drawToughContestRibbons(GuiGraphics guiGraphics){
        int yPos = this.topPos + 97 + 72;
        int xPos = this.leftPos + 19;
        if(this.ribbonList != null && this.ribbonList.size() == 6){
            if(this.ribbonList.get(pokemonIndex).getToughRanked(0)){
                guiGraphics.blit(RANK_RIBBONS, xPos, yPos, 0, 64, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getToughRanked(1)){
                guiGraphics.blit(RANK_RIBBONS, xPos +11, yPos, 16, 64, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getToughRanked(2)){
                guiGraphics.blit(RANK_RIBBONS, xPos +27, yPos, 32, 64, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getToughRanked(3)){
                guiGraphics.blit(RANK_RIBBONS, xPos +45, yPos, 48, 64, 16, 16, 80, 80);
            }
            if(this.ribbonList.get(pokemonIndex).getToughRanked(4)){
                guiGraphics.blit(RANK_RIBBONS, xPos +63, yPos, 64, 64, 16, 16, 80, 80);
            }

        }
    }

    public void drawContestStats(GuiGraphics guiGraphics, Pokemon pokemon){
        List<Move> moves = pokemon.getMoveSet().getMoves();
        int xPos = getGuiLeft() + 95;
        int yPos = getGuiTop()  + 20;
        int xInc = 0;
        int yInc = 40;
        for(int i = 0; i < 4; i++){
            if(moves.size() > i && moves.get(i) != null){
                Move move = moves.get(i);
                drawMovePanel(guiGraphics, move, xPos, yPos + (i * yInc));
            }else {
                guiGraphics.blit(MOVE_PANELS, xPos, yPos + (i * yInc), 0, 0, 186, 32, 291, 400);
            }
        }
    }

    private void drawMovePanel(GuiGraphics guiGraphics, Move move, int xPos, int yPos){
        String moveName = move.getName();
        int panelOffset = 0;
        int appeal = 1;
        PoseStack poses = guiGraphics.pose();
        String description = "placeholder";
        Map<String, ContestMoves.MoveData> contestMoves = CobbleContestsMoves.INSTANCE.allMoves;//CobbleContestsForge.contestMoves;'
        if(contestMoves.containsKey(moveName)) {
            ContestMoves.MoveData data = contestMoves.get(moveName);
            String type = data.getType();
            switch (type) {
                case "Cool":
                    panelOffset = 32;
                    break;
                case "Beauty":
                    panelOffset = 64;
                    break;
                case "Cute":
                    panelOffset = 96;
                    break;
                case "Smart":
                    panelOffset = 128;
                    break;
                case "Tough":
                    panelOffset = 160;
                    break;
                default:
                    break;
            }
            appeal = data.getAppeal();
        }
        guiGraphics.blit(MOVE_PANELS, xPos, yPos, 0, panelOffset, 186, 32, 291, 400);
        drawScaledText(guiGraphics, null, lang("move." + moveName),
                (Number) (xPos + 37),
                (Number) (yPos + 5),
                1f, 1f, 2147483647, 0x00FFFFFF, true, true, null, null);
        //System.out.println(lang("move", moveName));
        guiGraphics.blit(MOVE_PANELS, xPos + 75 - (1 + appeal * 8), yPos + 21, 1, 193, 1 + appeal * 8, 9, 291, 400);

        poses.pushPose();
        poses.scale(0.5f, 0.5f, 1F);
        MultiLineLabelK.Companion.create(
                Component.translatable("cobble_contests.move_info." + description),
                100 / 0.5f,
                5
        ).renderLeftAligned(
                guiGraphics,
                (xPos + 80) / 0.5f,
                (yPos + 4) / 0.5f,
                0,
                11,
                ColourLibrary.WHITE,
                1f,
                true
        );
        poses.popPose();

        //display description
    }


    private void drawStatHexagon(Vector3f colour, CVs cvs, GuiGraphics guiGraphics) {
        int maximum = 50;
        int minimum = 1;

        int sheenStars = cvs.getSheenStars();

        float upperHexX = (float) (Math.sin(Math.toRadians(72.0)) * (float) maximum);
        float upperHexY = (float) (Math.cos(Math.toRadians(72.0)) * (float) maximum);
        float lowerHexX = (float) (Math.sin(Math.toRadians(36.0)) * (float) maximum);
        float lowerHexY = (float) (Math.cos(Math.toRadians(36.0)) * (float) maximum);

        float hexCenterX = this.leftPos + (this.imageWidth / 2.0F) + 47;
        float hexCenterY = this.topPos + (this.imageHeight / 2.0F) - 13;

        float coolRatio = Math.max(0.0F, Math.min(1.0F, (float)  cvs.getCool()/ (float)MAX_STATS))+ 0.05f;
        float beautyRatio = Math.max(0.0F, Math.min(1.0F, (float)  cvs.getBeauty()/ (float)MAX_STATS))+ 0.05f;
        float cuteRatio = Math.max(0.0F, Math.min(1.0F, (float)  cvs.getCute()/ (float)MAX_STATS)) + 0.05f;
        float smartRatio = Math.max(0.0F, Math.min(1.0F, (float)  cvs.getSmart()/ (float)MAX_STATS))+ 0.05f;
        float toughRatio = Math.max(0.0F, Math.min(1.0F, (float)  cvs.getTough()/ (float)MAX_STATS))+ 0.05f;

        Vector2f centerPoint = new Vector2f(hexCenterX, hexCenterY);
        Vector2f coolPoint = new Vector2f(hexCenterX, hexCenterY - (maximum * coolRatio));
        Vector2f beautyPoint = new Vector2f(hexCenterX + (upperHexX * beautyRatio), hexCenterY - (upperHexY * beautyRatio));
        Vector2f cutePoint = new Vector2f(hexCenterX + (lowerHexX * cuteRatio), hexCenterY + (lowerHexY * cuteRatio));
        Vector2f smartPoint = new Vector2f(hexCenterX - (lowerHexX * smartRatio), hexCenterY + (lowerHexY * smartRatio));
        Vector2f toughPoint = new Vector2f(hexCenterX - (upperHexX * toughRatio), hexCenterY - (upperHexY * toughRatio));

        drawTriangle(colour, coolPoint, centerPoint, beautyPoint); // upper right cool to beauty
        drawTriangle(colour, beautyPoint, centerPoint, cutePoint); // bottom right cute to beauty
        drawTriangle(colour, cutePoint, centerPoint, smartPoint); // bottom center cute to smart
        drawTriangle(colour, smartPoint, centerPoint, toughPoint); // bottom left smart to tough
        drawTriangle(colour, toughPoint, centerPoint, coolPoint);  //upper left tough to cool

        guiGraphics.blit(TEXTURE, this.leftPos + 163, this.topPos + 164, 292, 51, 1+ 8*sheenStars, 14, 1000, 750);
    }

    private void drawTriangle(Vector3f colour, Vector2f v1, Vector2f v2, Vector2f v3) {
        /**RenderSystem.setShaderTexture(0, CobblemonResources.INSTANCE.getWHITE());
        RenderSystem.setShaderColor(colour.x, colour.y, colour.z, 0.6F);
        RenderSystem.enableBlend();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(v1.x, v1.y, 10.0D).endVertex();//.color(0x2d, 0xed, 0x60, 0x99)
        bufferBuilder.vertex(v2.x, v2.y, 10.0D).endVertex();
        bufferBuilder.vertex(v3.x, v3.y, 10.0D).endVertex();

        bufferBuilder.nextElement();
        tessellator.end();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);*/

        RenderSystem.setShaderTexture(0, CobblemonResources.INSTANCE.getWHITE());
        RenderSystem.setShaderColor(colour.x, colour.y, colour.z, 0.6F);
        RenderSystem.enableBlend();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);//getBuilder();

        //bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        bufferBuilder.addVertex(v1.x, v1.y, 10.0F);//.color(0x2d, 0xed, 0x60, 0x99)
        bufferBuilder.addVertex(v2.x, v2.y, 10.0F);
        bufferBuilder.addVertex(v3.x, v3.y, 10.0F);

        //bufferBuilder.nextElement();
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        //tessellator.end();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void writeFlavours(GuiGraphics guiGraphics, Pokemon pokemon){
        Flavour fav = pokemon.getNature().getFavouriteFlavour();
        Flavour dis = pokemon.getNature().getDislikedFlavour();
        drawScaledText(guiGraphics, Component.literal("Favorite: " + ((fav != null)? fav.name(): "none")).getVisualOrderText(),
                (Number) (this.leftPos + 15),
                (Number) (this.topPos + 135),
                1f, 1f, 1f, 0x00FFFFFF, false, false);
        drawScaledText(guiGraphics, Component.literal("Disliked: " + ((dis != null)? dis.name(): "none")).getVisualOrderText(),
                (Number) (this.leftPos + 15),
                (Number) (this.topPos + 145),
                1f, 1f, 1f, 0x00FFFFFF, false, false);
    }

    @Override
    protected void renderLabels(GuiGraphics arg, int i, int j) {
    }

    private void setPokemonPage(int index){
        pokemonIndex = index;
        setSelectedModel();
    }

    private void setPageIndex(int index){
        pageIndex = index;
    }

    public void setParty(PlayerPartyStore pps){
        this.playerPartyStore = pps;
    }

    public void setCVs(CompoundTag tag) {
        cvList = new ArrayList<>();

        cvList.add(CVs.getFromTag(tag.getCompound("poke0")));
        cvList.add(CVs.getFromTag(tag.getCompound("poke1")));
        cvList.add(CVs.getFromTag(tag.getCompound("poke2")));
        cvList.add(CVs.getFromTag(tag.getCompound("poke3")));
        cvList.add(CVs.getFromTag(tag.getCompound("poke4")));
        cvList.add(CVs.getFromTag(tag.getCompound("poke5")));
        //System.out.println("Set up CVs");
    }

    public void setRibbons(CompoundTag tag) {
        ribbonList = new ArrayList<>();

        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke0ribbons")));
        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke1ribbons")));
        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke2ribbons")));
        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke3ribbons")));
        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke4ribbons")));
        ribbonList.add(Ribbons.getFromTag(tag.getCompound("poke5ribbons")));
    }
}
