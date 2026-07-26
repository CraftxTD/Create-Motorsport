package com.createmotorsport.block;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.WheelSide;
import com.createmotorsport.menu.SuspensionMenu;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SuspensionBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<SuspensionBlock> CODEC = simpleCodec(SuspensionBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 2.0, 0.0, 16.0, 14.0, 16.0);
    private static final Component STEERING_TITLE = Component.translatable("container.createmotorsport.suspension");

    public SuspensionBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.get(OffroadDataComponents.TIRE) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof SuspensionBlockEntity suspension)) {
            return ItemInteractionResult.FAIL;
        }

        WheelSide side = sideFromHit(state, pos, hitResult);
        if (!level.isClientSide) {
            if (!suspension.installTire(side, stack)) {
                return ItemInteractionResult.FAIL;
            }
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SuspensionBlockEntity suspension)) {
            return InteractionResult.PASS;
        }

        // Sneak-click to remove tire, normal click to open menu for redstone links
        if (player.isShiftKeyDown()) {
            WheelSide side = sideFromHit(state, pos, hitResult);
            if (!level.isClientSide) {
                ItemStack removed = suspension.removeTire(side);
                if (removed.isEmpty()) {
                    return InteractionResult.PASS;
                }
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SuspensionBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, menuPlayer) -> new SuspensionMenu(containerId, playerInventory, be),
                    STEERING_TITLE
            ), pos); // writes position so the client menu can target this suspension
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SuspensionBlockEntity suspension) {
            for (ItemStack stack : suspension.getTires()) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            for (int slot = 0; slot < suspension.getControls().getContainerSize(); slot++) {
                ItemStack stack = suspension.getControls().getItem(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static WheelSide sideFromHit(BlockState state, BlockPos pos, BlockHitResult hitResult) {
        Direction facing = state.getValue(FACING);
        Direction left = facing.getCounterClockWise();
        double localX = hitResult.getLocation().x - pos.getX() - 0.5;
        double localZ = hitResult.getLocation().z - pos.getZ() - 0.5;
        double towardsLeft = localX * left.getStepX() + localZ * left.getStepZ();
        return towardsLeft >= 0.0 ? WheelSide.LEFT : WheelSide.RIGHT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuspensionBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == CreateMotorsport.SUSPENSION_BLOCK_ENTITY.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> ((SuspensionBlockEntity) blockEntity).tick()
                : null;
    }
}
