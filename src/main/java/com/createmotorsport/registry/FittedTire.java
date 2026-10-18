package com.createmotorsport.registry;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.physics.vehicle.Corner;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.world.item.ItemStack;

public final class FittedTire {

    private FittedTire() {
    }

    public static double mass(ItemStack stack, double carMass) {
        return Corner.wheelMass(stack.getOrDefault(CreateMotorsport.TIRE_MASS, 0.0f), carMass);
    }

    public static double inertia(ItemStack stack, double carMass, double noTireRadius) {
        TireLike tire = stack.get(OffroadDataComponents.TIRE);
        double radius = tire != null ? tire.radius() : noTireRadius;
        return Corner.wheelInertia(radius, mass(stack, carMass));
    }

    public static float foreignDesignLoad(double sprungMassPerWheel, double gravity) {
        return (float) (sprungMassPerWheel * gravity);
    }

    public static float designLoad(ItemStack stack, double sprungMassPerWheel, double gravity) {
        float designLoad = stack.getOrDefault(CreateMotorsport.TIRE_DESIGN_LOAD, 0.0f);
        return designLoad > 0.0f ? designLoad : foreignDesignLoad(sprungMassPerWheel, gravity);
    }

    public static double verticalRate(ItemStack stack, double radius, double sprungMassPerWheel, double gravity) {
        float designLoad = stack.getOrDefault(CreateMotorsport.TIRE_DESIGN_LOAD, 0.0f);
        float deflection = stack.getOrDefault(CreateMotorsport.TIRE_VERTICAL_DEFLECTION,
                TireEntry.DEFAULT_VERTICAL_DEFLECTION);
        if (designLoad <= 0.0f) {
            return TireEntry.verticalRate(foreignDesignLoad(sprungMassPerWheel, gravity), radius,
                    TireEntry.DEFAULT_VERTICAL_DEFLECTION);
        }
        return TireEntry.verticalRate(Math.max(designLoad, sprungMassPerWheel * gravity), radius, deflection);
    }
}
