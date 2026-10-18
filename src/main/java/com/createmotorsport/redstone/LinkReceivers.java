package com.createmotorsport.redstone;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.IntUnaryOperator;

public final class LinkReceivers {

    private final BlockEntity owner;
    private final Container slots;
    private final IntUnaryOperator slotA;
    private final IntUnaryOperator slotB;

    private final int[] receivedSignals;
    private final boolean[] registeredLinks;
    private final IRedstoneLinkable[] channelLinks;

    public LinkReceivers(BlockEntity owner, Container slots, int channels, IntUnaryOperator slotA,
                         IntUnaryOperator slotB) {
        this.owner = owner;
        this.slots = slots;
        this.slotA = slotA;
        this.slotB = slotB;
        this.receivedSignals = new int[channels];
        this.registeredLinks = new boolean[channels];
        this.channelLinks = new IRedstoneLinkable[channels];
        for (int i = 0; i < channels; i++) {
            this.channelLinks[i] = new ChannelLink(i);
        }
    }

    public int signal(int channel) {
        return receivedSignals[channel];
    }

    public boolean hasLink(int channel) {
        return !slots.getItem(slotA.applyAsInt(channel)).isEmpty()
                || !slots.getItem(slotB.applyAsInt(channel)).isEmpty();
    }

    public void refresh() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < channelLinks.length; i++) {
            if (registeredLinks[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, channelLinks[i]);
                registeredLinks[i] = false;
            }
            if (hasLink(i)) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, channelLinks[i]);
                registeredLinks[i] = true;
            } else {
                receivedSignals[i] = 0;
            }
        }
    }

    public void remove() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < channelLinks.length; i++) {
            if (registeredLinks[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, channelLinks[i]);
                registeredLinks[i] = false;
            }
        }
    }

    private final class ChannelLink implements IRedstoneLinkable {
        private final int channel;

        private ChannelLink(int channel) {
            this.channel = channel;
        }

        @Override
        public int getTransmittedStrength() {
            return 0;
        }

        @Override
        public void setReceivedStrength(int power) {
            receivedSignals[channel] = power;
        }

        @Override
        public boolean isListening() {
            return hasLink(channel);
        }

        @Override
        public boolean isAlive() {
            Level level = owner.getLevel();
            return level != null && !level.isClientSide && !owner.isRemoved() && level.isLoaded(owner.getBlockPos());
        }

        @Override
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
            return Couple.create(
                    RedstoneLinkNetworkHandler.Frequency.of(slots.getItem(slotA.applyAsInt(channel))),
                    RedstoneLinkNetworkHandler.Frequency.of(slots.getItem(slotB.applyAsInt(channel)))
            );
        }

        @Override
        public BlockPos getLocation() {
            return owner.getBlockPos();
        }
    }
}
