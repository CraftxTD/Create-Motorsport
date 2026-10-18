package com.createmotorsport.redstone;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;


public final class LinkTransmitters {

    private final BlockEntity owner;
    private final IntFunction<ItemStack> item;
    private final IntUnaryOperator slotA;
    private final IntUnaryOperator slotB;

    private final boolean[] registered;
    private final Transmitter[] transmitters;

    public LinkTransmitters(BlockEntity owner, IntFunction<ItemStack> item, int channels, IntUnaryOperator slotA,
                            IntUnaryOperator slotB) {
        this.owner = owner;
        this.item = item;
        this.slotA = slotA;
        this.slotB = slotB;
        this.registered = new boolean[channels];
        this.transmitters = new Transmitter[channels];
        for (int i = 0; i < channels; i++) {
            this.transmitters[i] = new Transmitter(i);
        }
    }

    public boolean hasLink(int channel) {
        return !item.apply(slotA.applyAsInt(channel)).isEmpty()
                || !item.apply(slotB.applyAsInt(channel)).isEmpty();
    }

    public void setPower(int channel, int power) {
        this.transmitters[channel].setPower(power);
    }

    public void refresh() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < transmitters.length; i++) {
            if (registered[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, transmitters[i]);
                registered[i] = false;
            }
            if (hasLink(i)) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, transmitters[i]);
                registered[i] = true;
            }
        }
    }

    public void remove() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < transmitters.length; i++) {
            if (registered[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, transmitters[i]);
                registered[i] = false;
            }
        }
    }

    private final class Transmitter implements IRedstoneLinkable {
        private final int channel;
        private int power;

        private Transmitter(int channel) {
            this.channel = channel;
        }

        private void setPower(int newPower) {
            int clamped = Math.max(0, Math.min(15, newPower));
            if (clamped == power) {
                return;
            }
            power = clamped;
            Level level = owner.getLevel();
            if (registered[channel] && level != null && !level.isClientSide) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, this);
            }
        }

        @Override
        public int getTransmittedStrength() {
            return power;
        }

        @Override
        public void setReceivedStrength(int received) {
        }

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public boolean isAlive() {
            Level level = owner.getLevel();
            return level != null && !level.isClientSide && !owner.isRemoved() && level.isLoaded(owner.getBlockPos());
        }

        @Override
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
            return Couple.create(
                    RedstoneLinkNetworkHandler.Frequency.of(item.apply(slotA.applyAsInt(channel))),
                    RedstoneLinkNetworkHandler.Frequency.of(item.apply(slotB.applyAsInt(channel)))
            );
        }

        @Override
        public BlockPos getLocation() {
            return owner.getBlockPos();
        }
    }
}
