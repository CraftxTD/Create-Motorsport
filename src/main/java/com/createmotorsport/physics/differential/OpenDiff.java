package com.createmotorsport.physics.differential;

import com.createmotorsport.physics.Differential;
import com.createmotorsport.physics.spec.DifferentialSpec;

public final class OpenDiff implements Differential {

    public static final OpenDiff INSTANCE = new OpenDiff();

    private OpenDiff() {
    }

    @Override
    public void solve(Axle axle, DifferentialSpec spec, double massScale, boolean driftMode, double dt) {
        axle.omegaLeft -= axle.diffTorque * dt / axle.inertiaLeft;
        axle.omegaRight += axle.diffTorque * dt / axle.inertiaRight;
        axle.diffTorque = 0.0;
    }
}
