package com.createmotorsport.block;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class DownFlapBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
   public static final MapCodec<DownFlapBlock> CODEC = simpleCodec(DownFlapBlock::new);
   public static final BooleanProperty PILLAR = BooleanProperty.create("pillar");
   public static final IntegerProperty FLAP_POWER = IntegerProperty.create("flap_power", 0, 15);

    private static final int placementHelperId = PlacementHelpers.register(new DownFlapBlock.PlacementHelper());

    public DownFlapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PILLAR, false).setValue(FLAP_POWER, 0));
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

        Direction facing = state.getValue(FACING);
        DownFlapBlockEntity be = (DownFlapBlockEntity) level.getBlockEntity(pos);
        int power = 0;

        if (be == null) { return; }
        if (state.getValue(PILLAR)) {
            power = level.getBestNeighborSignal(pos);
        } else {
            Block fromBlock = level.getBlockState(fromPos).getBlock();
            if (fromBlock instanceof DownFlapBlock) {
                int[] temp = getNeighborFlapSignal(state, level, pos);
                power = (temp[1] > -1) ? temp[1] : temp[0];
            }
        }
        if (state.getValue(FLAP_POWER) != power) {
            level.setBlockAndUpdate(pos, state.setValue(FLAP_POWER, power));
            be.changeState(power);
        }

        if (!level.getBlockTicks()
                .willTickThisTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    private int[] getNeighborFlapSignal(BlockState state, Level level, BlockPos pos) {
        int[] nums = new int[2];
        // regular
        nums[0] = 0;
        // pillar
        nums[1] = -1;
        ArrayList<Direction> sides = new ArrayList<>();
        sides.add(state.getValue(FACING).getClockWise());
        sides.add(state.getValue(FACING).getCounterClockWise());

        int j;
        for (Direction direction : sides) {
            Block block = level.getBlockState(pos.relative(direction)).getBlock();
            if (block instanceof DownFlapBlock flap) {
                j = level.getBlockState(pos.relative(direction)).getValue(FLAP_POWER);
            } else {
                continue;
            }
            if (level.getBlockState(pos.relative(direction)).getValue(PILLAR) && j > nums[1]) {
                nums[1] = j;
                continue;
            }

            if (j > nums[0]) {
                nums[0] = j;
            }
        }
        return nums;
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
        builder.add(HORIZONTAL_FACING, PILLAR, FLAP_POWER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == CreateMotorsport.DOWN_FLAP_BLOCK_ENTITY.get()
                ? (tickerLevel, pos, tickerState, be) -> ((DownFlapBlockEntity) be).tick()
                : null;
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
