package com.createmotorsport.physics.spec;


public sealed interface DiffParams {

    DifferentialSpec.Type type();

    record Open() implements DiffParams {
        @Override
        public DifferentialSpec.Type type() {
            return DifferentialSpec.Type.OPEN;
        }
    }

    record ClutchLsd(double antiSlipTorque, double driftAntiSlipTorque, double torqueSensitivity,
                     double decelerationFactor) implements DiffParams {
        @Override
        public DifferentialSpec.Type type() {
            return DifferentialSpec.Type.CLUTCH_LSD;
        }
    }

    record Locked() implements DiffParams {
        @Override
        public DifferentialSpec.Type type() {
            return DifferentialSpec.Type.LOCKED;
        }
    }
}
