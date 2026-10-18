package com.createmotorsport.physics.differential;

import com.createmotorsport.physics.Differential;
import com.createmotorsport.physics.spec.DifferentialSpec;

public final class Differentials {

    private Differentials() {
    }

    public static Differential of(DifferentialSpec.Type type) {
        return switch (type) {
            case OPEN -> OpenDiff.INSTANCE;
            case CLUTCH_LSD -> ClutchLsdDiff.INSTANCE;
            case LOCKED -> LockedDiff.INSTANCE;
        };
    }
}
