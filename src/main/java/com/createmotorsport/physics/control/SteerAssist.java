package com.createmotorsport.physics.control;

import com.createmotorsport.physics.vehicle.CornerState;
import net.minecraft.util.Mth;

import java.util.List;

public final class SteerAssist {

    private SteerAssist() {
    }

    public static double lockFraction(double steerDeg, double frontSlipDeg, double frontPeakDeg, double fullLockDeg) {
        if (!Double.isFinite(frontPeakDeg) || fullLockDeg <= 0.0) {
            return 1.0;
        }
        double limit = Math.abs(steerDeg) + frontPeakDeg - frontSlipDeg;
        return Mth.clamp(limit / fullLockDeg, 0.0, 1.0);
    }

    public static double frontLock(double steerRad, List<? extends CornerState> fitted, double fullLockDeg) {
        double sign = Math.signum(steerRad);
        double slip = 0.0, peak = 0.0;
        int wheels = 0;
        for (CornerState w : fitted) {
            if (w.grounded && Double.isFinite(w.peakSlipAngleRad)) {
                slip -= w.slipAngleRad * sign;
                peak += w.peakSlipAngleRad;
                wheels++;
            }
        }
        if (wheels == 0) {
            return 1.0;
        }
        return lockFraction(Math.toDegrees(steerRad), Math.toDegrees(slip / wheels), Math.toDegrees(peak / wheels),
                fullLockDeg);
    }
}
