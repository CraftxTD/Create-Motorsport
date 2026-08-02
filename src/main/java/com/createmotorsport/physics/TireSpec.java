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

    private static final double SLICK_LOAD_FACTOR_LIGHT = 1.15;
    private static final double SLICK_LOAD_FACTOR_HEAVY = 0.80;
    private static final double SLICK_LOAD_FALLOFF = -0.6;

    public static TireSpec fromConfig(boolean front) {
        return new TireSpec(
                Config.PACEJKA_CORNERING_STIFFNESS.getAsDouble(),
                Config.PACEJKA_RFACTOR.getAsDouble(),
                Config.PACEJKA_EFACTOR.getAsDouble(),
                (front ? Config.TIRE_GRIP_FRONT : Config.TIRE_GRIP_REAR).getAsDouble(),
                SLICK_LOAD_FACTOR_LIGHT, SLICK_LOAD_FACTOR_HEAVY, SLICK_LOAD_FALLOFF);
    }
}
