package com.createmotorsport.physics.differential;

import com.createmotorsport.physics.Differential;
import com.createmotorsport.physics.spec.DifferentialSpec;

public final class LockedDiff implements Differential {

    public static final LockedDiff INSTANCE = new LockedDiff();

    private LockedDiff() {
    }

    @Override
    public void solve(Axle axle, DifferentialSpec spec, double massScale, boolean driftMode, double dt) {
        double jointInertia = 1.0 / (1.0 / axle.inertiaLeft + 1.0 / axle.inertiaRight);
        double lambda = -(axle.omegaLeft - axle.omegaRight) * jointInertia;
        axle.omegaLeft += lambda / axle.inertiaLeft;
        axle.omegaRight -= lambda / axle.inertiaRight;
        axle.diffTorque += lambda / dt;
    }
}
