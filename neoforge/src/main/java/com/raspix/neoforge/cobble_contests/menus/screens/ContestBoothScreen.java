package com.raspix.neoforge.cobble_contests.menus.screens;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.raspix.common.cobble_contests.CobbleContests;
import com.raspix.common.cobble_contests.contest.ContestPhase;
import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import com.raspix.neoforge.cobble_contests.menus.widgets.FixedImageButton;
import com.raspix.neoforge.cobble_contests.menus.widgets.PokemonContestBoothSlotButton;
import com.raspix.neoforge.cobble_contests.network.CBContestState;
import net.minecraft.ChatFormatting;
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
import java.util.Locale;
import java.util.UUID;

import static com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText;

@OnlyIn(Dist.CLIENT)
public class ContestBoothScreen extends AbstractContainerScreen<ContestBoothMenu> {
    private static final int STARTING_PAGE = 0;
    private static final int CONTEST_TYPE_SELECTION = 1;
    private static final int CONTEST_WAITING_PAGE = 2;
    private static final int POKEMON_SELECTION_PAGE = 3;
    private static final int CONTEST_RUNNING_PAGE = 4;
    private static final int RESULTS_PAGE = 5;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleContests.MOD_ID, "textures/gui/contest_booth.png"
    );

    private int pageIndex;
    private int pokemonIndex;
    private int colorIndex;
    private ClientParty clientParty;
    private List<Button> homeButtons = new ArrayList<>();
    private List<Button> waitButtons = new ArrayList<>();
    private List<Button> typeButtons = new ArrayList<>();
    private List<Button> partyButtons = new ArrayList<>();
    private List<Button> contestButtons = new ArrayList<>();
    private final Inventory playerInventory;
    private UUID playerId;
    private CBContestState contestState;
    private long stateReceivedAtNanos;

    public ContestBoothScreen(ContestBoothMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 288;
        this.imageHeight = 224;
        this.playerInventory = playerInventory;
    }

    @Override
    protected void init() {
        super.init();
        pokemonIndex = 0;
        pageIndex = STARTING_PAGE;
        colorIndex = -1;
        clientParty = CobblemonClient.INSTANCE.getStorage().getParty();
        playerId = playerInventory.player.getUUID();

        createHomeButtons();
        createWaitingButtons();
        createTypeButtons();
        createPartyButtons();
        setPageIndex(STARTING_PAGE);
    }

    private void createHomeButtons() {
        homeButtons.add(addRenderableWidget(new FixedImageButton(
                leftPos + 110, topPos + 40, 64, 18, 289, 43, 18,
                TEXTURE, 1000, 750, button -> setPageIndex(CONTEST_TYPE_SELECTION)
        )));
    }

    private void createWaitingButtons() {
        waitButtons.add(addRenderableWidget(new FixedImageButton(
                leftPos + 110, topPos + 140, 64, 18, 289, 43, 18,
                TEXTURE, 1000, 750, button -> requestContestStart()
        )));
    }

    private void createTypeButtons() {
        int[][] positions = {{134, 49}, {210, 78}, {173, 155}, {95, 155}, {58, 78}};
        for (int type = 0; type < positions.length; type++) {
            int selectedType = type;
            typeButtons.add(addRenderableWidget(new FixedImageButton(
                    leftPos + positions[type][0], topPos + positions[type][1], 20, 20,
                    288 + type * 20, 0, 21, TEXTURE, 1000, 750,
                    button -> selectContestType(selectedType)
            )));
        }
    }

    private void createPartyButtons() {
        for (int index = 0; index < clientParty.getSlots().size(); index++) {
            if (clientParty.get(index) == null) {
                continue;
            }
            int buttonX = leftPos + 39 + 73 * (index % 3);
            int buttonY = topPos + 36 + 81 * (index / 3);
            int selectedIndex = index;
            partyButtons.add(addRenderableWidget(new PokemonContestBoothSlotButton(
                    buttonX, buttonY, 64, 70, 418, 1, 72, TEXTURE, 1000, 750,
                    button -> selectPokemon(selectedIndex), clientParty.get(index)
            )));
        }
    }

    public void applyContestState(CBContestState state) {
        if (contestState != null && contestState.sessionId().equals(state.sessionId())
                && state.stateVersion() < contestState.stateVersion()) {
            return;
        }
        contestState = state;
        stateReceivedAtNanos = System.nanoTime();
        setPageIndex(state.finished() ? RESULTS_PAGE : CONTEST_RUNNING_PAGE);
        rebuildContestButtons();
    }

    private void rebuildContestButtons() {
        for (Button button : contestButtons) {
            button.visible = false;
            button.active = false;
        }
        contestButtons = new ArrayList<>();
        if (contestState == null || contestState.finished()) {
            return;
        }

        ContestPhase phase = currentPhase();
        switch (phase) {
            case PRESENTATION -> addPresentationButton();
            case CAPABILITIES -> addMoveButtons();
            case CATEGORY -> addPrecisionTargets();
            case RHYTHM, FINALE -> addTimingButton();
            case EVALUATION, RESULTS -> {
            }
        }
    }

    private void addPresentationButton() {
        int seed = contestState.sessionId().hashCode() + contestState.progress() * 71;
        int x = leftPos + 35 + Math.floorMod(seed, 170);
        int y = topPos + 80 + Math.floorMod(seed / 17, 75);
        addContestButton(Button.builder(
                Component.translatable("cobble_contests.action.present"),
                button -> submitAction(contestState.progress())
        ).bounds(x, y, 82, 20).build());
    }

    private void addMoveButtons() {
        String[] moves = contestState.moveNames();
        for (int index = 0; index < moves.length; index++) {
            if (moves[index].isBlank()) {
                continue;
            }
            int selectedMove = index;
            Component name = Component.translatable("move." + moves[index]);
            addContestButton(Button.builder(name, button -> submitAction(selectedMove))
                    .bounds(leftPos + 36 + (index % 2) * 112,
                            topPos + 92 + (index / 2) * 28, 104, 20)
                    .build());
        }
    }

    private void addPrecisionTargets() {
        for (int index = 0; index < 12; index++) {
            if ((contestState.resolvedTargets() & (1 << index)) != 0) {
                continue;
            }
            int kind = (contestState.packedTargets() >> (index * 2)) & 3;
            Component label = switch (kind) {
                case 0 -> Component.literal("●").withStyle(ChatFormatting.GREEN);
                case 1 -> Component.literal("★").withStyle(ChatFormatting.GOLD);
                case 2 -> Component.translatable("cobble_contests.target.decoy")
                        .withStyle(ChatFormatting.RED);
                default -> Component.literal("?");
            };
            int targetIndex = index;
            addContestButton(Button.builder(label, button -> submitAction(targetIndex))
                    .bounds(leftPos + 28 + (index % 4) * 59,
                            topPos + 72 + (index / 4) * 31, 50, 20)
                    .build());
        }
    }

    private void addTimingButton() {
        Component label = Component.translatable(currentPhase() == ContestPhase.RHYTHM
                ? "cobble_contests.action.rhythm" : "cobble_contests.action.finale");
        addContestButton(Button.builder(label, button -> submitAction(contestState.progress()))
                .bounds(leftPos + 94, topPos + 137, 100, 20)
                .build());
    }

    private void addContestButton(Button button) {
        contestButtons.add(addRenderableWidget(button));
    }

    private void submitAction(int value) {
        if (contestState == null || contestState.finished()) {
            return;
        }
        for (Button button : contestButtons) {
            button.active = false;
        }
        menu.sendContestAction(contestState.sessionId(), value);
    }

    private void selectPokemon(int index) {
        pokemonIndex = index;
        setPageIndex(CONTEST_WAITING_PAGE);
    }

    private void selectContestType(int type) {
        colorIndex = type;
        setPageIndex(POKEMON_SELECTION_PAGE);
    }

    private void requestContestStart() {
        if (colorIndex < 0 || pokemonIndex < 0 || clientParty.get(pokemonIndex) == null) {
            return;
        }
        menu.startStatAssesment(playerId, pokemonIndex, colorIndex);
        for (Button button : waitButtons) {
            button.active = false;
        }
    }

    private void setPageIndex(int index) {
        pageIndex = index;
        setVisible(homeButtons, index == STARTING_PAGE);
        setVisible(waitButtons, index == CONTEST_WAITING_PAGE);
        setVisible(typeButtons, index == CONTEST_TYPE_SELECTION);
        setVisible(partyButtons, index == POKEMON_SELECTION_PAGE);
        setVisible(contestButtons, index == CONTEST_RUNNING_PAGE);
    }

    private static void setVisible(List<Button> buttons, boolean visible) {
        for (Button button : buttons) {
            button.visible = visible;
            if (visible) {
                button.active = true;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (pageIndex == CONTEST_TYPE_SELECTION) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 1000, 750);
        } else if (pageIndex == POKEMON_SELECTION_PAGE) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 225, imageWidth, imageHeight, 1000, 750);
        } else {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 450, imageWidth, imageHeight, 1000, 750);
        }
        if (pageIndex == CONTEST_RUNNING_PAGE || pageIndex == RESULTS_PAGE) {
            graphics.fill(leftPos + 15, topPos + 15, leftPos + imageWidth - 15,
                    topPos + imageHeight - 15, 0xDD241A2C);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        switch (pageIndex) {
            case STARTING_PAGE -> drawCentered(graphics,
                    Component.translatable("cobble_contests.contest_text.start"), 44, 0x918B99);
            case CONTEST_TYPE_SELECTION -> renderTypeSelection(graphics);
            case POKEMON_SELECTION_PAGE -> drawCentered(graphics,
                    Component.translatable("cobble_contests.contest_text.pokemon_select"), 10, 0xFFFFFF);
            case CONTEST_WAITING_PAGE -> renderWaiting(graphics);
            case CONTEST_RUNNING_PAGE -> renderContest(graphics);
            case RESULTS_PAGE -> renderResults(graphics);
            default -> {
            }
        }
    }

    private void renderTypeSelection(GuiGraphics graphics) {
        drawCentered(graphics, Component.translatable("cobble_contests.contest_text.select_type"),
                104, 0x918B99);
        String[] keys = {"cool", "beauty", "cute", "smart", "tough"};
        int[][] positions = {{143, 78}, {220, 106}, {184, 184}, {104, 184}, {68, 106}};
        for (int index = 0; index < keys.length; index++) {
            Component label = Component.translatable("cobble_contests.contest_type." + keys[index]);
            drawScaledText(graphics, label.getVisualOrderText(), leftPos + positions[index][0],
                    topPos + positions[index][1], 1F, 1F, 1F, 0x00918B99, true, false);
        }
    }

    private void renderWaiting(GuiGraphics graphics) {
        drawCentered(graphics, Component.translatable("cobble_contests.contest_text.start"),
                145, 0x918B99);
        String pokemonName = clientParty.get(pokemonIndex).getDisplayName(false).getString();
        graphics.drawString(font, Component.translatable("cobble_contests.contest_text.selected_pokemon",
                pokemonName), leftPos + 40, topPos + 50, 0x918B99, false);
        graphics.drawString(font, Component.translatable("cobble_contests.contest_text.selected_type",
                Component.translatable(contestTypeKey(colorIndex))), leftPos + 40, topPos + 70,
                0x918B99, false);
    }

    private void renderContest(GuiGraphics graphics) {
        if (contestState == null) {
            return;
        }
        ContestPhase phase = currentPhase();
        drawCentered(graphics, Component.translatable(phaseKey(phase)), 28, 0xFFFFFF);
        drawCentered(graphics, Component.translatable(instructionKey(phase)), 48, 0xE8D9F0);
        graphics.drawString(font, Component.translatable("cobble_contests.contest.rank",
                        Component.translatable(rankKey(contestState.rank()))),
                leftPos + 25, topPos + 185, 0xDFA9FF, false);
        graphics.drawString(font, Component.translatable("cobble_contests.contest.timer",
                        String.format(Locale.ROOT, "%.1f", remainingSeconds())),
                leftPos + 196, topPos + 185, 0xFFFFFF, false);
        renderCompletedScore(graphics, phase);
        if (phase == ContestPhase.RHYTHM || phase == ContestPhase.FINALE) {
            renderTimingBar(graphics);
        }
    }

    private void renderCompletedScore(GuiGraphics graphics, ContestPhase phase) {
        int[] scores = contestState.scores();
        int x = leftPos + 25;
        int y = topPos + 202;
        int completed = Math.min(phase.ordinal(), scores.length);
        int subtotal = 0;
        for (int index = 0; index < completed; index++) {
            subtotal += scores[index] * ContestPhase.values()[index].weight();
        }
        graphics.drawString(font, Component.translatable("cobble_contests.contest.partial_score",
                Math.round(subtotal / 100.0F)), x, y, 0xFFFFFF, false);
    }

    private void renderTimingBar(GuiGraphics graphics) {
        int startX = leftPos + 54;
        int endX = leftPos + 234;
        int y = topPos + 112;
        graphics.fill(startX, y, endX, y + 5, 0xFF5A4A68);
        int center = (startX + endX) / 2;
        graphics.fill(center - 2, y - 4, center + 2, y + 9, 0xFFFFD966);
        double elapsedTicks = (System.nanoTime() - stateReceivedAtNanos) / 50_000_000.0;
        double ticksUntilTarget = contestState.timingTicks() - elapsedTicks;
        int marker = (int) Math.round(center - ticksUntilTarget * 4.0);
        marker = Math.max(startX, Math.min(endX - 3, marker));
        graphics.fill(marker, y - 2, marker + 3, y + 7, 0xFF72E6FF);
    }

    private void renderResults(GuiGraphics graphics) {
        if (contestState == null) {
            return;
        }
        Component title = Component.translatable(contestState.won()
                ? "cobble_contests.contest_result.victory" : "cobble_contests.contest_result.defeat");
        drawCentered(graphics, title, 25, contestState.won() ? 0x7CFF92 : 0xFF8585);
        int[] scores = contestState.scores();
        ContestPhase[] phases = ContestPhase.values();
        for (int index = 0; index < scores.length; index++) {
            graphics.drawString(font, Component.translatable("cobble_contests.contest_result.phase_line",
                            Component.translatable(phaseKey(phases[index])), scores[index], phases[index].weight()),
                    leftPos + 42, topPos + 48 + index * 20, 0xFFFFFF, false);
        }
        drawCentered(graphics, Component.translatable("cobble_contests.contest_result.total",
                contestState.totalScore(), contestState.requiredScore()), 178, 0xFFD966);
        if (contestState.won()) {
            drawCentered(graphics, Component.translatable("cobble_contests.contest_result.ribbon_awarded"),
                    198, 0xDFA9FF);
        }
    }

    private double remainingSeconds() {
        double elapsed = (System.nanoTime() - stateReceivedAtNanos) / 1_000_000_000.0;
        return Math.max(0.0, contestState.remainingTicks() / 20.0 - elapsed);
    }

    private ContestPhase currentPhase() {
        ContestPhase[] phases = ContestPhase.values();
        if (contestState == null || contestState.phase() < 0 || contestState.phase() >= phases.length) {
            return ContestPhase.RESULTS;
        }
        return phases[contestState.phase()];
    }

    private void drawCentered(GuiGraphics graphics, Component text, int relativeY, int color) {
        graphics.drawCenteredString(font, text, leftPos + imageWidth / 2, topPos + relativeY, color);
    }

    private static String phaseKey(ContestPhase phase) {
        return "cobble_contests.phase." + phase.name().toLowerCase(Locale.ROOT);
    }

    private static String instructionKey(ContestPhase phase) {
        return "cobble_contests.phase." + phase.name().toLowerCase(Locale.ROOT) + ".instruction";
    }

    private static String contestTypeKey(int type) {
        String suffix = switch (type) {
            case 0 -> "cool";
            case 1 -> "beauty";
            case 2 -> "cute";
            case 3 -> "smart";
            case 4 -> "tough";
            default -> "cool";
        };
        return "cobble_contests.contest_type." + suffix;
    }

    private static String rankKey(int rank) {
        return "cobble_contests.contest_level." + Math.max(0, Math.min(4, rank));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
