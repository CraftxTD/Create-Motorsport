package com.createmotorsport.block.entity;

import com.createmotorsport.CreateMotorsport;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DownFlapBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public DownFlapBlockEntity(BlockPos pos, BlockState state) {
        super(CreateMotorsport.DOWN_FLAP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }
}
