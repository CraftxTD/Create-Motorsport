package com.createmotorsport.physics.spec;


public record TractionControlSpec(double targetSlip, double minThrottle, double cutRate, double buildRate,
                                  double dragMaxThrottle) {

    public static final TractionControlSpec DEFAULT = new TractionControlSpec(0.14, 0.02, 10.0, 8.0, 0.3);
}
