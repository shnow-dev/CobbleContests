package com.raspix.neoforge.cobble_contests.blocks;

import com.raspix.neoforge.cobble_contests.CobbleContestsForge;
import com.raspix.neoforge.cobble_contests.blocks.entity.BlockEntityInit;
import com.raspix.neoforge.cobble_contests.blocks.entity.ContestBlockEntity;
import com.raspix.neoforge.cobble_contests.menus.ContestBoothMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Try to display the nature, -> flavor, evs


public class ContestBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PART = BooleanProperty.create("part");

    private static final VoxelShape SHAPE_AABB = Block.box(0F, 0F, 0F, 16F, 16F, 16F);

    private static final Component TITLE = Component.translatable("container." + CobbleContestsForge.MOD_ID + ".contest_block");

    public ContestBlock(Properties arg) {
        super(arg);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state){
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, true));
        return BlockEntityInit.CONTEST_BLOCK_ENTITY.get().create(pos, state);
    }


    public InteractionResult useWithoutItem(BlockState arg, Level level, BlockPos pos, Player player, BlockHitResult arg6) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {

            /**if (player.isInBattle()) {
                player.sendMessage(lang("pc.inbattle").red());
                return ActionResult.SUCCESS;
            }*/


            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(!(blockEntity instanceof ContestBlockEntity)){
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer) {



                /**try {
                    UUID playerID = player.getUUID();
                    PlayerPartyStore partyStore = Cobblemon.INSTANCE.getStorage().getParty(player.getUUID());
                    PCStore pcStore = Cobblemon.INSTANCE.getStorage().getPC(player.getUUID());
                } catch (NoPokemonStoreException e) {
                    throw new RuntimeException(e);
                }*/

                /**try {
                    PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(((ServerPlayer) player).getUUID());
                    //System.out.println("tried finding pc");
                } catch (NoPokemonStoreException e) {
                    //System.out.println("error finding pc");
                    throw new RuntimeException(e);
                }*/

                //PacketHandler.INSTANCE.sendToServer(new SSendPartyPacket(player.getUUID(), pos));
                //OpenPCPacket(pc.uuid).sendToPlayer(player);
                MenuProvider menuP = blockEntity.getBlockState().getMenuProvider(level, pos);
                if(menuP == null){
                    System.out.println("null provider");
                }
                player.openMenu((ContestBlockEntity)blockEntity, pos);
                //NetworkHooks.openScreen((ServerPlayer)player, (MenuProvider) blockEntity, pos);
                //PacketHandler.sendToServer(new SSendPartyPacket(player.getUUID()));
                //System.out.println("hi1");

            }
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, playerInv, arg4) -> {
            return new ContestBoothMenu(id, playerInv, level.getBlockEntity(pos));
        }, TITLE);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return super.getExplosionResistance(state, level, pos, explosion);
    }


    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        //VoxelShape shape = Block.box(0, 0, 0, 0, 0, 0);
        //shape = Shapes.join(shape, SHAPE_AABB, BooleanOp.OR);
        VoxelShape shape = SHAPE_AABB;

        return shape;
    }

    public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape shape = SHAPE_AABB;//Block.box(0, 0, 0, 0, 0, 0);
        return getCollisionShape(state, pLevel, pPos, pContext);
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }


    @Override
    public @NotNull RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, PART);
    }

    @javax.annotation.Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getHorizontalDirection().getOpposite();
        BlockPos blockpos = pContext.getClickedPos().above();
        Level level = pContext.getLevel();
        return level.getBlockState(blockpos).canBeReplaced(pContext) && level.getWorldBorder().isWithinBounds(blockpos)
                ? this.defaultBlockState().setValue(FACING, direction) : null;
        //return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @javax.annotation.Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide && pState.getValue(PART) == true) {
            Direction dir = pState.getValue(FACING);
            addPartBlock(pLevel, pPos.above(), pState, dir);
        }
    }

    private void addPartBlock(Level pLevel, BlockPos blockpos, BlockState pState, Direction dir){
        pLevel.setBlock(blockpos, pState.setValue(PART, false).setValue(FACING, dir), 3);
        pLevel.blockUpdated(blockpos, Blocks.AIR);
        pState.updateNeighbourShapes(pLevel, blockpos, 3);
    }

    private void removePartBlock(Level pLevel, BlockPos blockpos){
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (blockstate.is(this)) {
            pLevel.removeBlock(blockpos, false);
        }
    }

    public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pLevel.isClientSide) {
            if (!pState.getValue(PART)){
                removePartBlock(pLevel, pPos.below());
            } else if (pState.getValue(PART)){
                removePartBlock(pLevel, pPos.above());
            }
        }

        return super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

}
