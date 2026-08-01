package com.createmotorsport.block.entity;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.DownFlapBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DownFlapBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, BlockSubLevelLiftProvider {
    int state = 0;
    LerpedFloat clientState;

    public DownFlapBlockEntity(BlockPos pos, BlockState state) {
        super(CreateMotorsport.DOWN_FLAP_BLOCK_ENTITY.get(), pos, state);
        clientState = LerpedFloat.linear();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("State", state);


    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        state = tag.getInt("State");
        clientState.chase(state, 0.2f, LerpedFloat.Chaser.EXP);
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
        if (level.isClientSide)
            clientState.tickChaser();
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }

    @Override
    public @NotNull Direction sable$getNormal(BlockState blockState) {
        return null;
    }
}
