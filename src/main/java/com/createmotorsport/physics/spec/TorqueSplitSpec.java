package com.createmotorsport.physics.spec;


public record TorqueSplitSpec(double frontBias, double lock, double coupling) {

    public static final TorqueSplitSpec DEFAULT = new TorqueSplitSpec(0.5, 0.5, 50.0);

    public static final TorqueSplitSpec OPEN = new TorqueSplitSpec(0.5, 0.0, 0.0);
}
