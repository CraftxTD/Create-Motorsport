package com.createmotorsport.physics;

import net.minecraft.util.Mth;

/**
 * Immutable definition of an engine + gearbox tier with values stated in real SI units
 * (Nm, RPM); DRIVETRAIN_TORQUE_SCALE in the common config, converts crank torque
 * into Sable's world scale, so users will see realistic car numbers, not Create/Sable's numbers
 */
public record EngineSpec(
        double idleRpm,
        double redlineRpm,
        double peakTorque,
        double[] gearRatios,
        double reverseRatio,
        double finalDrive,
        double shiftUpRpm,
        double shiftDownRpm,
        double drivelineEfficiency
) {

    // still working on tuning this
    public static final EngineSpec RACING_V6_HYBRID = new EngineSpec(
            3000, 15000, 420,
            new double[]{3.20, 2.49, 2.00, 1.67, 1.44, 1.26, 1.12, 1.00},
            3.20, 14.0, 13500, 8500, 0.93
    );

    public double overallRatio(int gear) {
        return this.gearRatios[gear] * this.finalDrive;
    }

    public double overallReverseRatio() {
        return this.reverseRatio * this.finalDrive;
    }

    public int topGear() {
        return this.gearRatios.length - 1;
    }


    // Crank torque in (Nm) at full throttle, including rev limiter. Attempted to model realistic weak idle,
    // flat peak through the mid range and taper to the limiter
    public double torqueAt(double rpm) {
        if (rpm >= this.redlineRpm) {
            return 0.0;
        }
        if (rpm <= 0.0) {
            return 0.30 * this.peakTorque;
        }

        double idleFrac = this.idleRpm / this.redlineRpm;
        double n = rpm / this.redlineRpm;
        double peakStart = 0.45;
        double peakEnd = 0.80;

        double factor;
        if (n < idleFrac) {
            factor = Mth.lerp(n / idleFrac, 0.30, 0.55);
        } else if (n < peakStart) {
            factor = Mth.lerp((n - idleFrac) / (peakStart - idleFrac), 0.55, 1.0);
        } else if (n < peakEnd) {
            factor = 1.0;
        } else {
            factor = Mth.lerp((n - peakEnd) / (1.0 - peakEnd), 1.0, 0.82);
        }
        return factor * this.peakTorque;
    }


    // Passive drag form the engine when throttle is closed, in newton-meters (Nm)
    public double engineBrakeTorque(double rpm) {
        return 0.07 * this.peakTorque * Mth.clamp(rpm / this.redlineRpm, 0.0, 1.1);
    }
}
