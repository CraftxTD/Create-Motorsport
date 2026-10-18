package com.createmotorsport.physics.starter;

import com.createmotorsport.physics.Starter;

public final class ElectricStarter implements Starter {

    private final double stallTorque;
    private final double freeOmega;

    public ElectricStarter(double stallTorque, double freeOmega) {
        this.stallTorque = Math.max(0.0, stallTorque);
        this.freeOmega = freeOmega;
    }

    @Override
    public double torque(double omega) {
        if (!(this.freeOmega > 0.0)) {
            return 0.0;
        }
        return this.stallTorque * Math.max(0.0, 1.0 - omega / this.freeOmega);
    }
}
