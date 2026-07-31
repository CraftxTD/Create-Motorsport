package com.createmotorsport.block.entity;

import com.createmotorsport.CreateMotorsport;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DownFlapBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    int state = 0;
    int lastChange;
    LerpedFloat clientState;

    public DownFlapBlockEntity(BlockPos pos, BlockState state) {
        super(CreateMotorsport.DOWN_FLAP_BLOCK_ENTITY.get(), pos, state);
        clientState = LerpedFloat.linear();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("State", state);
        compound.putInt("ChangeTimer", lastChange);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        state = compound.getInt("State");
        lastChange = compound.getInt("ChangeTimer");
        clientState.chase(state, 0.2f, LerpedFloat.Chaser.EXP);
        super.read(compound, registries, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void changeState(int n) {
        state = n;
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
