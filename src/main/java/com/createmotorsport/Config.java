package com.createmotorsport;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Set to 'true' to see additional debugging logging")
            .define("enableDebugLogging", false);

    public static final ModConfigSpec.DoubleValue DRIVETRAIN_TORQUE_SCALE = BUILDER
            .comment("Tuning for torque maths, to account for Minecraft scale car mass")
            .defineInRange("drivetrainTorqueScale", 0.016, 0.0001, 10.0);

    public static final ModConfigSpec.DoubleValue DIFFERENTIAL_ANTISLIP_TORQUE = BUILDER
            .comment("0 = open differential, 200+ is like a near-locked spool")
            .defineInRange("differentialAntiSlipTorque", 200.0, 0.0, 100000.0);

    static final ModConfigSpec SPEC = BUILDER.build();
}
