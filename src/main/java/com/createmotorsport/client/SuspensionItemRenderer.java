package com.createmotorsport.client;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


public class SuspensionItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float ITEM_SCALE = 0.35F;
    private static final float ITEM_LIFT = 0.1F;

    private final SuspensionRenderer delegate = new SuspensionRenderer();
    private SuspensionBlockEntity dummy;

    private static SuspensionItemRenderer instance;

    public static SuspensionItemRenderer getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new SuspensionItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    public SuspensionItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (dummy == null) {
            dummy = new SuspensionBlockEntity(BlockPos.ZERO, CreateMotorsport.SUSPENSION.get().defaultBlockState());
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + ITEM_LIFT, 0.5);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);

        delegate.render(dummy, 0.0F, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
