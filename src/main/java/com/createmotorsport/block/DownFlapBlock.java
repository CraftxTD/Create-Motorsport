package com.createmotorsport.block;

import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DownFlapBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
   public static final MapCodec<DownFlapBlock> CODEC = simpleCodec(DownFlapBlock::new);
   private static final Component CONTAINER_TITLE = Component.translatable("container.createmotorsport.down_flap");

    public DownFlapBlock(Properties properties) {
        super(properties);
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
}
