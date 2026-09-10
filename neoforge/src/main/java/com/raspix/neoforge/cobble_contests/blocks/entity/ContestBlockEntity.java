package com.raspix.neoforge.cobble_contests.blocks.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.raspix.common.cobble_contests.contest.ContestEvaluation;
import com.raspix.common.cobble_contests.contest.ContestSession;
import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.CobbleContestsMoves;
import com.raspix.neoforge.cobble_contests.events.ContestMoves;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import com.raspix.neoforge.cobble_contests.network.CBContestState;
import com.raspix.neoforge.cobble_contests.pokemon.CVs;
import com.raspix.neoforge.cobble_contests.pokemon.Ribbons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ContestBlockEntity extends BlockEntity implements MenuProvider {
    private static final Component TITLE = Component.translatable(
            "container." + CobbleContestsForge.MOD_ID + ".contest_block"
    );
    private static final int COOL_CATEGORY = 0;
    private static final int[] REQUIRED_SCORES = {50, 55, 60, 65, 70};

    private boolean isHost;
    private UUID hostId;
    private int contestType;
    private final Map<UUID, ContestParticipation> participants = new HashMap<>();
    private final Map<UUID, ActiveContest> activeContests = new HashMap<>();

    public ContestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ContestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CONTEST_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, Player player) {
        return new ContestBoothMenu(containerId, playerInventory, this);
    }

    public boolean tryHosting(UUID id) {
        if (!isHost) {
            hostId = id;
            isHost = true;
            return true;
        }
        return Objects.equals(hostId, id);
    }

    public boolean setContestType(UUID id, int type) {
        if (type < 0 || type > 4) {
            return false;
        }
        if ((isHost && Objects.equals(hostId, id)) || (!isHost && tryHosting(id))) {
            contestType = type;
            return true;
        }
        return false;
    }

    public void startContest(int color, UUID player) {
        setContestType(player, color);
    }

    public void addContestant(UUID player, int pokemonIndex) {
        if (pokemonIndex >= 0 && pokemonIndex <= 5) {
            participants.put(player, new ContestParticipation(player, pokemonIndex));
        }
    }

    public String getContestResults() {
        return "cobble_contests.contest_type.cool";
    }

    public void clearContest() {
        participants.clear();
        activeContests.clear();
        hostId = null;
        isHost = false;
        contestType = 0;
    }

    public void kickContestent(UUID id) {
        participants.remove(id);
        activeContests.remove(id);
    }

    public String getCurrentContestInfo() {
        if (!isHost) {
            return "No current host";
        }
        Player host = level == null ? null : level.getPlayerByUUID(hostId);
        return "Host: " + (host == null ? hostId : host.getDisplayName().getString())
                + ", Type: " + contestType;
    }

    /** Starts the interactive 0.2 contest, deriving every sensitive value on the server. */
    public void startContestSession(ServerPlayer player, int pokemonIndex, int requestedType) {
        if (level == null || level.isClientSide || pokemonIndex < 0 || pokemonIndex > 5
                || requestedType < 0 || requestedType > 4) {
            return;
        }

        Pokemon pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(pokemonIndex);
        if (pokemon == null) {
            player.displayClientMessage(Component.translatable("cobble_contests.error.invalid_pokemon"), false);
            return;
        }

        // The other four categories keep their original stat contest until their
        // definitive category challenges arrive in 0.3.0.
        if (requestedType != COOL_CATEGORY) {
            runLegacyStatContest(pokemon, requestedType, player);
            return;
        }

        Ribbons ribbons = Ribbons.getFromTag(pokemon.getPersistentData().getCompound("Ribbons"));
        int rank = ribbons.getNextContestLevel(COOL_CATEGORY);
        if (rank >= REQUIRED_SCORES.length) {
            player.displayClientMessage(Component.translatable(
                    "cobble_contests.contest_result.maxed_ranked",
                    pokemon.getDisplayName(false).getString(), getContestTypeString(COOL_CATEGORY)
            ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return;
        }

        CVs stats = CVs.getFromTag(pokemon.getPersistentData().getCompound("CVs"));
        int evaluationScore = ContestEvaluation.scoreForCondition(stats.getCool());
        List<ContestSession.MoveOption> moves = createMoveOptions(pokemon, COOL_CATEGORY);
        long gameTime = level.getGameTime();
        ContestSession session = new ContestSession(
                player.getUUID(), pokemonIndex, COOL_CATEGORY, rank, evaluationScore,
                moves, level.getRandom().nextLong(), gameTime
        );
        activeContests.put(player.getUUID(), new ActiveContest(session, pokemon));
        PacketDistributor.sendToPlayer(player, CBContestState.from(
                session, worldPosition, gameTime, REQUIRED_SCORES[rank]
        ));
    }

    public void handleContestAction(ServerPlayer player, UUID sessionId, int value) {
        if (level == null || level.isClientSide) {
            return;
        }
        ActiveContest active = activeContests.get(player.getUUID());
        if (active == null || !active.session.sessionId().equals(sessionId)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (!active.session.act(value, gameTime)) {
            return;
        }
        sendState(player, active.session, gameTime);
        finishIfReady(player, active);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ContestBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || blockEntity.activeContests.isEmpty()) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        Iterator<Map.Entry<UUID, ActiveContest>> iterator = blockEntity.activeContests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveContest> entry = iterator.next();
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
            ActiveContest active = entry.getValue();
            if (player == null || !(player.containerMenu instanceof ContestBoothMenu menu)
                    || menu.getBlockEntity() != blockEntity
                    || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                iterator.remove();
                continue;
            }

            if (active.session.tick(gameTime)) {
                blockEntity.sendState(player, active.session, gameTime);
                if (active.session.isFinished()) {
                    blockEntity.completeContest(player, active);
                    iterator.remove();
                }
            }
        }
    }

    private void finishIfReady(ServerPlayer player, ActiveContest active) {
        if (!active.session.isFinished()) {
            return;
        }
        completeContest(player, active);
        activeContests.remove(player.getUUID());
    }

    private void completeContest(ServerPlayer player, ActiveContest active) {
        ContestSession session = active.session;
        int required = REQUIRED_SCORES[session.rank()];
        boolean won = session.totalScore() >= required;
        String pokemonName = active.pokemon.getDisplayName(false).getString();
        if (won) {
            Ribbons ribbons = Ribbons.getFromTag(active.pokemon.getPersistentData().getCompound("Ribbons"));
            ribbons.setRankedCool(session.rank(), true);
            active.pokemon.getPersistentData().put("Ribbons", ribbons.saveToNBT());
            player.displayClientMessage(Component.translatable(
                    "cobble_contests.contest_result.won_ranked",
                    pokemonName, getContestLevelString(session.rank()), getContestTypeString(session.category())
            ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        } else {
            player.displayClientMessage(Component.translatable(
                    "cobble_contests.contest_result.lost_ranked",
                    pokemonName, getContestLevelString(session.rank()), getContestTypeString(session.category())
            ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
    }

    private void sendState(ServerPlayer player, ContestSession session, long gameTime) {
        PacketDistributor.sendToPlayer(player, CBContestState.from(
                session, worldPosition, gameTime, REQUIRED_SCORES[session.rank()]
        ));
    }

    private static List<ContestSession.MoveOption> createMoveOptions(Pokemon pokemon, int category) {
        List<ContestSession.MoveOption> options = new ArrayList<>(4);
        List<Move> pokemonMoves = pokemon.getMoveSet().getMoves();
        for (int index = 0; index < Math.min(4, pokemonMoves.size()); index++) {
            Move move = pokemonMoves.get(index);
            if (move == null) {
                continue;
            }
            ContestMoves.MoveData data = CobbleContestsMoves.INSTANCE.allMoves.get(move.getName());
            int appeal = data == null ? 1 : data.getAppeal();
            boolean categoryMatch = data != null && contestTypeMatches(category, data.getType());
            options.add(new ContestSession.MoveOption(move.getName(), appeal, categoryMatch));
        }
        return options;
    }

    private static boolean contestTypeMatches(int category, String type) {
        return switch (category) {
            case 0 -> "Cool".equalsIgnoreCase(type);
            case 1 -> "Beauty".equalsIgnoreCase(type);
            case 2 -> "Cute".equalsIgnoreCase(type);
            case 3 -> "Smart".equalsIgnoreCase(type);
            case 4 -> "Tough".equalsIgnoreCase(type);
            default -> false;
        };
    }

    private void runLegacyStatContest(Pokemon pokemon, int category, ServerPlayer player) {
        Ribbons ribbons = Ribbons.getFromTag(pokemon.getPersistentData().getCompound("Ribbons"));
        int rank = ribbons.getNextContestLevel(category);
        String pokemonName = pokemon.getDisplayName(false).getString();
        if (rank >= REQUIRED_SCORES.length) {
            player.displayClientMessage(Component.translatable(
                    "cobble_contests.contest_result.maxed_ranked", pokemonName, getContestTypeString(category)
            ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return;
        }

        CVs stats = CVs.getFromTag(pokemon.getPersistentData().getCompound("CVs"));
        int value = switch (category) {
            case 1 -> stats.getBeauty();
            case 2 -> stats.getCute();
            case 3 -> stats.getSmart();
            case 4 -> stats.getTough();
            default -> stats.getCool();
        };
        int[] legacyThresholds = {5, 40, 100, 175, 245};
        boolean won = value >= legacyThresholds[rank];
        if (won) {
            setRibbon(ribbons, category, rank);
            pokemon.getPersistentData().put("Ribbons", ribbons.saveToNBT());
        }
        player.displayClientMessage(Component.translatable(
                won ? "cobble_contests.contest_result.won_ranked"
                        : "cobble_contests.contest_result.lost_ranked",
                pokemonName, getContestLevelString(rank), getContestTypeString(category)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static void setRibbon(Ribbons ribbons, int category, int rank) {
        switch (category) {
            case 0 -> ribbons.setRankedCool(rank, true);
            case 1 -> ribbons.setRankedBeauty(rank, true);
            case 2 -> ribbons.setRankedCute(rank, true);
            case 3 -> ribbons.setRankedSmart(rank, true);
            case 4 -> ribbons.setRankedTough(rank, true);
            default -> throw new IllegalArgumentException("Unknown contest category " + category);
        }
    }

    public static String getContestTypeString(int contestType) {
        return switch (contestType) {
            case 0 -> "Cool";
            case 1 -> "Beauty";
            case 2 -> "Cute";
            case 3 -> "Smart";
            case 4 -> "Tough";
            default -> "ERROR";
        };
    }

    public static String getContestTypeString1(int contestType) {
        return getContestTypeString(contestType);
    }

    public static String getContestLevelString(int contestLevel) {
        return switch (contestLevel) {
            case 0 -> "Normal";
            case 1 -> "Super";
            case 2 -> "Hyper";
            case 3 -> "Master";
            case 4 -> "Legend";
            default -> "ERROR";
        };
    }

    public record ContestParticipation(UUID id, int pokemonIndex) {
    }

    private record ActiveContest(ContestSession session, Pokemon pokemon) {
    }
}
