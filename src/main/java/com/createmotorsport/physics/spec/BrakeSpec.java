package com.createmotorsport.physics.spec;

public record BrakeSpec(double strength, boolean abs, double absSlipThreshold,
                        double absDumpRate, double absBuildRate, double absMinSpeed,
                        double absFloor, double applyTime) {

    public static final BrakeSpec DEFAULT =
            new BrakeSpec(2000.0, true, 0.15, 10.0, 3.0, 1.5, 0.05, 0.2);
}
