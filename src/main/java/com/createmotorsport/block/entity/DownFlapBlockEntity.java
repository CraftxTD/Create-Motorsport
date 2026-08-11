package com.createmotorsport.block.entity;

import com.createmotorsport.CreateMotorsport;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class DownFlapBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, BlockSubLevelLiftProvider {
    int state = 0;
    public LerpedFloat clientState;

    public DownFlapBlockEntity(BlockPos pos, BlockState state) {
        super(CreateMotorsport.DOWN_FLAP_BLOCK_ENTITY.get(), pos, state);
        clientState = LerpedFloat.linear();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt("Flap State", state);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        state = tag.getInt("Flap State");
        clientState.chase(state, 0.5f, Chaser.EXP);
        super.read(tag, registries, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void changeState(int n) {
        state = n;
        sendData();
    }

    public int getState() { return this.state; }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            clientState.tickChaser();
        }
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.translate("tooltip.flapState", this.state).forGoggles(tooltip);
        return true;
    }

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return ((Direction) Direction.DOWN);
    }

    @Override
    public void sable$contributeLiftAndDrag(LiftProviderContext ctx, ServerSubLevel subLevel, @NotNull Pose3d localPose, double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity, Vector3d linearImpulse, Vector3d angularImpulse, @Nullable LiftProviderGroup group) {
        double x = state * -6;
        ctx.dir().add(x, 0, 0);
        BlockSubLevelLiftProvider.super.sable$contributeLiftAndDrag(ctx, subLevel, localPose, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, group);
    }
}
