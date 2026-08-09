package com.createmotorsport.block;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.placement.PoleHelper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class DownFlapBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
   public static final MapCodec<DownFlapBlock> CODEC = simpleCodec(DownFlapBlock::new);
   public static final BooleanProperty PILLAR = BooleanProperty.create("pillar");
    public int power = 0;

    private static final int placementHelperId = PlacementHelpers.register(new DownFlapBlock.PlacementHelper());

    public DownFlapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PILLAR, false));
    }

    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DownFlapBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(stack))
                return placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, ((BlockItem) stack.getItem()), player, hand, hitResult);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState newState = state.setValue(PILLAR, !state.getValue(PILLAR));
        level.setBlockAndUpdate(pos, newState);

        // TODO: add diff sound
        if (level.getBlockState(pos) != state) {
            IWrenchable.playRotateSound(level, pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        DownFlapBlockEntity be = (DownFlapBlockEntity) level.getBlockEntity(pos);
        if (be == null) { return; }
        if (state.getValue(PILLAR)) {
            int temp = level.getBestNeighborSignal(pos);
            power = (temp == -1) ? power : temp;
        } else {
            power = getNeighborFlapSignal(state, level, pos, fromPos);
        }
        be.changeState(power);

        if (!level.getBlockTicks()
                .willTickThisTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    // Overload for getting nearby flap signal
    public int getSignal(BlockState state) {
        if (!state.getValue(PILLAR)) {
            return 0;
        }
        return power;
    }

    private int getNeighborFlapSignal(BlockState state, BlockGetter world, BlockPos pos, BlockPos fromPos) {
        Block fromBlock = world.getBlockState(fromPos).getBlock();
        if (!(fromBlock instanceof DownFlapBlock)) {
            return -1;
        }

        int i = 0;
        ArrayList<Direction> sides = new ArrayList<>();
        sides.add(state.getValue(FACING).getClockWise());
        sides.add(state.getValue(FACING).getCounterClockWise());

        for (Direction direction : sides) {
            int j;
            BlockState blockState = world.getBlockState(pos.relative(direction));
            Block block = blockState.getBlock();
            if (block instanceof DownFlapBlock flapBlock) {
                j = flapBlock.getSignal(blockState);
            } else {
                continue;
            }
            if (j >= 15) {
                return 15;
            }

            if (j > i) {
                i = j;
            }
        }

        return i;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        if (state.getValue(PILLAR)) {
            return side != null;
        }
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, PILLAR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }


    @MethodsReturnNonnullByDefault
    private static class PlacementHelper extends PoleHelper<Direction> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof DownFlapBlock, state -> state.getValue(FACING).getClockWise().getAxis(), FACING);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> stack.is(CreateMotorsport.DOWN_FLAP_ITEM);
        }
    }
}
