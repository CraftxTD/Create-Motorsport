package com.createmotorsport.network;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server; raise or lower the suspension ride-height lift at (pos) by one click
public record AdjustLiftPacket(BlockPos pos, int delta) implements CustomPacketPayload {
    public static final Type<AdjustLiftPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "adjust_lift"));

    public static final StreamCodec<FriendlyByteBuf, AdjustLiftPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AdjustLiftPacket::pos,
            ByteBufCodecs.INT, AdjustLiftPacket::delta,
            AdjustLiftPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdjustLiftPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof SuspensionBlockEntity suspension) {
                suspension.adjustLift(packet.delta() > 0 ? 1 : -1);
            }
        });
    }
}
