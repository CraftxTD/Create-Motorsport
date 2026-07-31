package com.createmotorsport.physics;

import com.createmotorsport.Config;
import net.minecraft.util.Mth;

/**
 * Tire tier's grip character, parametised the Speed-dreams way (simuv2.1 wheel.cpp SimWheelConfig)
 *
 *   corneringStiffness (Ca) -> stiffness factor B, sets the slip % at which the tire makes maximum force
 *                              Higher Ca -> peak at a lower slip = less forgiving launches
 *   rFactor              -> the shape factor C, how the curve behaves past the peak
 *   eFactor            -> the curvature E (the fall-off past the peak).
 *
 * grip is the base rubber multiplier, and the load-factor values handle load sensitivity (grip vs load)
 */
public record TireSpec(
        double corneringStiffness,
        double rFactor,
        double eFactor,
        double grip,
        double loadFactorLight, // grip multiplier as load -> 0 (Speed-dreams lfMax)
        double loadFactorHeavy, // grip multiplier as load -> large (lfMin)
        double loadFalloff      // how fast grip drops as the tire is loaded (lfK)
) {

    // Pacejka B/C/E done the same as Speed dreams: C = 2 - asin(RFactor)*2/PI, B = Ca/C, E = EFactor
    public double pacejkaC() {
        return 2.0 - Math.asin(Mth.clamp(rFactor, 0.1, 1.0)) * 2.0 / Math.PI;
    }

    public double pacejkaB() {
        return corneringStiffness / pacejkaC();
    }

    public double pacejkaE() {
        return Math.min(1.0, eFactor);
    }


    // Default tier with numbers that would calculate to the old ones (B=10, C=1.9, E=0.85). So, replace them with
    //(Ca 30, rFactor 0.8, eFactor 0.7) to be closer to speed dreams
    // per axle tier for grip is a simple oversteering balance for now
    public static final TireSpec RACING_SLICK_FRONT = new TireSpec(
            Config.PACEJKA_CORNERING_STIFFNESS.getAsDouble(), Config.PACEJKA_RFACTOR.getAsDouble(), Config.PACEJKA_EFACTOR.getAsDouble(), Config.TIRE_GRIP_FRONT.getAsDouble(), 1.15, 0.80, -0.6);
    public static final TireSpec RACING_SLICK_REAR = new TireSpec(
            Config.PACEJKA_CORNERING_STIFFNESS.getAsDouble(), Config.PACEJKA_RFACTOR.getAsDouble(), Config.PACEJKA_EFACTOR.getAsDouble(), Config.TIRE_GRIP_REAR.getAsDouble(), 1.15, 0.80, -0.6);
}
