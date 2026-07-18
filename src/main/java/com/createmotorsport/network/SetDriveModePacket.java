package com.createmotorsport.network;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server; cycle the drive layout (FWD/RWD/AWD) on the steering wheel at (pos)
public record SetDriveModePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<SetDriveModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "set_drive_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetDriveModePacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetDriveModePacket::pos,
            SetDriveModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetDriveModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof SteeringWheelBlockEntity wheel) {
                wheel.cycleDriveMode();
            }
        });
    }
}
