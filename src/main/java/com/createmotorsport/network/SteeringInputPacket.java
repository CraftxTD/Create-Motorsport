package com.createmotorsport.network;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server; the bitmask for the driver's current pressed control, at (pos). One bit per SteeringWheelBlockEntity.SteeringControl
public record SteeringInputPacket(BlockPos pos, int mask) implements CustomPacketPayload {
    public static final Type<SteeringInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "steering_input"));

    public static final StreamCodec<FriendlyByteBuf, SteeringInputPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SteeringInputPacket::pos,
            ByteBufCodecs.VAR_INT, SteeringInputPacket::mask,
            SteeringInputPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SteeringInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(packet.pos()) instanceof SteeringWheelBlockEntity wheel)) {
                return;
            }
            wheel.setInput(context.player(), packet.mask());
        });
    }
}
