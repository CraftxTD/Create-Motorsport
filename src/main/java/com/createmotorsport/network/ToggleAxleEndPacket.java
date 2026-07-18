package com.createmotorsport.network;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server; toggle suspension between front and rear axle at (pos)
public record ToggleAxleEndPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToggleAxleEndPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "toggle_axle_end"));

    public static final StreamCodec<FriendlyByteBuf, ToggleAxleEndPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleAxleEndPacket::pos,
            ToggleAxleEndPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleAxleEndPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof SuspensionBlockEntity suspension) {
                suspension.toggleAxleEnd();
            }
        });
    }
}
