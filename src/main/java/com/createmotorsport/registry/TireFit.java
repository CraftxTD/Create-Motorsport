package com.createmotorsport.registry;

import com.createmotorsport.CreateMotorsport;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.world.item.ItemStack;

public record TireFit(double designMassKg, float radius, float verticalDeflection, float verticalDampingRatio,
                      float mass, float width) {

    public static TireFit of(TireEntry entry) {
        return new TireFit(entry.designMassKg(), entry.radius(), entry.verticalDeflection(),
                entry.verticalDampingRatio(), entry.mass(), entry.width());
    }

    public static TireFit of(ItemStack stack) {
        float load = stack.getOrDefault(CreateMotorsport.TIRE_DESIGN_LOAD, 0.0f);
        double kg = Math.round(TireEntry.designMassKg(load) * 1000.0) / 1000.0;
        TireLike art = stack.get(OffroadDataComponents.TIRE);
        return new TireFit(kg, art != null ? art.radius() : 1.0f,
                stack.getOrDefault(CreateMotorsport.TIRE_VERTICAL_DEFLECTION, TireEntry.DEFAULT_VERTICAL_DEFLECTION),
                stack.getOrDefault(CreateMotorsport.TIRE_VERTICAL_DAMPING, TireEntry.DEFAULT_VERTICAL_DAMPING),
                stack.getOrDefault(CreateMotorsport.TIRE_MASS, 0.0f),
                stack.getOrDefault(CreateMotorsport.TIRE_WIDTH, 0.0f));
    }

    public void fit(ItemStack stack) {
        stack.set(CreateMotorsport.TIRE_DESIGN_LOAD, TireEntry.designLoad(designMassKg));
        stack.set(CreateMotorsport.TIRE_VERTICAL_DEFLECTION, verticalDeflection);
        stack.set(CreateMotorsport.TIRE_VERTICAL_DAMPING, verticalDampingRatio);
        stack.set(CreateMotorsport.TIRE_MASS, mass);
        stack.set(CreateMotorsport.TIRE_WIDTH, width);
        TireLike art = stack.get(OffroadDataComponents.TIRE);
        if (art != null && art.radius() != radius) {
            stack.set(OffroadDataComponents.TIRE,
                    new TireLike(radius, art.rotation(), art.offset(), art.model(), art.minimumFriction()));
        }
    }
}
