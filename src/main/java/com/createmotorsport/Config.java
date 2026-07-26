package com.createmotorsport;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Set to 'true' to see additional debugging logging")
            .define("enableDebugLogging", false);

    // ======================================================================
    // ENGINE
    // ======================================================================
    static { BUILDER.push("engine"); }

    public static final ModConfigSpec.DoubleValue ENGINE_PEAK_TORQUE = BUILDER
            .comment("Peak crank torque of the engine in real newton-metres (Nm).",
                    "320 is a realistic number for a 2011 era V8 F1 engine.",
                    "Turn it up if you want to buff the engine, but keep in mind",
                    "that at some point you are also limited by grip.")
            .defineInRange("enginePeakTorque", 320.0, 50.0, 2000.0);

    static { BUILDER.pop(); }

    // =======================================================================
    // PHYSICS
    // =======================================================================
    static { BUILDER.push("physics"); }

    public static final ModConfigSpec.DoubleValue DRIVETRAIN_TORQUE_SCALE = BUILDER
            .comment("Converts real crank torque (Nm) into Sable's world scale,",
                    "to account for Minecraft-scale car mass")
            .defineInRange("drivetrainTorqueScale", 0.016, 0.0001, 10.0);

    public static final ModConfigSpec.DoubleValue DIFFERENTIAL_ANTISLIP_TORQUE = BUILDER
            .comment("Limited-slip differential lock: 0 = open diff (inside wheel spins up freely), 200+ is like",
                    "a near-locked spool (both wheels forced to the same speed); higher = more traction off the",
                    "line but more understeer; default is 200")
            .defineInRange("differentialAntiSlipTorque", 200.0, 0.0, 100000.0);

    public static final ModConfigSpec.DoubleValue SIM_SLIP_LIMIT = BUILDER
            .comment("SIM tire model only; caps how far up the slip curve the slip is allowed to travel, so the curve",
                    "keeps most of its grip until here, then that grip is held; LOWER = more arcade-y,",
                    "HIGHER = easier to spin out; default 4.0")
            .defineInRange("simSlipLimit", 4.0, 0.8, 8.0);

    public static final ModConfigSpec.DoubleValue TIRE_FORCE_RELAXATION = BUILDER
            .comment("How fast the longitudinal tire force chases its target each substep",
                    "(speed dreams' FLOAT_RELAXATION); 1.0 = instant (might judder on launch)",
                    "lower = smoother but laggier; default 0.3")
            .defineInRange("tireForceRelaxation", 0.3, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue LATERAL_GRIP_FRACTION = BUILDER
            .comment("Fraction of a wheel's sideways velocity killed per substep by lateral grip",
                    "Higher = more track/rail-like, lower = slidier; default 0.5")
            .defineInRange("lateralGripFraction", 0.5, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue SIM_LOWSPEED_BLEND_MS = BUILDER
            .comment("SIM tire model only; below this speed (m/s) the saturated slip-angle force would jitter or spin a",
                    "car and never stop, so lateral grip blends back to the simple arcade cancellation",
                    "Default 4.0 m/s (~14 km/h)")
            .defineInRange("simLowSpeedBlend", 4.0, 0.5, 15.0);

    public static final ModConfigSpec.DoubleValue ROLLING_RESISTANCE_COEF = BUILDER
            .comment("Rolling resistance as a fraction of the tire's vertical load. Slows a coasting car and",
                    "settles it to a stop; higher = more drag / stops sooner. Default 0.015")
            .defineInRange("rollingResistanceCoef", 0.015, 0.0, 0.2);

    public static final ModConfigSpec.DoubleValue MAX_CORNERING_G = BUILDER
            .comment("Cap on suspension spring force in g (multiples of static wheel load), " +
                    "Stops a force spike (like a bump) from launching the car, also affects/caps the max load a tire can carry," +
                    "Lower = tamer over bumps, higher = allows bigger forces / more grip under load. Default 6.0")
            .defineInRange("maxCorneringG", 6.0, 1.0, 20.0);

    public static final ModConfigSpec.DoubleValue ROLL_INFLUENCE = BUILDER
            .comment("Where the lateral tire force is applied vertically; 0 is ground level",
                    "(least rollover potential), 1 is at centre of mass (most roll potential). Default 0.2")
            .defineInRange("rollInfluence", 0.2, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue FRICTION_ELLIPSE_LONG_WEIGHT = BUILDER
            .comment("ARCADE tire model only; weights longitudinal grip inside the friction ellipse",
                    "(so a combined accelerate-while-turning budget) ",
                    "<1 lets you put more power down mid-corner; ",
                    ">1 makes the throttle take from this budget more. Default 0.5")
            .defineInRange("frictionEllipseLongWeight", 0.5, 0.1, 2.0);

    public static final ModConfigSpec.DoubleValue SABLE_DRAG_SCALE = BUILDER
            .comment("Temporary fix for Sable's universal drag. It applies in their units, which ends up making the ",
                    "drag force roughly 7x too strong, making the default 0.14 until we improve the aerodynamics modeling")
            .defineInRange("sableDragScale", 0.14, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue SIM_TIRE_MODEL = BUILDER
            .comment("false = ARCADE tire model, like driving on rails",
                    "true = SIM tire model: more realistic, lets you spin out of control",
                    "Default is ARCADE, so false")
            .define("simTireModel", false);

    static { BUILDER.pop(); }

    // =========================================================================
    // CONTROLS
    // =======================================================================
    static { BUILDER.push("controls"); }

    public static final ModConfigSpec.BooleanValue SEMI_AUTO_SHIFT = BUILDER
            .comment("false = full manual: you must hold the clutch channel to change gear",
                    "true = paddle shifters like F1 actually uses")
            .define("semiAutoShift", true);

    public static final ModConfigSpec.DoubleValue STEER_SPEED_SENSITIVITY = BUILDER
            .comment("Gamepad Controller 'Assist': velocity based steering lock",
                    "Max steer angle is scaled by 1/(1 + k*speed^2), so you",
                    "cant steer as much at higher speed and spin out")
            .defineInRange("steerSpeedSensitivity", 0.0006, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue STEER_INPUT_GAMMA = BUILDER
            .comment("Gamepad Controller 'Assist': Exponent applied to analog steering input",
                    "1.0 = linear; softens small stick movements near centre for finer control")
            .defineInRange("steerInputGamma", 1.8, 1.0, 4.0);

    public static final ModConfigSpec.DoubleValue PEDAL_INPUT_GAMMA = BUILDER
            .comment("Exponent applied to analog throttle and brake (trigger) input. 1.0 = linear;",
                    "~1.8 and the first half of the signal only asks for ~30% power, easier to feather the throttle")
            .defineInRange("pedalInputGamma", 1.8, 1.0, 4.0);

    static { BUILDER.pop(); }

    static final ModConfigSpec SPEC = BUILDER.build();
}
