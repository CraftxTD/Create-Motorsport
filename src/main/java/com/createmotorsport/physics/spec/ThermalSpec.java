package com.createmotorsport.physics.spec;

public record ThermalSpec(
        double optimalTempC,
        double coldGripFactor,
        double heatingRate,
        double coolingRate
) {

    public static final ThermalSpec DEFAULT = new ThermalSpec(70.0, 0.6, 0.005, 0.02);
}
