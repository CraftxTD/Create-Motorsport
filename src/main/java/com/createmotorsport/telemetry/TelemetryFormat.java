package com.createmotorsport.telemetry;

import com.createmotorsport.physics.control.TractionControl;
import com.createmotorsport.block.entity.EngineBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TelemetryFormat {

    private record Sample(WheelTelemetry t, double steerDeg) {
    }

    @FunctionalInterface
    private interface Read {
        double of(Sample s);
    }

    private record WheelCol(String name, boolean core, String format, Read read) {
    }

    private static WheelCol col(String name, String format, Read read) {
        return new WheelCol(name, true, format, read);
    }

    private static WheelCol extra(String name, String format, Read read) {
        return new WheelCol(name, false, format, read);
    }

    private static final WheelCol[] WHEEL_COLS = {
            col("grounded", "%.0f", s -> s.t().grounded() ? 1 : 0),
            col("load_N", "%.1f", s -> s.t().loadN()),
            col("slip_ratio", "%.4f", s -> s.t().slipRatio()),
            col("slip_angle_deg", "%.3f", s -> s.t().slipAngleDeg()),
            col("vlon_ms", "%.3f", s -> s.t().vLonMs()),
            col("vlat_ms", "%.3f", s -> s.t().vLatMs()),
            col("Fx_N", "%.1f", s -> s.t().longForceN()),
            col("Fy_N", "%.1f", s -> s.t().latForceN()),
            col("omega", "%.2f", s -> s.t().omega()),
            col("wheelspeed_ms", "%.3f", s -> s.t().wheelSpeedMs()),
            col("spring_m", "%.4f", s -> s.t().springLenM()),
            col("compress_m", "%.4f", s -> s.t().compressionM()),
            col("eff_mu", "%.3f", s -> s.t().effMu()),
            col("peak_mu", "%.3f", s -> s.t().peakMu()),
            col("steer_deg", "%.2f", Sample::steerDeg),
            col("brake_Nm", "%.1f", s -> s.t().brakeTorqueNm()),
            col("grip_mult", "%.3f", s -> s.t().gripMult()),
            col("drive_Nm", "%.2f", s -> s.t().driveTorqueNm()),
            extra("tire_temp_C", "%.1f", s -> s.t().tireTempC()),
            col("rigid_m", "%.4f", s -> s.t().rigidLenM()),
            col("defl_m", "%.5f", s -> s.t().tireDeflM()),
            col("unsprung_v", "%.4f", s -> s.t().unsprungVMs()),
            col("eff_mass_kg", "%.2f", s -> s.t().effMassKg()),
            col("surface_mu", "%.4f", s -> s.t().surfaceMu()),
            col("hardpoint_v", "%.4f", s -> s.t().hardpointVMs()),
            col("hpv_span", "%.4f", s -> s.t().hpvSpanMs()),
            col("spring_N", "%.1f", s -> s.t().springForceN()),
            col("tire_rate_N", "%.0f", s -> s.t().tireRateN()),
            extra("hpv_world", "%.4f", s -> s.t().velWorld()),
            extra("hpv_body", "%.4f", s -> s.t().velBody()),
            col("hpv_diff", "%.4f", s -> s.t().velDiff()),
            extra("hpv_normal", "%.4f", s -> s.t().velNormal()),
            col("cast_lift", "%.5f", s -> s.t().castLift()),
            col("damp_frac", "%.4f", s -> s.t().dampFraction()),
            col("assist_m", "%.5f", s -> s.t().assistLift()),
            col("grip_use", "%.4f", s -> s.t().gripUse()),
            col("peak_N", "%.1f", s -> s.t().peakForceN()),
            col("slip_comb", "%.5f", s -> s.t().slipCombined()),
            col("slip_peak", "%.5f", s -> s.t().slipAtPeak()),
            col("curve_N", "%.1f", s -> s.t().curveForceN()),
            extra("hp_y", "%.4f", s -> s.t().hardpointY()),
            extra("rigid_raw", "%.5f", s -> s.t().rigidRawM()),
            col("assist_tgt", "%.5f", s -> s.t().assistTargetM()),
            extra("assist_rise", "%.5f", s -> s.t().assistRiseM()),
            extra("assist_probe", "%.3f", s -> s.t().assistProbeM()),
            col("assist_why", "%.0f", s -> s.t().assistWhy()),
            col("abs_f", "%.4f", s -> s.t().absFactor()),
            col("lock_slip", "%.4f", s -> s.t().lockSlip()),
    };

    private TelemetryFormat() {
    }

    private static java.util.List<WheelCol> profile(boolean extra) {
        return extra ? java.util.List.of(WHEEL_COLS)
                : java.util.Arrays.stream(WHEEL_COLS).filter(WheelCol::core).toList();
    }

    public static String header(CarActors car, boolean extra) {
        StringBuilder sb = new StringBuilder(
                "t_s,tick,speed_ms,speed_kmh,mass_kg,gear,rpm,throttle,clutch_locked,"
                        + "engine_torque_Nm,gear_ratio,wheel_torque_Nm,wheel_torque_applied,avg_wheel_omega,driven_wheels,"
                        + "power_mode,tc_on,boost_reserve,torque_factor,tc_slip_f,tc_cap_f,tc_slip_target,tc_slip,drag_thr,"
                        + "cruise_on,cruise_tgt_ms,ignition,firing,starter_Nm,in_throttle,in_brake,in_steer,in_keys,"
                        + "pos_x,pos_y,pos_z,vel_x,vel_y,vel_z,"
                        + "bodypos_x,bodypos_y,bodypos_z,quat_x,quat_y,quat_z,quat_w,"
                        + "com_x,com_y,com_z");
        for (int a = 0; a < car.suspensions().size(); a++) {
            for (String sideTag : new String[]{"L", "R"}) {
                for (WheelCol col : profile(extra)) {
                    sb.append(",s").append(a).append(sideTag).append('_').append(col.name());
                }
            }
        }
        return sb.toString();
    }

    public static String row(Level level, BlockPos worldPosition, CarActors car,
                             long logStartGameTime, boolean cruiseOn, double cruiseTargetMs,
                             boolean extra, DriverInputs inputs) {
        java.util.Locale l = java.util.Locale.ROOT;
        StringBuilder sb = new StringBuilder();

        double tS = (level.getGameTime() - logStartGameTime) / 20.0;
        Vec3 velocity = Sable.HELPER.getVelocity(level, Vec3.atCenterOf(worldPosition));
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        Vec3 worldPos = car.subLevel() != null
                ? car.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(worldPosition))
                : Vec3.atCenterOf(worldPosition);
        org.joml.Vector3dc bodyPos = car.subLevel() != null
                ? car.subLevel().logicalPose().position() : new org.joml.Vector3d();
        org.joml.Quaterniondc quat = car.subLevel() != null
                ? car.subLevel().logicalPose().orientation() : new org.joml.Quaterniond();
        double mass = car.subLevel() instanceof ServerSubLevel ssl && ssl.getMassTracker() != null
                ? ssl.getMassTracker().getMass() : -1.0;
        org.joml.Vector3dc com = car.subLevel() instanceof ServerSubLevel ssl2 && ssl2.getMassTracker() != null
                && ssl2.getMassTracker().getCenterOfMass() != null
                ? ssl2.getMassTracker().getCenterOfMass() : new org.joml.Vector3d();

        EngineBlockEntity engine = car.engine();
        String gear = engine != null ? engine.getDrivetrain().gearDisplay() : "-";
        int rpm = engine != null ? engine.getDisplayRpm() : 0;
        double throttle = engine != null ? engine.getThrottle() : 0.0;
        boolean clutchLocked = engine != null && engine.getDrivetrain().lastClutchLocked();
        double engineTorque = engine != null ? engine.getDrivetrain().lastEngineTorque() : 0.0;
        double gearRatio = engine != null ? engine.getDrivetrain().lastRatio() : 0.0;
        double wheelTorque = engine != null ? engine.getDrivetrain().lastWheelTorque() : 0.0;
        TractionControl.Result traction = car.state() == null ? TractionControl.Result.OFF : car.state().traction();
        double wheelTorqueApplied = engine != null ? engine.getWheelTorqueTotal() * traction.applied() : 0.0;
        double avgOmega = engine != null ? engine.getAvgWheelOmega() : 0.0;
        int drivenWheels = engine != null ? engine.getDrivenWheelCount() : 0;
        int powerMode = engine != null ? engine.getPowerMode() : 0;
        boolean tcOn = car.state() != null && car.state().aids().tractionControl();
        double boostReserve = engine != null ? engine.getBoostReserve() : 0.0;
        double torqueFactor = engine != null ? engine.getPowerFactor() * traction.applied() : 0.0;
        double tcSlipF = traction.factor();
        double tcCapF = traction.capacityCap();
        double tcTarget = traction.slipTarget();
        double tcSlip = traction.slipRatio();
        double dragThrottle = car.state() == null ? 0.0 : car.state().dragThrottle();
        boolean ignitionOn = engine != null && engine.ignitionOn();
        boolean firing = engine != null && engine.getDrivetrain().firing();
        double starterTorque = engine != null ? engine.getDrivetrain().starterTorque() : 0.0;

        sb.append(String.format(l, "%.2f,%d,%.3f,%.2f,%.1f,%s,%d,%.3f,%d,%.2f,%.3f,%.2f,%.2f,%.3f,%d,%d,%d,%.3f,%.3f,%.4f,%.4f,%.4f,%.4f,%.3f,%d,%.3f,%d,%d,%.2f,%.3f,%.3f,%.3f,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.4f,%.4f,%.4f,%.6f,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f",
                tS, level.getGameTime(), speed, speed * 3.6, mass, gear, rpm, throttle,
                clutchLocked ? 1 : 0, engineTorque, gearRatio, wheelTorque, wheelTorqueApplied,
                avgOmega, drivenWheels, powerMode, tcOn ? 1 : 0, boostReserve, torqueFactor,
                tcSlipF, tcCapF, tcTarget, tcSlip, dragThrottle, cruiseOn ? 1 : 0, cruiseTargetMs,
                ignitionOn ? 1 : 0, firing ? 1 : 0, starterTorque,
                inputs.throttle(), inputs.brake(), inputs.steer(), inputs.keys(),
                worldPos.x, worldPos.y, worldPos.z, velocity.x, velocity.y, velocity.z,
                bodyPos.x(), bodyPos.y(), bodyPos.z(), quat.x(), quat.y(), quat.z(), quat.w(),
                com.x(), com.y(), com.z()));

        Direction carRight = carRight(car);
        for (SuspensionBlockEntity s : car.suspensions()) {
            double base = s.getSteerAngleRad();
            for (SuspensionBlockEntity.WheelSide side : carSideOrder(s, carRight)) {
                double steerDeg = Math.toDegrees(s.wheelSteerAngle(base, side));
                Sample sample = new Sample(s.getTelemetry(side), steerDeg);
                for (WheelCol col : profile(extra)) {
                    sb.append(',').append(String.format(l, col.format(), col.read().of(sample)));
                }
            }
        }
        return sb.toString();
    }

    private static SuspensionBlockEntity.WheelSide[] carSideOrder(SuspensionBlockEntity axle,
                                                                  Direction carRight) {
        return axle.getSideDirection(SuspensionBlockEntity.WheelSide.RIGHT) == carRight
                ? new SuspensionBlockEntity.WheelSide[]{
                        SuspensionBlockEntity.WheelSide.LEFT, SuspensionBlockEntity.WheelSide.RIGHT}
                : new SuspensionBlockEntity.WheelSide[]{
                        SuspensionBlockEntity.WheelSide.RIGHT, SuspensionBlockEntity.WheelSide.LEFT};
    }

    private static Direction carRight(CarActors car) {
        for (SuspensionBlockEntity s : car.suspensions()) {
            if (s.isFrontAxle()) {
                return s.getSideDirection(SuspensionBlockEntity.WheelSide.RIGHT);
            }
        }
        return car.suspensions().isEmpty()
                ? Direction.EAST
                : car.suspensions().get(0).getSideDirection(SuspensionBlockEntity.WheelSide.RIGHT);
    }
}
