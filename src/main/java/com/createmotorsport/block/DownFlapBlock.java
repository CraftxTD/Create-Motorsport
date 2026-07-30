package com.createmotorsport.block;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.placement.PoleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class DownFlapBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
   public static final MapCodec<DownFlapBlock> CODEC = simpleCodec(DownFlapBlock::new);
   public static final BooleanProperty PILLAR = BooleanProperty.create("pillar");
    public int power = 0;

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
            power = level.getBestNeighborSignal(pos);
        } else {
            power = getNeighborFlapSignal(state, level, pos);
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

    public int getNeighborFlapSignal(BlockState state, BlockGetter world, BlockPos pos) {
        int i = 0;
        ArrayList<Direction> sides = new ArrayList<Direction>();
        sides.add(state.getValue(FACING).getClockWise());
        sides.add(state.getValue(FACING).getCounterClockWise());

        for (Direction direction : sides) {
            int j = 0;
            BlockState blockState = world.getBlockState(pos.relative(direction));
            Block block = blockState.getBlock();
            if (block instanceof DownFlapBlock flapBlock) {
                j = flapBlock.getSignal(blockState);
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
    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof DownFlapBlock, state -> state.getValue(AXIS), AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> stack.is(CreateMotorsport.DOWN_FLAP_ITEM);
        }
    }
}
