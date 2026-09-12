package com.raspix.neoforge.cobble_contests.menus.screens;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.trade.ModelWidget;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raspix.common.cobble_contests.CobbleContests;
import com.raspix.common.cobble_contests.contest.ContestPhase;
import com.raspix.common.cobble_contests.contest.PresentationBubbleChallenge;
import com.raspix.neoforge.cobble_contests.client.sound.LoopingContestMusic;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import com.raspix.neoforge.cobble_contests.network.CBContestState;
import com.raspix.neoforge.cobble_contests.network.SBWalletScreenParty;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import com.raspix.neoforge.cobble_contests.pokemon.Ribbons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt.drawProfilePokemon;

/** Full-screen 0.3 presentation prototype backed by server-owned contest data. */
@OnlyIn(Dist.CLIENT)
public final class ContestBoothScreen extends AbstractContainerScreen<ContestBoothMenu> {
    private static final int SOURCE_WIDTH = 1672;
    private static final int SOURCE_HEIGHT = 941;
    private static final int BUBBLE_SOURCE_SIZE = 1254;
    private static final int CUSTOM_ICON_SOURCE_SIZE = 256;
    private static final int WELCOME_COUNTER_SOURCE_Y = 590;
    private static final long CURTAIN_DURATION_NANOS = 700_000_000L;
    private static final long COUNTDOWN_STEP_NANOS = 900_000_000L;
    private static final long COUNTDOWN_DURATION_NANOS = COUNTDOWN_STEP_NANOS * 4L;
    private static final long TRANSITION_FADE_NANOS = 350_000_000L;
    private static final long POP_DURATION_NANOS = 320_000_000L;
    private static final long GAUGE_ANIMATION_NANOS = 300_000_000L;

    private static final ResourceLocation WELCOME = texture("welcome.png");
    private static final ResourceLocation POKEMON_SELECTION = texture("pokemon_selection.png");
    private static final ResourceLocation ARENA = texture("arena.png");
    private static final float[] CURTAIN_OPENNESS = {
            0.0F, 0.15F, 0.30F, 0.45F, 0.60F, 0.75F, 0.90F, 1.0F
    };
    private static final ResourceLocation[] CURTAIN_FRAMES = {
            texture("curtain_00.png"),
            texture("curtain_15.png"),
            texture("curtain_30.png"),
            texture("curtain_45.png"),
            texture("curtain_60.png"),
            texture("curtain_75.png"),
            texture("curtain_90.png"),
            texture("curtain_100.png")
    };
    private static final ResourceLocation[] POP_FRAMES = {
            texture("bubble_pop_1.png"),
            texture("bubble_pop_2.png"),
            texture("bubble_pop_3.png"),
            texture("bubble_pop_4.png")
    };
    private static final ResourceLocation POSITIVE_BUBBLE = texture("bubble_positive.png");
    private static final ResourceLocation NEGATIVE_BUBBLE = texture("bubble_negative.png");
    private static final ResourceLocation BORED_BUBBLE = texture("bubble_bored.png");
    private static final ResourceLocation HOST_SKIN = texture("contest_host.png");
    private static final SoundEvent MENU_MUSIC = sound("music.menu");
    private static final SoundEvent CHALLENGE_MUSIC = sound("music.challenge");
    private static final ResourceLocation[] CATEGORY_ICONS = {
            texture("category_cool.png"),
            texture("category_beauty.png"),
            texture("category_grace.png"),
            texture("category_smart.png"),
            texture("category_tough.png")
    };

    private final UUID playerId;
    private final List<CVs> contestStats = new ArrayList<>(6);
    private final List<Ribbons> earnedRibbons = new ArrayList<>(6);
    private final List<PopEffect> popEffects = new ArrayList<>();

    private ClientParty clientParty;
    private Page page = Page.WELCOME;
    private int selectedCategory = -1;
    private int selectedRank = -1;
    private int selectedPokemon = -1;
    private CBContestState contestState;
    private ModelWidget selectionModel;
    private ModelWidget arenaModel;
    private ModelWidget welcomeModel;
    private LoopingContestMusic menuMusic;
    private LoopingContestMusic challengeMusic;
    private long transitionStartedAtNanos;
    private long resultsShownAtNanos;
    private long stateReceivedAtNanos;
    private long bubbleShownAtNanos;
    private boolean requestedPartyData;
    private boolean contestDataLoaded;
    private boolean awaitingServer;
    private boolean awaitingBubbleAck;
    private int gaugeAnimationFrom;
    private int gaugeAnimationTo;
    private long gaugeAnimationStartedAtNanos;
    private int highestGaugeMilestone;
    private int lastCountdownCue = Integer.MIN_VALUE;
    private boolean transitionFromSelection;

    public ContestBoothScreen(ContestBoothMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerId = playerInventory.player.getUUID();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CobbleContests.MOD_ID, "textures/gui/contest_v03/" + name
        );
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(CobbleContests.MOD_ID, name)
        );
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        leftPos = 0;
        topPos = 0;
        if (clientParty == null) {
            clientParty = CobblemonClient.INSTANCE.getStorage().getParty();
        }
        if (!requestedPartyData) {
            requestedPartyData = true;
            PacketDistributor.sendToServer(new SBWalletScreenParty(playerId));
        }
        rebuildWidgets();
        rebuildPokemonModels();
        startMusicLoops();
        switchToMenuMusic();
    }

    public void setContestData(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        contestStats.clear();
        earnedRibbons.clear();
        for (int index = 0; index < 6; index++) {
            contestStats.add(CVs.getFromTag(tag.getCompound("poke" + index)));
            earnedRibbons.add(Ribbons.getFromTag(tag.getCompound("poke" + index + "ribbons")));
        }
        contestDataLoaded = true;
        if (page == Page.POKEMON_SELECTION) {
            rebuildWidgets();
        }
    }

    public void applyContestState(CBContestState state) {
        if (contestState != null && contestState.sessionId().equals(state.sessionId())
                && state.stateVersion() < contestState.stateVersion()) {
            return;
        }
        boolean sameSession = contestState != null
                && contestState.sessionId().equals(state.sessionId());
        int previousWave = contestState == null ? -1 : contestState.progress();
        int previousPhase = contestState == null ? -1 : contestState.phase();
        int previousGauge = contestState == null ? 0 : contestState.resolvedTargets();
        contestState = state;
        stateReceivedAtNanos = System.nanoTime();
        awaitingServer = false;
        awaitingBubbleAck = false;
        if (!sameSession) {
            highestGaugeMilestone = 0;
        }

        int gauge = state.resolvedTargets();
        if (gauge != previousGauge) {
            gaugeAnimationFrom = previousGauge;
            gaugeAnimationTo = gauge;
            gaugeAnimationStartedAtNanos = stateReceivedAtNanos;
        }
        int milestone = milestoneForGauge(gauge);
        if (milestone > highestGaugeMilestone) {
            highestGaugeMilestone = milestone;
            playSelectedPokemonCry();
        }

        if (state.finished()) {
            if (page != Page.CURTAIN_CLOSING_TO_RESULTS && page != Page.RESULTS) {
                page = Page.CURTAIN_CLOSING_TO_RESULTS;
                transitionStartedAtNanos = System.nanoTime();
                bubbleShownAtNanos = 0L;
                rebuildWidgets();
            }
            return;
        }
        if (!sameSession) {
            beginChallengeTransition(true);
            return;
        }
        if (previousPhase != state.phase() && page == Page.PRESENTATION) {
            beginChallengeTransition(false);
            return;
        }
        if (page == Page.PRESENTATION) {
            if (!sameSession || previousWave != state.progress() || bubbleShownAtNanos == 0L) {
                bubbleShownAtNanos = stateReceivedAtNanos;
            }
        }
    }

    private void beginChallengeTransition(boolean fromSelection) {
        transitionFromSelection = fromSelection;
        page = Page.CURTAIN_CLOSING_TO_CHALLENGE;
        transitionStartedAtNanos = System.nanoTime();
        bubbleShownAtNanos = 0L;
        lastCountdownCue = Integer.MIN_VALUE;
        rebuildPokemonModels();
        switchToChallengeMusic();
        rebuildWidgets();
    }

    private static int milestoneForGauge(int gauge) {
        if (gauge >= 15) {
            return 15;
        }
        if (gauge >= 10) {
            return 10;
        }
        return gauge >= 5 ? 5 : 0;
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        switch (page) {
            case WELCOME -> createWelcomeWidgets();
            case POKEMON_SELECTION -> createPokemonSelectionWidgets();
            case RESULTS -> addRenderableWidget(Button.builder(
                    Component.translatable("cobble_contests.action.return_to_menu"),
                    button -> returnToWelcome()
            ).bounds(px(0.43F), py(0.82F), px(0.14F), Math.max(20, py(0.055F))).build());
            case CURTAIN_CLOSING_TO_CHALLENGE, COUNTDOWN, CURTAIN_OPENING,
                    PRESENTATION, CURTAIN_CLOSING_TO_RESULTS -> {
            }
        }
    }

    private void createWelcomeWidgets() {
        float[][] centers = {
                {0.099F, 0.259F}, {0.233F, 0.171F}, {0.374F, 0.128F},
                {0.515F, 0.171F}, {0.654F, 0.259F}
        };
        int size = Math.max(56, Math.min(px(0.110F), py(0.195F)));
        for (int category = 0; category < centers.length; category++) {
            int selected = category;
            addRenderableWidget(new CategoryButton(
                    px(centers[category][0]) - size / 2,
                    py(centers[category][1]) - size / 2,
                    size, selected,
                    Component.translatable(contestTypeKey(selected)),
                    () -> selectCategory(selected)
            ));
        }

        int rankX = px(0.770F);
        int rankWidth = px(0.165F);
        int rankHeight = Math.max(16, py(0.040F));
        for (int rank = 0; rank < 5; rank++) {
            int selected = rank;
            Component label = selectedRank == rank
                    ? Component.literal("▶ ").append(Component.translatable(rankKey(rank)))
                    : Component.translatable(rankKey(rank));
            Button rankButton = addRenderableWidget(Button.builder(
                    label, button -> selectRank(selected)
            ).bounds(rankX, py(0.220F) + rank * (rankHeight + 3), rankWidth, rankHeight).build());
            rankButton.active = selectedCategory >= 0;
        }

        Button startButton = addRenderableWidget(Button.builder(
                Component.translatable("cobble_contests.action.start"), button -> {
                    page = Page.POKEMON_SELECTION;
                    selectedPokemon = -1;
                    rebuildWidgets();
                    rebuildPokemonModels();
                }
        ).bounds(rankX, py(0.625F), rankWidth, rankHeight).build());
        startButton.active = selectedCategory >= 0 && selectedRank >= 0;
    }

    private void createPokemonSelectionWidgets() {
        List<Pokemon> slots = clientParty == null ? List.of() : clientParty.getSlots();
        for (int index = 0; index < 6; index++) {
            int column = index % 2;
            int row = index / 2;
            Pokemon pokemon = index < slots.size() ? slots.get(index) : null;
            int slot = index;
            PartySlotButton button = new PartySlotButton(
                    px(0.098F + column * 0.220F),
                    py(0.118F + row * 0.225F),
                    px(0.202F), py(0.195F), slot, pokemon,
                    () -> selectPokemon(slot)
            );
            button.active = pokemon != null && !pokemon.isFainted();
            addRenderableWidget(button);
        }

        int footerY = py(0.882F);
        int footerHeight = Math.max(16, py(0.036F));
        addRenderableWidget(Button.builder(
                Component.translatable("cobble_contests.action.back"), button -> {
                    page = Page.WELCOME;
                    selectedPokemon = -1;
                    rebuildWidgets();
                    rebuildPokemonModels();
                }
        ).bounds(px(0.086F), footerY, px(0.105F), footerHeight).build());

        Button start = addRenderableWidget(Button.builder(
                Component.translatable("cobble_contests.action.start"), button -> requestContestStart()
        ).bounds(px(0.738F), footerY, px(0.145F), footerHeight).build());
        start.active = canSelectedPokemonEnterRank() && !awaitingServer;
    }

    private void selectCategory(int category) {
        selectedCategory = category;
        selectedRank = -1;
        rebuildWidgets();
    }

    private void selectRank(int rank) {
        selectedRank = rank;
        rebuildWidgets();
    }

    private void selectPokemon(int slot) {
        List<Pokemon> slots = clientParty == null ? List.of() : clientParty.getSlots();
        if (slot < 0 || slot >= slots.size()) {
            return;
        }
        Pokemon pokemon = slots.get(slot);
        if (pokemon == null || pokemon.isFainted()) {
            return;
        }
        selectedPokemon = slot;
        rebuildPokemonModels();
        rebuildWidgets();
    }

    private boolean canSelectedPokemonEnterRank() {
        if (!contestDataLoaded || selectedPokemon < 0 || selectedCategory < 0 || selectedRank < 0
                || selectedPokemon >= earnedRibbons.size()) {
            return false;
        }
        int nextRank = earnedRibbons.get(selectedPokemon).getNextContestLevel(selectedCategory);
        return selectedRank <= Math.min(4, nextRank);
    }

    private void requestContestStart() {
        if (!canSelectedPokemonEnterRank() || awaitingServer) {
            return;
        }
        awaitingServer = true;
        highestGaugeMilestone = 0;
        switchToChallengeMusic();
        menu.startStatAssesment(playerId, selectedPokemon, selectedCategory, selectedRank);
        rebuildWidgets();
    }

    private void returnToWelcome() {
        page = Page.WELCOME;
        selectedCategory = -1;
        selectedRank = -1;
        selectedPokemon = -1;
        contestState = null;
        awaitingServer = false;
        awaitingBubbleAck = false;
        popEffects.clear();
        bubbleShownAtNanos = 0L;
        highestGaugeMilestone = 0;
        switchToMenuMusic();
        rebuildPokemonModels();
        rebuildWidgets();
    }

    private void rebuildPokemonModels() {
        welcomeModel = null;
        selectionModel = null;
        arenaModel = null;
        Pokemon decorativePokemon = firstPartyPokemon();
        if (decorativePokemon != null) {
            welcomeModel = new ModelWidget(
                    px(0.792F), py(0.655F), px(0.115F), py(0.125F),
                    decorativePokemon.asRenderablePokemon(), 1.10F, 30F, -42.0
            );
        }
        Pokemon pokemon = selectedPokemon();
        if (pokemon == null) {
            return;
        }
        selectionModel = new ModelWidget(
                px(0.620F), py(0.505F), px(0.260F), py(0.260F),
                pokemon.asRenderablePokemon(), 1.55F, 30F, -42.0
        );
        arenaModel = new ModelWidget(
                px(0.365F), py(0.395F), px(0.270F), py(0.270F),
                pokemon.asRenderablePokemon(), 1.75F, 30F, -42.0
        );
    }

    private Pokemon firstPartyPokemon() {
        if (clientParty == null) {
            return null;
        }
        for (Pokemon pokemon : clientParty.getSlots()) {
            if (pokemon != null) {
                return pokemon;
            }
        }
        return null;
    }

    private Pokemon selectedPokemon() {
        List<Pokemon> slots = clientParty == null ? List.of() : clientParty.getSlots();
        if (selectedPokemon < 0 || selectedPokemon >= slots.size()) {
            return null;
        }
        return slots.get(selectedPokemon);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long now = System.nanoTime();
        if (minecraft != null) {
            minecraft.getMusicManager().stopPlaying();
        }
        popEffects.removeIf(effect -> now - effect.startedAtNanos > POP_DURATION_NANOS);
        switch (page) {
            case CURTAIN_CLOSING_TO_CHALLENGE -> {
                if (now - transitionStartedAtNanos >= CURTAIN_DURATION_NANOS) {
                    page = Page.COUNTDOWN;
                    transitionStartedAtNanos = now;
                    lastCountdownCue = Integer.MIN_VALUE;
                    updateCountdownSound(now);
                }
            }
            case COUNTDOWN -> {
                updateCountdownSound(now);
                if (now - transitionStartedAtNanos >= COUNTDOWN_DURATION_NANOS) {
                    page = Page.CURTAIN_OPENING;
                    transitionStartedAtNanos = now;
                }
            }
            case CURTAIN_OPENING -> {
                if (now - transitionStartedAtNanos >= CURTAIN_DURATION_NANOS) {
                    page = Page.PRESENTATION;
                    bubbleShownAtNanos = now;
                    playSelectedPokemonCry();
                    rebuildWidgets();
                }
            }
            case CURTAIN_CLOSING_TO_RESULTS -> {
                if (now - transitionStartedAtNanos >= CURTAIN_DURATION_NANOS) {
                    page = Page.RESULTS;
                    resultsShownAtNanos = now;
                    rebuildWidgets();
                }
            }
            default -> {
            }
        }
    }

    private void updateCountdownSound(long now) {
        int cue = countdownCue(now);
        if (cue == lastCountdownCue) {
            return;
        }
        lastCountdownCue = cue;
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        float pitch = switch (cue) {
            case 3 -> 0.80F;
            case 2 -> 0.95F;
            case 1 -> 1.10F;
            default -> 1.35F;
        };
        minecraft.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), cue == 0 ? 1.0F : 0.78F, pitch);
    }

    private int countdownCue(long now) {
        long elapsed = Math.max(0L, now - transitionStartedAtNanos);
        int step = Math.min(3, (int) (elapsed / COUNTDOWN_STEP_NANOS));
        return step == 3 ? 0 : 3 - step;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (page) {
            case WELCOME -> renderWelcomeBackground(graphics, mouseX, mouseY, partialTick);
            case POKEMON_SELECTION -> renderPokemonSelectionBackground(graphics, mouseX, mouseY, partialTick);
            case CURTAIN_CLOSING_TO_CHALLENGE -> {
                if (transitionFromSelection) {
                    renderPokemonSelectionBackground(graphics, mouseX, mouseY, partialTick);
                } else {
                    renderArena(graphics, mouseX, mouseY, partialTick, false);
                }
                float progress = curtainProgress();
                renderCurtain(graphics, 1.0F - progress);
                renderFullscreenFade(graphics, ease(progress) * 0.55F);
            }
            case COUNTDOWN -> {
                renderArena(graphics, mouseX, mouseY, partialTick, false);
                renderCurtain(graphics, 0.0F);
                renderCountdown(graphics, countdownCue(System.nanoTime()), 1.0F);
                float fade = 1.0F - Math.min(1.0F,
                        (System.nanoTime() - transitionStartedAtNanos) / (float) TRANSITION_FADE_NANOS);
                renderFullscreenFade(graphics, fade * 0.55F);
            }
            case CURTAIN_OPENING -> {
                renderArena(graphics, mouseX, mouseY, partialTick, false);
                renderCurtain(graphics, curtainProgress());
                float goFade = 1.0F - Math.min(1.0F,
                        (System.nanoTime() - transitionStartedAtNanos) / (float) TRANSITION_FADE_NANOS);
                renderCountdown(graphics, 0, goFade);
            }
            case PRESENTATION -> renderArena(graphics, mouseX, mouseY, partialTick, true);
            case CURTAIN_CLOSING_TO_RESULTS -> {
                renderArena(graphics, mouseX, mouseY, partialTick, false);
                float progress = curtainProgress();
                renderCurtain(graphics, 1.0F - progress);
                renderFullscreenFade(graphics, ease(progress) * 0.70F);
            }
            case RESULTS -> {
                renderResultsBackground(graphics);
                float fade = 1.0F - Math.min(1.0F,
                        (System.nanoTime() - resultsShownAtNanos) / (float) TRANSITION_FADE_NANOS);
                renderFullscreenFade(graphics, fade * 0.70F);
            }
        }
    }

    private void renderWelcomeBackground(GuiGraphics graphics, int mouseX, int mouseY,
                                         float partialTick) {
        blitFullscreen(graphics, WELCOME);
        renderHost(graphics);
        int cutScreenY = Math.round(height * (WELCOME_COUNTER_SOURCE_Y / (float) SOURCE_HEIGHT));
        graphics.blit(WELCOME, 0, cutScreenY, width, height - cutScreenY,
                0.0F, WELCOME_COUNTER_SOURCE_Y, SOURCE_WIDTH,
                SOURCE_HEIGHT - WELCOME_COUNTER_SOURCE_Y,
                SOURCE_WIDTH, SOURCE_HEIGHT);
        if (welcomeModel != null) {
            graphics.enableScissor(px(0.745F), py(0.640F), px(0.950F), py(0.805F));
            welcomeModel.visible = true;
            welcomeModel.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }
    }

    private void renderPokemonSelectionBackground(GuiGraphics graphics, int mouseX, int mouseY,
                                                   float partialTick) {
        blitFullscreen(graphics, POKEMON_SELECTION);
        if (selectionModel != null) {
            graphics.enableScissor(px(0.585F), py(0.445F), px(0.915F), py(0.810F));
            selectionModel.visible = true;
            selectionModel.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }
        if (selectedPokemon >= 0 && selectedPokemon < contestStats.size()) {
            drawStatsDiamond(graphics, contestStats.get(selectedPokemon));
        }
    }

    private void renderArena(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                             boolean showChallenge) {
        blitFullscreen(graphics, ARENA);
        if (arenaModel != null) {
            graphics.enableScissor(px(0.285F), py(0.275F), px(0.715F), py(0.720F));
            arenaModel.visible = true;
            arenaModel.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }
        if (showChallenge && contestState != null
                && contestState.phase() == ContestPhase.PRESENTATION.ordinal()) {
            renderPresentationChallenge(graphics);
        }
        renderPopEffects(graphics);
    }

    private void renderResultsBackground(GuiGraphics graphics) {
        blitFullscreen(graphics, ARENA);
        blitFullscreen(graphics, CURTAIN_FRAMES[0]);
        graphics.fill(px(0.285F), py(0.245F), px(0.715F), py(0.760F), 0xDD15152E);
        graphics.drawCenteredString(font,
                Component.translatable("cobble_contests.contest_result.presentation_preview"),
                width / 2, py(0.330F), 0xFFFFD96A);
        int presentationScore = contestState == null ? 0
                : contestState.scores()[ContestPhase.PRESENTATION.ordinal()];
        graphics.drawCenteredString(font, Component.translatable(
                        "cobble_contests.contest_result.presentation_score", presentationScore),
                width / 2, py(0.445F), 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("cobble_contests.contest_result.no_reward_preview"),
                width / 2, py(0.540F), 0xFF9EEBFF);
    }

    private void renderCountdown(GuiGraphics graphics, int cue, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        String text = cue == 0 ? "GO !" : Integer.toString(cue);
        int centerX = width / 2;
        int centerY = py(0.455F);
        int boxWidth = Math.max(px(0.13F), font.width(text) * 5);
        int boxHeight = Math.max(py(0.12F), font.lineHeight * 6);
        int colorAlpha = Math.min(255, Math.round(alpha * 225.0F));
        graphics.fill(centerX - boxWidth / 2, centerY - boxHeight / 2,
                centerX + boxWidth / 2, centerY + boxHeight / 2,
                colorAlpha << 24 | 0x00151B44);
        graphics.renderOutline(centerX - boxWidth / 2, centerY - boxHeight / 2,
                boxWidth, boxHeight, colorAlpha << 24 | 0x00FFD966);

        PoseStack poses = graphics.pose();
        poses.pushPose();
        poses.translate(centerX, centerY, 250.0F);
        float scale = Math.max(3.0F, Math.min(width, height) / 210.0F);
        poses.scale(scale, scale, 1.0F);
        int textAlpha = Math.max(4, Math.min(255, Math.round(alpha * 255.0F)));
        graphics.drawCenteredString(font, text, 0, -font.lineHeight / 2,
                textAlpha << 24 | 0x00FFFFFF);
        poses.popPose();
    }

    private void renderFullscreenFade(GuiGraphics graphics, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        int fadeAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        graphics.fill(0, 0, width, height, fadeAlpha << 24);
    }

    private void renderPresentationChallenge(GuiGraphics graphics) {
        double elapsed = bubbleElapsedSeconds();
        int radius = Math.max(16, Math.min(width, height) / 24);
        for (int packed : contestState.presentationTargets()) {
            PresentationBubbleChallenge.BubbleType type =
                    PresentationBubbleChallenge.unpackType(packed);
            int x = bubbleX(packed, elapsed);
            int y = bubbleY(packed, elapsed);
            float floatingScale = bubbleScale(packed, elapsed);
            drawBubble(graphics, x, y, Math.round(radius * floatingScale), type);
        }
        drawPresentationGauge(graphics, contestState.resolvedTargets());
        graphics.drawCenteredString(font, Component.translatable(
                        "cobble_contests.presentation.timer", String.format(Locale.ROOT, "%.1f", remainingSeconds())),
                width / 2, py(0.075F), 0xFFFFFFFF);
    }

    private void drawBubble(GuiGraphics graphics, int centerX, int centerY, int radius,
                            PresentationBubbleChallenge.BubbleType type) {
        ResourceLocation texture = switch (type) {
            case POSITIVE_HEART -> POSITIVE_BUBBLE;
            case NEGATIVE_HEART -> NEGATIVE_BUBBLE;
            case BORED -> BORED_BUBBLE;
        };
        int sourceSize = type == PresentationBubbleChallenge.BubbleType.POSITIVE_HEART
                ? CUSTOM_ICON_SOURCE_SIZE : BUBBLE_SOURCE_SIZE;
        int diameter = radius * 2;
        graphics.blit(texture, centerX - radius, centerY - radius, diameter, diameter,
                0.0F, 0.0F, sourceSize, sourceSize, sourceSize, sourceSize);
    }

    private void drawPresentationGauge(GuiGraphics graphics, int value) {
        int segmentWidth = Math.max(8, px(0.025F));
        int gap = Math.max(2, px(0.004F));
        int totalWidth = PresentationBubbleChallenge.MAX_GAUGE * segmentWidth
                + (PresentationBubbleChallenge.MAX_GAUGE - 1) * gap;
        int startX = (width - totalWidth) / 2;
        int y = py(0.865F);
        int h = Math.max(8, py(0.025F));
        long animationAge = System.nanoTime() - gaugeAnimationStartedAtNanos;
        boolean animating = gaugeAnimationTo != gaugeAnimationFrom
                && animationAge >= 0L && animationAge < GAUGE_ANIMATION_NANOS;
        boolean increasing = animating && gaugeAnimationTo > gaugeAnimationFrom;
        boolean decreasing = animating && gaugeAnimationTo < gaugeAnimationFrom;
        int animationFrame = animating
                ? Math.min(4, (int) (animationAge * 5L / GAUGE_ANIMATION_NANOS))
                : 4;
        for (int index = 0; index < PresentationBubbleChallenge.MAX_GAUGE; index++) {
            int x = startX + index * (segmentWidth + gap);
            int fillColor = index < value ? 0xFFFF5E91 : 0xBB202A52;
            if (increasing && index == value - 1) {
                int alpha = 70 + animationFrame * 45;
                fillColor = alpha << 24 | 0x00FF5E91;
            }
            if (decreasing && index == value) {
                int alpha = Math.max(0, 230 - animationFrame * 45);
                fillColor = alpha << 24 | 0x00C52B55;
            }
            graphics.fill(x, y, x + segmentWidth, y + h, fillColor);
            int border = (index + 1) % 5 == 0 ? 0xFFFFD966 : 0xFF71D8FF;
            if (decreasing && index == value) {
                border = 0xFFFF405F;
            }
            graphics.renderOutline(x, y, segmentWidth, h, border);
            if (increasing && index == value - 1 && animationFrame >= 2) {
                int spark = Math.max(1, animationFrame - 1);
                graphics.fill(x + segmentWidth / 2 - spark, y - spark * 2,
                        x + segmentWidth / 2 + spark + 1, y, 0xFFFFFFFF);
            }
        }
    }

    private void renderPopEffects(GuiGraphics graphics) {
        long now = System.nanoTime();
        for (PopEffect effect : popEffects) {
            float progress = Math.min(1.0F,
                    (now - effect.startedAtNanos) / (float) POP_DURATION_NANOS);
            float framePosition = progress * (POP_FRAMES.length - 1);
            int firstFrame = Math.min(POP_FRAMES.length - 1, (int) Math.floor(framePosition));
            int secondFrame = Math.min(POP_FRAMES.length - 1, firstFrame + 1);
            float blend = framePosition - firstFrame;
            int baseSize = Math.max(48, Math.min(width, height) / 8);
            int size = Math.round(baseSize * (0.82F + progress * 0.28F));
            drawPopFrame(graphics, POP_FRAMES[firstFrame], effect.x, effect.y, size,
                    1.0F - blend);
            if (secondFrame != firstFrame) {
                drawPopFrame(graphics, POP_FRAMES[secondFrame], effect.x, effect.y, size, blend);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawPopFrame(GuiGraphics graphics, ResourceLocation texture,
                              int centerX, int centerY, int size, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
        graphics.blit(texture, centerX - size / 2, centerY - size / 2,
                size, size, 0.0F, 0.0F, BUBBLE_SOURCE_SIZE, BUBBLE_SOURCE_SIZE,
                BUBBLE_SOURCE_SIZE, BUBBLE_SOURCE_SIZE);
    }

    private void renderCurtain(GuiGraphics graphics, float openness) {
        float easedOpenness = ease(Math.max(0.0F, Math.min(1.0F, openness)));
        int firstFrame = 0;
        while (firstFrame + 1 < CURTAIN_OPENNESS.length
                && easedOpenness > CURTAIN_OPENNESS[firstFrame + 1]) {
            firstFrame++;
        }
        int secondFrame = Math.min(CURTAIN_FRAMES.length - 1, firstFrame + 1);
        float range = CURTAIN_OPENNESS[secondFrame] - CURTAIN_OPENNESS[firstFrame];
        float blend = secondFrame == firstFrame || range <= 0.0F
                ? 0.0F
                : (easedOpenness - CURTAIN_OPENNESS[firstFrame]) / range;
        blitFullscreen(graphics, CURTAIN_FRAMES[firstFrame]);
        if (secondFrame != firstFrame && blend > 0.0F) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, blend));
            blitFullscreen(graphics, CURTAIN_FRAMES[secondFrame]);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private float curtainProgress() {
        float progress = (System.nanoTime() - transitionStartedAtNanos)
                / (float) CURTAIN_DURATION_NANOS;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private void renderHost(GuiGraphics graphics) {
        int scale = Math.max(4, Math.min(width, height) / 72);
        int x = px(0.390F) - 8 * scale;
        int counterY = Math.round(height * (WELCOME_COUNTER_SOURCE_Y / (float) SOURCE_HEIGHT));
        int y = counterY - 20 * scale;
        drawSkinPart(graphics, x + 4 * scale, y, 8 * scale, 8 * scale, 8, 8, 8, 8);
        drawSkinPart(graphics, x + 4 * scale, y, 8 * scale, 8 * scale, 40, 8, 8, 8);
        int bodyY = y + 8 * scale;
        drawSkinPart(graphics, x + 4 * scale, bodyY, 8 * scale, 12 * scale, 20, 20, 8, 12);
        drawSkinPart(graphics, x, bodyY, 4 * scale, 12 * scale, 44, 20, 4, 12);
        drawSkinPart(graphics, x + 12 * scale, bodyY, 4 * scale, 12 * scale, 36, 52, 4, 12);
        drawSkinPart(graphics, x + 4 * scale, bodyY, 8 * scale, 12 * scale, 20, 36, 8, 12);
    }

    private void drawSkinPart(GuiGraphics graphics, int x, int y, int width, int height,
                              int u, int v, int sourceWidth, int sourceHeight) {
        graphics.blit(HOST_SKIN, x, y, width, height,
                u, v, sourceWidth, sourceHeight, 64, 64);
    }

    private void drawStatsDiamond(GuiGraphics graphics, CVs stats) {
        float centerX = px(0.750F);
        float centerY = py(0.237F);
        float maximum = Math.min(px(0.080F), py(0.127F));
        float[] values = {
                stats.getCool() / 255.0F, stats.getBeauty() / 255.0F,
                stats.getCute() / 255.0F, stats.getSmart() / 255.0F,
                stats.getTough() / 255.0F
        };
        Vector2f[] points = new Vector2f[5];
        for (int index = 0; index < points.length; index++) {
            double angle = Math.toRadians(-90.0D + index * 72.0D);
            float ratio = Math.max(0.04F, Math.min(1.0F, values[index]));
            points[index] = new Vector2f(
                    centerX + (float) Math.cos(angle) * maximum * ratio,
                    centerY + (float) Math.sin(angle) * maximum * ratio
            );
        }
        Vector2f center = new Vector2f(centerX, centerY);
        Vector3f color = categoryColor(selectedCategory);
        for (int index = 0; index < points.length; index++) {
            drawTriangle(color, points[index], center, points[(index + 1) % points.length]);
        }

        String[] keys = {"cool", "beauty", "cute", "smart", "tough"};
        for (int index = 0; index < keys.length; index++) {
            double angle = Math.toRadians(-90.0D + index * 72.0D);
            int labelX = Math.round(centerX + (float) Math.cos(angle) * maximum * 1.28F);
            int labelY = Math.round(centerY + (float) Math.sin(angle) * maximum * 1.28F);
            graphics.drawCenteredString(font,
                    Component.translatable("cobble_contests.contest_type." + keys[index]),
                    labelX, labelY - font.lineHeight / 2, 0xFFFFFFFF);
        }
    }

    private void drawTriangle(Vector3f color, Vector2f v1, Vector2f v2, Vector2f v3) {
        RenderSystem.setShaderTexture(0, CobblemonResources.INSTANCE.getWHITE());
        RenderSystem.setShaderColor(color.x, color.y, color.z, 0.62F);
        RenderSystem.enableBlend();
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION
        );
        builder.addVertex(v1.x, v1.y, 20.0F);
        builder.addVertex(v2.x, v2.y, 20.0F);
        builder.addVertex(v3.x, v3.y, 20.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static Vector3f categoryColor(int category) {
        return switch (category) {
            case 0 -> new Vector3f(1.0F, 0.30F, 0.34F);
            case 1 -> new Vector3f(0.25F, 0.82F, 1.0F);
            case 2 -> new Vector3f(1.0F, 0.42F, 0.78F);
            case 3 -> new Vector3f(0.25F, 0.88F, 0.45F);
            case 4 -> new Vector3f(1.0F, 0.68F, 0.20F);
            default -> new Vector3f(0.55F, 0.82F, 1.0F);
        };
    }

    private void blitFullscreen(GuiGraphics graphics, ResourceLocation texture) {
        graphics.blit(texture, 0, 0, width, height, 0.0F, 0.0F,
                SOURCE_WIDTH, SOURCE_HEIGHT, SOURCE_WIDTH, SOURCE_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (page == Page.WELCOME) {
            renderWelcomeText(graphics);
        } else if (page == Page.POKEMON_SELECTION) {
            renderPokemonSelectionText(graphics);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderWelcomeText(GuiGraphics graphics) {
        int panelCenterX = px(0.847F);
        if (selectedCategory >= 0) {
            graphics.drawCenteredString(font, Component.translatable(contestTypeKey(selectedCategory)),
                    panelCenterX, py(0.170F), categoryTextColor(selectedCategory));
        }
        if (selectedCategory < 0 || selectedRank < 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("cobble_contests.welcome.choose_category_rank"),
                    px(0.390F), py(0.838F), 0xFFFFFFFF);
        } else {
            graphics.drawCenteredString(font, Component.translatable(
                            "cobble_contests.welcome.summary",
                            Component.translatable(contestTypeKey(selectedCategory)),
                            Component.translatable(rankKey(selectedRank))),
                    px(0.390F), py(0.838F), 0xFFFFFFFF);
        }
    }

    private void renderPokemonSelectionText(GuiGraphics graphics) {
        if (selectedCategory >= 0 && selectedRank >= 0) {
            graphics.drawCenteredString(font, Component.translatable(
                            "cobble_contests.selection.category_rank",
                            Component.translatable(contestTypeKey(selectedCategory)),
                            Component.translatable(rankKey(selectedRank))),
                    px(0.465F), py(0.898F), 0xFFFFFFFF);
        }
        if (!contestDataLoaded) {
            graphics.drawCenteredString(font,
                    Component.translatable("cobble_contests.selection.loading"),
                    px(0.800F), py(0.920F), 0xFF9EEBFF);
        } else if (awaitingServer) {
            graphics.drawCenteredString(font,
                    Component.translatable("cobble_contests.selection.waiting"),
                    px(0.800F), py(0.920F), 0xFFFFFFFF);
        } else if (selectedPokemon >= 0 && !canSelectedPokemonEnterRank()) {
            graphics.drawCenteredString(font,
                    Component.translatable("cobble_contests.error.rank_locked_short"),
                    px(0.800F), py(0.920F), 0xFFFF7070);
        }
    }

    private static int categoryTextColor(int category) {
        return switch (category) {
            case 0 -> 0xFFFF6671;
            case 1 -> 0xFF65DFFF;
            case 2 -> 0xFFFF7CC7;
            case 3 -> 0xFF65E883;
            case 4 -> 0xFFFFB347;
            default -> 0xFFFFFFFF;
        };
    }

    private double remainingSeconds() {
        if (contestState == null) {
            return 0.0D;
        }
        double elapsed = (System.nanoTime() - stateReceivedAtNanos) / 1_000_000_000.0D;
        return Math.max(0.0D, contestState.remainingTicks() / 20.0D - elapsed);
    }

    private double bubbleElapsedSeconds() {
        if (bubbleShownAtNanos == 0L) {
            return 0.0D;
        }
        return Math.max(0.0D,
                (System.nanoTime() - bubbleShownAtNanos) / 1_000_000_000.0D);
    }

    private int bubbleX(int packed, double elapsed) {
        int index = PresentationBubbleChallenge.unpackBubbleIndex(packed);
        double sway = Math.sin(elapsed * 2.7D + index * 1.73D) * width * 0.012D;
        double drift = Math.sin(elapsed * 1.15D + index * 0.61D) * width * 0.004D;
        return px(PresentationBubbleChallenge.unpackX(packed) / 100.0F)
                + (int) Math.round(sway + drift);
    }

    private int bubbleY(int packed, double elapsed) {
        int index = PresentationBubbleChallenge.unpackBubbleIndex(packed);
        double rise = elapsed * Math.max(5, height * 0.025D);
        double bob = Math.sin(elapsed * 3.4D + index * 0.91D) * height * 0.006D;
        return py(PresentationBubbleChallenge.unpackY(packed) / 100.0F)
                - (int) Math.round(rise - bob);
    }

    private static float bubbleScale(int packed, double elapsed) {
        int index = PresentationBubbleChallenge.unpackBubbleIndex(packed);
        return 1.0F + 0.045F * (float) Math.sin(elapsed * 2.2D + index * 1.31D);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.PRESENTATION && button == 0 && !awaitingBubbleAck
                && contestState != null
                && contestState.phase() == ContestPhase.PRESENTATION.ordinal()) {
            double elapsed = bubbleElapsedSeconds();
            int radius = Math.max(16, Math.min(width, height) / 24);
            int clickedTarget = -1;
            int clickedX = 0;
            int clickedY = 0;
            double closestDistance = Double.MAX_VALUE;
            for (int packed : contestState.presentationTargets()) {
                int bubbleX = bubbleX(packed, elapsed);
                int bubbleY = bubbleY(packed, elapsed);
                double dx = mouseX - bubbleX;
                double dy = mouseY - bubbleY;
                double distance = dx * dx + dy * dy;
                if (distance <= radius * radius && distance < closestDistance) {
                    clickedTarget = PresentationBubbleChallenge.unpackBubbleIndex(packed);
                    clickedX = bubbleX;
                    clickedY = bubbleY;
                    closestDistance = distance;
                }
            }
            if (clickedTarget >= 0) {
                awaitingBubbleAck = true;
                popEffects.add(new PopEffect(clickedX, clickedY, System.nanoTime()));
                menu.sendContestAction(contestState.sessionId(), contestState.phase(),
                        contestState.stateVersion(), clickedTarget);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playSelectedPokemonCry() {
        Pokemon pokemon = selectedPokemon();
        if (pokemon == null || minecraft == null || minecraft.player == null) {
            return;
        }
        ResourceLocation species = pokemon.getSpecies().getResourceIdentifier();
        ResourceLocation cry = ResourceLocation.fromNamespaceAndPath(
                "cobblemon", "pokemon." + species.getPath() + ".cry"
        );
        minecraft.player.playSound(SoundEvent.createVariableRangeEvent(cry), 1.0F, 1.0F);
    }

    private void startMusicLoops() {
        if (minecraft == null || menuMusic != null || challengeMusic != null) {
            return;
        }
        minecraft.getMusicManager().stopPlaying();
        menuMusic = new LoopingContestMusic(MENU_MUSIC, 0.0F);
        challengeMusic = new LoopingContestMusic(CHALLENGE_MUSIC, 0.0F);
        minecraft.getSoundManager().play(menuMusic);
        minecraft.getSoundManager().play(challengeMusic);
    }

    private void switchToMenuMusic() {
        startMusicLoops();
        if (menuMusic != null) {
            menuMusic.fadeTo(0.62F);
        }
        if (challengeMusic != null) {
            challengeMusic.fadeTo(0.0F);
        }
    }

    private void switchToChallengeMusic() {
        startMusicLoops();
        if (menuMusic != null) {
            menuMusic.fadeTo(0.0F);
        }
        if (challengeMusic != null) {
            challengeMusic.fadeTo(0.68F);
        }
    }

    @Override
    public void removed() {
        if (menuMusic != null) {
            menuMusic.stopImmediately();
            menuMusic = null;
        }
        if (challengeMusic != null) {
            challengeMusic.stopImmediately();
            challengeMusic = null;
        }
        super.removed();
    }

    private int px(float ratio) {
        return Math.round(width * ratio);
    }

    private int py(float ratio) {
        return Math.round(height * ratio);
    }

    private static String contestTypeKey(int type) {
        return "cobble_contests.contest_type." + switch (type) {
            case 0 -> "cool";
            case 1 -> "beauty";
            case 2 -> "cute";
            case 3 -> "smart";
            case 4 -> "tough";
            default -> "cool";
        };
    }

    private static String rankKey(int rank) {
        return "cobble_contests.contest_level." + Math.max(0, Math.min(4, rank));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private enum Page {
        WELCOME,
        POKEMON_SELECTION,
        CURTAIN_CLOSING_TO_CHALLENGE,
        COUNTDOWN,
        CURTAIN_OPENING,
        PRESENTATION,
        CURTAIN_CLOSING_TO_RESULTS,
        RESULTS
    }

    private record PopEffect(int x, int y, long startedAtNanos) {
    }

    private final class CategoryButton extends AbstractButton {
        private final int category;
        private final Runnable action;

        private CategoryButton(int x, int y, int size, int category, Component label,
                               Runnable action) {
            super(x, y, size, size, label);
            this.category = category;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (selectedCategory == category || isHoveredOrFocused()) {
                graphics.fill(getX() - 3, getY() - 3,
                        getX() + getWidth() + 3, getY() + getHeight() + 3,
                        selectedCategory == category ? 0x66FFFFFF : 0x443FD9FF);
            }
            int iconSize = Math.min(getWidth(), getHeight());
            graphics.blit(CATEGORY_ICONS[category], getX(), getY(), iconSize, iconSize,
                    0.0F, 0.0F, CUSTOM_ICON_SOURCE_SIZE, CUSTOM_ICON_SOURCE_SIZE,
                    CUSTOM_ICON_SOURCE_SIZE, CUSTOM_ICON_SOURCE_SIZE);

            int labelWidth = Math.min(getWidth() - 8, font.width(getMessage()) + 14);
            int labelHeight = font.lineHeight + 7;
            int labelX = getX() + (getWidth() - labelWidth) / 2;
            int labelY = getY() + getHeight() - labelHeight - 7;
            graphics.fill(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                    0xD91A2149);
            graphics.renderOutline(labelX, labelY, labelWidth, labelHeight,
                    selectedCategory == category ? 0xFFFFFFFF : 0xFFFFD966);
            graphics.drawCenteredString(font, getMessage(),
                    getX() + getWidth() / 2, labelY + 4, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class PartySlotButton extends AbstractButton {
        private final int slot;
        private final Pokemon pokemon;
        private final Runnable action;

        private PartySlotButton(int x, int y, int width, int height, int slot,
                                Pokemon pokemon, Runnable action) {
            super(x, y, width, height, pokemon == null
                    ? Component.translatable("cobble_contests.selection.empty")
                    : pokemon.getDisplayName(false));
            this.slot = slot;
            this.pokemon = pokemon;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int overlay = selectedPokemon == slot ? 0x664BEAFF
                    : isHoveredOrFocused() ? 0x33FFFFFF : 0x00000000;
            if (overlay != 0) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), overlay);
                graphics.renderOutline(getX(), getY(), getWidth(), getHeight(),
                        selectedPokemon == slot ? 0xFFFFFFFF : 0xFF7CEBFF);
            }
            if (pokemon == null) {
                graphics.drawCenteredString(font,
                        Component.translatable("cobble_contests.selection.empty"),
                        getX() + getWidth() / 2, getY() + getHeight() / 2, 0xFF8994B5);
                return;
            }

            PoseStack poses = graphics.pose();
            int modelBottom = getY() + getHeight() - font.lineHeight - 12;
            graphics.enableScissor(getX() + 4, getY() + 4,
                    getX() + getWidth() - 4, modelBottom);
            poses.pushPose();
            poses.translate(getX() + getWidth() / 2.0F,
                    getY() + getHeight() * 0.28F, 30.0F);
            drawProfilePokemon(
                    pokemon.asRenderablePokemon(), poses,
                    new Quaternionf().rotationXYZ((float) Math.toRadians(13.0F),
                            (float) Math.toRadians(35.0F), 0.0F),
                    PoseType.PROFILE, new FloatingState(), partialTick,
                    Math.min(getWidth(), getHeight()) * 0.36F,
                    true, false, 1.0F, 1.0F, 1.0F, active ? 1.0F : 0.45F,
                    0.0F, 0.0F
            );
            poses.popPose();
            graphics.disableScissor();
            graphics.drawCenteredString(font, pokemon.getDisplayName(false),
                    getX() + getWidth() / 2, getY() + getHeight() - font.lineHeight - 6,
                    active ? 0xFFFFFFFF : 0xFF888888);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
