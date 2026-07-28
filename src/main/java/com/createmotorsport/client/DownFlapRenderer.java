package com.createmotorsport.client;

import com.createmotorsport.block.entity.DownFlapBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DownFlapRenderer extends SafeBlockEntityRenderer<DownFlapBlockEntity> {
   public DownFlapRenderer() {
   }

    @Override
    protected void renderSafe(DownFlapBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {

    }
}
