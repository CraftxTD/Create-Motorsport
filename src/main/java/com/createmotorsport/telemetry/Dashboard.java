package com.createmotorsport.telemetry;

import com.createmotorsport.Config;
import com.createmotorsport.block.entity.EngineBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.vehicle.AssembledCar;
import com.createmotorsport.vehicle.CarAssembler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public record Dashboard(int speedKmh, int gearCode, int rpm, boolean braking, int steer, int powerMode,
                        boolean tractionControl, boolean boosting, int throttlePct, int[] tireTempsC, int slipMask,
                        int effMuX100, int gripUsePct, int gripLonPct, int gripLatPct, boolean engineOff) {

    public static final Dashboard EMPTY = new Dashboard(0, 1, 0, false, 0, 8, false, false, 0, new int[0], 0, 0, 0,
            0, 0, false);

    private static final double SLIP_RATIO = 0.20;
    private static final double SLIP_ANGLE = 10.0;

    public static Dashboard gather(Level level, BlockPos at, @Nullable SubLevel subLevel, boolean tractionControl,
                                   float throttle01, boolean braking, float steer01) {
        int gear = 1;
        int rpm = 0;
        int speed = 0;
        int mode = 8;
        boolean boost = false;
        boolean engineOff = false;
        List<SuspensionBlockEntity> axles = new ArrayList<>();
        if (subLevel != null) {
            AssembledCar car = CarAssembler.of(level, subLevel);
            EngineBlockEntity engine = car == null ? null : car.engine();
            if (engine != null) {
                gear = engine.getGearCode();
                rpm = engine.getDisplayRpm();
                mode = engine.getPowerMode();
                boost = engine.isBoosting();
                engineOff = !engine.getDrivetrain().firing();
            }
            if (car != null) {
                for (SuspensionBlockEntity s : car.suspensions()) {
                    if (!s.isRemoved()) {
                        axles.add(s);
                    }
                }
            }
            Vec3 velocity = Sable.HELPER.getVelocity(level, Vec3.atCenterOf(at));
            speed = (int) Math.round(Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 3.6);
        }

        axles.sort(Comparator.comparing((SuspensionBlockEntity s) -> !s.isFrontAxle())
                .thenComparingLong(s -> s.getBlockPos().asLong()));
        boolean thermal = Config.TIRE_THERMAL_MODEL.get();
        int[] temps = new int[thermal ? axles.size() * 2 : 0];
        int slipMask = 0;
        double gripUseMax = 0.0, worstLon = 0.0, worstLat = 0.0, muSum = 0.0;
        int muCount = 0;
        int ti = 0;
        for (SuspensionBlockEntity s : axles) {
            for (SuspensionBlockEntity.WheelSide side : SuspensionBlockEntity.WheelSide.values()) {
                WheelTelemetry t = s.getTelemetry(side);
                if (thermal) {
                    temps[ti] = (int) Math.round(t.tireTempC());
                }
                if (t.grounded()) {
                    if (Math.abs(t.slipRatio()) > SLIP_RATIO || Math.abs(t.slipAngleDeg()) > SLIP_ANGLE) {
                        slipMask |= (1 << ti);
                    }
                    if (t.gripUse() > gripUseMax) {
                        gripUseMax = t.gripUse();
                        worstLon = t.gripLon();
                        worstLat = t.gripLat();
                    }
                    muSum += s.getPeakMu(side);
                    muCount++;
                }
                ti++;
            }
        }
        return new Dashboard(speed, gear, rpm, braking, Math.round(Mth.clamp(-steer01, -1.0F, 1.0F) * 100.0F), mode,
                tractionControl, boost, Math.round(Mth.clamp(throttle01, 0.0F, 1.0F) * 100.0F), temps, slipMask,
                muCount > 0 ? (int) Math.round(muSum / muCount * 100.0) : 0,
                (int) Math.round(Math.min(gripUseMax, 1.5) * 100.0),
                (int) Math.round(Mth.clamp(worstLon, -1.5, 1.5) * 100.0),
                (int) Math.round(Mth.clamp(worstLat, -1.5, 1.5) * 100.0), engineOff);
    }

    public boolean differs(Dashboard o) {
        return speedKmh != o.speedKmh || gearCode != o.gearCode || Math.abs(rpm - o.rpm) > 50
                || braking != o.braking || Math.abs(steer - o.steer) > 2
                || powerMode != o.powerMode || tractionControl != o.tractionControl || boosting != o.boosting
                || throttlePct != o.throttlePct || slipMask != o.slipMask || effMuX100 != o.effMuX100
                || Math.abs(gripUsePct - o.gripUsePct) >= 2
                || Math.abs(gripLonPct - o.gripLonPct) >= 2 || Math.abs(gripLatPct - o.gripLatPct) >= 2
                || !Arrays.equals(tireTempsC, o.tireTempsC) || engineOff != o.engineOff;
    }

    public void write(CompoundTag tag) {
        tag.putInt("SpeedKmh", speedKmh);
        tag.putInt("GearCode", gearCode);
        tag.putInt("Rpm", rpm);
        tag.putBoolean("Brake", braking);
        tag.putInt("Steer", steer);
        tag.putInt("PowerMode", powerMode);
        tag.putBoolean("Tc", tractionControl);
        tag.putBoolean("Boost", boosting);
        tag.putInt("Throttle", throttlePct);
        tag.putIntArray("TireTemps", tireTempsC);
        tag.putInt("SlipMask", slipMask);
        tag.putInt("EffMu", effMuX100);
        tag.putInt("GripUse", gripUsePct);
        tag.putInt("GripLon", gripLonPct);
        tag.putInt("GripLat", gripLatPct);
        tag.putBoolean("EngineOff", engineOff);
    }

    public static Dashboard read(CompoundTag tag) {
        return new Dashboard(tag.getInt("SpeedKmh"), tag.getInt("GearCode"), tag.getInt("Rpm"),
                tag.getBoolean("Brake"), tag.getInt("Steer"), tag.getInt("PowerMode"), tag.getBoolean("Tc"),
                tag.getBoolean("Boost"), tag.getInt("Throttle"), tag.getIntArray("TireTemps"),
                tag.getInt("SlipMask"), tag.getInt("EffMu"), tag.getInt("GripUse"), tag.getInt("GripLon"),
                tag.getInt("GripLat"), tag.getBoolean("EngineOff"));
    }
}
