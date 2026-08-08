package com.createmotorsport.client;

import com.createmotorsport.block.DownFlapBlock;
import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class DownFlapRenderer extends SafeBlockEntityRenderer<DownFlapBlockEntity> {
   public DownFlapRenderer() {
   }

    @Override
    protected void renderSafe(DownFlapBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

       if (VisualizationManager.supportsVisualization(be.getLevel())) return;

       BlockState flapState = be.getBlockState();
       float state = be.clientState.getValue(partialTicks);

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

       // Flap
        SuperByteBuffer flap = CachedBuffers.partial(MotorsportPartialModels.DOWNFLAP_WING, flapState);
        float angle = (float) ((state * 6) / 180 * Math.PI);
        transform(flap, flapState).translate(1 / 2f, 1 / 16f, 1 / 2f)
                .rotate(angle, Direction.EAST)
                .translate(-1 / 2f, -1 / 16f, -1 / 2f);
        flap.light(light)
                .renderInto(ms, vb);
    }

    private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState flapState) {
       float rY = AngleHelper.horizontalAngle(flapState.getValue(DownFlapBlock.FACING));
       buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
       return buffer;
    }
}
