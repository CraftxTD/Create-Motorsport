package com.createmotorsport.physics;

import com.createmotorsport.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class DrivetrainSim {
    private static final double RAD_TO_RPM = 60.0 / (2.0 * Math.PI);

    public static final int GEAR_REVERSE = 0;
    public static final int GEAR_NEUTRAL = 1;
    public static final int GEAR_FIRST = 2;

    // from speed dreams, how quickly clutch re-engages after being disengaged, in seconds
    private static final double CLUTCH_ENGAGE_TIME = 0.4;

    // for semi-auto shifting, how long gearbox holds clutch open and cuts throttle
    private static final double SHIFT_RELEASE_TIME = 0.2;

    // trying to get clutch launch behavior better
    private static final double LAUNCH_FLARE_FRAC = 0.42;

    private final EngineSpec spec;

    private final int maxGear;

    private double rpm;
    private int gear = GEAR_NEUTRAL;
    private double clutchEngage = 1.0; // 0 = disengaged, 1 = fully engaged (Speed dreams' transferValue)
    private double shiftReleaseTimer = 0.0; // seconds left in the semi-auto clutch dip during a shift

    // last-computed values, for csv logging
    private double lastEngineTorque;
    private double lastRatio;
    private double lastWheelTorque;
    private boolean lastClutchLocked;

    public DrivetrainSim(EngineSpec spec) {
        this.spec = spec;
        this.maxGear = GEAR_FIRST + spec.topGear();
        this.rpm = spec.idleRpm();
    }


    /**
     * Advances the drivetrain by 1 tick
     * @param running       engine has fuel and is on
     * @param throttle      0-1, from pedal
     * @param clutchHeld    true while the clutch pedal (signal) is held, true is a disengaged clutch
     * @param semiAuto      true for F1-style paddle shifting: shift without the clutch, the box blips it
     * @param shiftUpEdge   rising edge of the shift-up signal this tick
     * @param shiftDownEdge rising edge of the shift-down signal this tick
     * @param wheelOmega    average driven-wheel angular velocity (rad/s), positive rolling forward
     * @param dt            tick length (s)
     * @return torque delivered to the driven wheels in newton-meters (Nm), signed (negative in reverse)
     */
    public double update(boolean running, double throttle, boolean clutchHeld, boolean semiAuto,
                         boolean shiftUpEdge, boolean shiftDownEdge, double wheelOmega, double dt) {
        if (!running) {
            this.rpm = Math.max(0.0, this.rpm - this.spec.redlineRpm() * 2.0 * dt);
            return record(0.0, 0.0, 0.0, false);
        }


        // manual and semi-auto modes now
        boolean canShift = semiAuto || clutchHeld;
        if (canShift) {
            if (shiftUpEdge) {
                this.gear = Math.min(this.maxGear, this.gear + 1);
                if (semiAuto) this.shiftReleaseTimer = SHIFT_RELEASE_TIME;
            } else if (shiftDownEdge) {
                this.gear = Math.max(GEAR_REVERSE, this.gear - 1);
                if (semiAuto) this.shiftReleaseTimer = SHIFT_RELEASE_TIME;
            }
        }
        if (this.shiftReleaseTimer > 0.0) {
            this.shiftReleaseTimer = Math.max(0.0, this.shiftReleaseTimer - dt);
        }
        boolean shiftDip = this.shiftReleaseTimer > 0.0;

        double ratio = overallRatio(this.gear);
        boolean disengaged = clutchHeld || shiftDip || this.gear == GEAR_NEUTRAL;

        if (disengaged) {
            // cuts throttle during paddle-shifting, inspired by speed dreams
            this.clutchEngage = 0.0;
            double revThrottle = shiftDip ? 0.0 : throttle;
            double targetRpm = this.spec.idleRpm() + revThrottle * (this.spec.redlineRpm() - this.spec.idleRpm());
            this.rpm = Mth.lerp(1.0 - Math.exp(-5.0 * dt), this.rpm, Math.max(this.spec.idleRpm(), targetRpm));
            if (revThrottle < 0.02) {
                this.rpm = Mth.lerp(1.0 - Math.exp(-3.0 * dt), this.rpm, this.spec.idleRpm());
            }
            return record(0.0, ratio, 0.0, false);
        }

        double absRatio = Math.abs(ratio);
        double rpmFromWheels = Math.abs(wheelOmega) * absRatio * RAD_TO_RPM;

        // speed dreams' transferValue ramps back up over short time instead of snapping locked, with transmitted
        // torque reaching full at 1/3 engaged
        this.clutchEngage = Math.min(1.0, this.clutchEngage + dt / CLUTCH_ENGAGE_TIME);
        double transfer = Math.min(1.0, this.clutchEngage * 3.0);

        // where the free revving engine wants to sit for the throttle
        double idle = this.spec.idleRpm();
        double flareRange = LAUNCH_FLARE_FRAC * (this.spec.redlineRpm() - idle);
        double flareTarget = idle + throttle * flareRange;
        double launchRpm = idle + flareRange; // full-throttle flare, top of the blend window

        // clutch slipping behavior from speed dreams
        double lock = Mth.clamp((rpmFromWheels - idle) / (launchRpm - idle), 0.0, 1.0);
        double targetRpm = Mth.lerp(lock, flareTarget, rpmFromWheels);
        targetRpm = Math.min(Math.max(idle, targetRpm), this.spec.redlineRpm() * 1.02);
        this.rpm = Mth.lerp(1.0 - Math.exp(-8.0 * dt), this.rpm, targetRpm);

        boolean slipping = lock < 1.0 || transfer < 1.0;

        // help stand-still drifting by making engine braking only work when wheels are turning near the idle-in-gear speed
        double engineTorque;
        if (throttle < 0.02) {
            double brakeEngage = Mth.clamp(rpmFromWheels / idle, 0.0, 1.0);
            engineTorque = -this.spec.engineBrakeTorque(this.rpm) * brakeEngage;
        } else {
            engineTorque = throttle * this.spec.torqueAt(this.rpm);
        }
        // Scale the whole curve so its peak equals the configured crank torque (Nm), with curve shape unchanged
        engineTorque *= Config.ENGINE_PEAK_TORQUE.getAsDouble() / this.spec.peakTorque();

        // Transmitted torque scales with engagement
        double wheelTorque = engineTorque * transfer * ratio * this.spec.drivelineEfficiency();
        return record(engineTorque, ratio, wheelTorque, !slipping);
    }

    private double record(double engineTorque, double ratio, double wheelTorque, boolean clutchLocked) {
        this.lastEngineTorque = engineTorque;
        this.lastRatio = ratio;
        this.lastWheelTorque = wheelTorque;
        this.lastClutchLocked = clutchLocked;
        return wheelTorque;
    }

    public double lastEngineTorque() {
        return this.lastEngineTorque;
    }

    public double lastRatio() {
        return this.lastRatio;
    }

    public double lastWheelTorque() {
        return this.lastWheelTorque;
    }

    public boolean lastClutchLocked() {
        return this.lastClutchLocked;
    }

    // Signed overall ratio from crank -> wheel for gear index; 0 for neutral, negative for reverse
    private double overallRatio(int gearIndex) {
        if (gearIndex == GEAR_REVERSE) {
            return -this.spec.overallReverseRatio();
        }
        if (gearIndex == GEAR_NEUTRAL) {
            return 0.0;
        }
        return this.spec.overallRatio(gearIndex - GEAR_FIRST);
    }

    public double getRpm() {
        return this.rpm;
    }

    public double getRpmFraction() {
        return Mth.clamp(this.rpm / this.spec.redlineRpm(), 0.0, 1.0);
    }

    public int getGearIndex() {
        return this.gear;
    }

    public String gearDisplay() {
        return switch (this.gear) {
            case GEAR_REVERSE -> "R";
            case GEAR_NEUTRAL -> "N";
            default -> String.valueOf(this.gear - GEAR_NEUTRAL);
        };
    }

    // menu-sync code: 0 = R, 1=N, 2= 1st gear, etc
    public int gearCode() {
        return this.gear;
    }

    public EngineSpec spec() {
        return this.spec;
    }

    public void save(CompoundTag tag) {
        tag.putDouble("Rpm", this.rpm);
        tag.putInt("Gear", this.gear);
    }

    public void load(CompoundTag tag) {
        this.rpm = tag.getDouble("Rpm");
        this.gear = Mth.clamp(tag.getInt("Gear"), GEAR_REVERSE, this.maxGear);
    }
}
