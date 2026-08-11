package com.createmotorsport.block;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.placement.PoleHelper;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class DownFlapBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable, BlockSubLevelLiftProvider {
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

        if (state.getValue(PILLAR)) {
            Direction facing = state.getValue(FACING);
            DownFlapBlockEntity be = (DownFlapBlockEntity) level.getBlockEntity(pos);
            if (be == null) { return; }
            int power = 0;

            if (state.getValue(PILLAR)) {
                power = level.getBestNeighborSignal(pos);
            }

            if (state.getValue(FLAP_POWER) != power && power > -1) {
                state = state.setValue(FLAP_POWER, power);
                level.setBlock(pos, state, 2);
                be.changeState(power);
                level.sendBlockUpdated(pos, state, state, 2);
                updateNeighborFlap(level, pos, facing.getClockWise(), power);
                updateNeighborFlap(level, pos, facing.getCounterClockWise(), power);
            }
        }

        if (!level.getBlockTicks().willTickThisTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    public void updateNeighborFlap(Level level, BlockPos pos, Direction direction, int power) {
        Stack<BlockPos> flapStack = new Stack<>();
        BlockPos nextPos = pos.relative(direction);

        // clockwise or counter-clockwise
        while (level.getBlockState(nextPos).getBlock() instanceof DownFlapBlock) {
            if (level.getBlockState(nextPos).getValue(PILLAR)) {
                power = Math.max(level.getBlockState(nextPos).getValue(FLAP_POWER), power);
                break;
            }
            flapStack.push(nextPos);
            nextPos = nextPos.relative(direction);
        }
        while (!flapStack.empty()) {
            nextPos = flapStack.pop();
            if (level.getBlockEntity(nextPos) instanceof DownFlapBlockEntity be) {
                BlockState state = level.getBlockState(nextPos).setValue(FLAP_POWER, power);
                level.setBlock(nextPos, state, 4);
                be.changeState(power);
                level.sendBlockUpdated(nextPos, state, state, 2);
            }
        }
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
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
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

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public void sable$contributeLiftAndDrag(BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel subLevel, @NotNull Pose3d localPose, double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity, Vector3d linearImpulse, Vector3d angularImpulse, @Nullable BlockSubLevelLiftProvider.LiftProviderGroup group) {
        double z = ctx.state().getValue(FLAP_POWER) * -60;
        ctx.dir().add(0, 0, z);
        BlockSubLevelLiftProvider.super.sable$contributeLiftAndDrag(ctx, subLevel, localPose, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, group);
    }
}
