package com.createmotorsport.client;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.WheelSide;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SuspensionModel extends GeoModel<SuspensionBlockEntity> {


    private record BoneDelta(String bone, double rotX, double rotY, double rotZ, double posX, double posY, double posZ) {}

    // Left side taken from suspension.animation.json left_suspension at t = 1.0
    private static final BoneDelta[] LEFT = {
            new BoneDelta("LeftUpperWishbone", 30.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("LeftLowerWishbone", 27.5, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("SteeringShaftLeft", 30.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("group26", 0.0, 0.0, 0.0, -1.2, 0.0, 0.0),
            new BoneDelta("group27", 0.0, 35.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("group28", 27.5, 0.0, 0.0, 0.0, 7.0, 0.0),
            new BoneDelta("LeftAttachment", 0.0, 0.0, 0.0, 0.0, 7.0, 0.0),
            new BoneDelta("LeftShaftRotation", 35.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("bone3", 0.0, 35.0, 0.0, 0.0, 0.0, 0.0),
    };

    // Right side, from "right_suspension" at t = 1.0
    private static final BoneDelta[] RIGHT = {
            new BoneDelta("RightUpperWishbone", -32.5, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("RightLowerWishbone", -27.5, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("RightSteer", -30.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("group17", 0.0, 0.0, 0.0, -1.2, 0.0, 0.0),
            new BoneDelta("group10", 0.0, -35.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("group13", -27.5, 0.0, 0.0, 0.0, 7.0, 0.0),
            new BoneDelta("RightAttachment", 0.0, 0.0, 0.0, 0.0, 7.0, 0.0),
            new BoneDelta("ShaftRight", -37.5, 0.0, 0.0, 0.0, 0.0, 0.0),
            new BoneDelta("bone4", 0.0, -35.0, 0.0, 0.0, 0.0, 0.0),
    };

    @Override
    public ResourceLocation getModelResource(SuspensionBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "geo/suspension.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SuspensionBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "textures/block/suspension.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SuspensionBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "animations/suspension.animation.json");
    }
    
    private static final float SPIN_SIGN = 1.0F;


    private static final float STEER_RACK_PX_PER_RAD = (float) (3.0 / Math.toRadians(30.0));


    @Override
    public void setCustomAnimations(SuspensionBlockEntity animatable, long instanceId, AnimationState<SuspensionBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        float partialTick = animationState.getPartialTick();

        applyPose(LEFT, animatable.getLerpedRaise(WheelSide.LEFT, partialTick));
        applyPose(RIGHT, animatable.getLerpedRaise(WheelSide.RIGHT, partialTick));

        applySpin(animatable.getLerpedAngle(WheelSide.LEFT, partialTick),
                animatable.getLerpedAngle(WheelSide.RIGHT, partialTick));

        applySteer(animatable, partialTick);
    }

    private void applySpin(float angleLeft, float angleRight) {
        spinZ("LeftWheel", angleLeft);
        spinZ("bone", angleLeft);             // left wheel hub spins with tire
        spinZ("bone5", angleLeft);            // left outer half-shaft
        spinZ("RightWheel", angleRight);
        spinZ("bone2", angleRight);           // right wheel hub spinny
        spinZ("ShaftRightRotation", angleRight); // right outer half-shaft
        spinZ("MiddleShaft", 0.5F * (angleLeft + angleRight));
        GeoBone prop = getAnimationProcessor().getBone("MainShaft");
        if (prop != null) {
            prop.setRotX(SPIN_SIGN * 0.5F * (angleLeft + angleRight));
        }
    }

    private void spinZ(String bone, float angle) {
        GeoBone b = getAnimationProcessor().getBone(bone);
        if (b != null) {
            b.setRotZ(SPIN_SIGN * angle);
        }
    }


    private void applySteer(SuspensionBlockEntity animatable, float partialTick) {
        yawUpright("LeftAttachment", animatable.getLerpedSteer(WheelSide.LEFT, partialTick));
        yawUpright("RightAttachment", animatable.getLerpedSteer(WheelSide.RIGHT, partialTick));
        float rack = STEER_RACK_PX_PER_RAD * animatable.getLerpedSteer(partialTick);
        slideRack("RightSteer", rack);
        slideRack("MiddleSteer", rack);
        slideRack("SteeringShaftLeft", rack);
    }

    private void yawUpright(String bone, float steer) {
        GeoBone b = getAnimationProcessor().getBone(bone);
        if (b != null) {
            b.setRotY(b.getInitialSnapshot().getRotY() + steer);
        }
    }

    private void slideRack(String bone, float rackPx) {
        GeoBone b = getAnimationProcessor().getBone(bone);
        if (b != null) {
            b.setPosZ(rackPx);
        }
    }


    private void applyPose(BoneDelta[] deltas, float raise) {
        for (BoneDelta d : deltas) {
            GeoBone bone = getAnimationProcessor().getBone(d.bone());
            if (bone == null) {
                continue;
            }
            var rest = bone.getInitialSnapshot();
            if (d.rotX() != 0.0 || d.rotY() != 0.0 || d.rotZ() != 0.0) {
                bone.setRotX(rest.getRotX() + (float) Math.toRadians(-d.rotX() * raise));
                bone.setRotY(rest.getRotY() + (float) Math.toRadians(-d.rotY() * raise));
                bone.setRotZ(rest.getRotZ() + (float) Math.toRadians(d.rotZ() * raise));
            }
            if (d.posX() != 0.0 || d.posY() != 0.0 || d.posZ() != 0.0) {
                bone.setPosX((float) (d.posX() * raise));
                bone.setPosY((float) (d.posY() * raise));
                bone.setPosZ((float) (d.posZ() * raise));
            }
        }
    }
}
