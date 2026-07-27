package com.createmotorsport;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Fallback forward gear ratios if the config string can't be parsed
    private static final double[] DEFAULT_GEAR_RATIOS = {3.20, 2.49, 2.00, 1.67, 1.44, 1.26, 1.00};

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

    public static final ModConfigSpec.ConfigValue<String> GEAR_RATIOS = BUILDER
            .comment("Forward gear ratios, highest (1st gear) to lowest (top gear), comma-separated.",
                    "Higher ratio = more torque but lower speed per gear",
                    "Multiplied by finalDrive to get the final crank to wheel ratio.",
                    "Default \"3.2, 2.49, 2.0, 1.67, 1.44, 1.26, 1.0\".")
            .define("gearRatios", "3.2, 2.49, 2.0, 1.67, 1.44, 1.26, 1.0");

    public static final ModConfigSpec.DoubleValue FINAL_DRIVE = BUILDER
            .comment("Final-drive ratio, multiplied onto every gear (and reverse).",
                    "Raise it to shorten all the gearing at once (more acceleration, lower top speed);",
                    "Default 14.0")
            .defineInRange("finalDrive", 14.0, 0.1, 60.0);

    public static final ModConfigSpec.DoubleValue REVERSE_RATIO = BUILDER
            .comment("Reverse gear ratio, default 3.2")
            .defineInRange("reverseRatio", 3.2, 0.1, 20.0);

    static { BUILDER.pop(); }

    // =======================================================================
    // PHYSICS
    // =======================================================================
    static { BUILDER.push("physics"); }

    public static final ModConfigSpec.DoubleValue DRIVETRAIN_TORQUE_SCALE = BUILDER
            .comment("Converts real crank torque (Nm) into Sable's world scale,",
                    "to account for Minecraft-scale car mass")
            .defineInRange("drivetrainTorqueScale", 0.03, 0.0001, 10.0);

    public static final ModConfigSpec.DoubleValue DIFFERENTIAL_ANTISLIP_TORQUE = BUILDER
            .comment("Limited-slip differential lock: 0 = open diff (inside wheel spins up freely), 200+ is like",
                    "a near-locked spool (both wheels forced to the same speed); higher = more traction off the",
                    "line but more understeer; default is 200")
            .defineInRange("differentialAntiSlipTorque", 200.0, 0.0, 100000.0);

    public static final ModConfigSpec.DoubleValue SIM_SLIP_LIMIT = BUILDER
            .comment("SIM tire model only; caps how far up the slip curve the slip is allowed to travel, so the curve",
                    "keeps most of its grip until here, then that grip is held; LOWER = more arcade-y,",
                    "HIGHER = easier to spin out; default 1.5")
            .defineInRange("simSlipLimit", 1.5, 0.8, 5.0);

    public static final ModConfigSpec.DoubleValue SIM_LATERAL_GRIP = BUILDER
            .comment("SIM tire model only; lateral (sideways) grip as a multiple of longitudinal grip.",
                    "Changing from 1.0 creates a friction ellipse instead of a circle",
                    "1.0 = equal both ways (friction circle)",
                    ">1 = more sideways grip that might be considered more realistic,",
                    "But its still being tested, default is 1.5 for now")
            .defineInRange("simLateralGrip", 1.5, 0.3, 3.0);

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
                    "Default is SIM, so true")
            .define("simTireModel", true);

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

    public static final ModConfigSpec.BooleanValue ENABLE_ADVANCED_INPUT = BUILDER
            .comment("Enable Racing Wheel / Pedal support, or any other advanced controller",
                    "Leave off if you only use standard gamepads. Default is true")
            .define("enableAdvancedInput", true);

    static { BUILDER.pop(); }

    // =========================================================================
    // ANIMATION
    // =======================================================================
    static { BUILDER.push("animation"); }

    public static final ModConfigSpec.DoubleValue STEERING_WHEEL_MAX_ANGLE = BUILDER
            .comment("How far the steering wheel rim turns at full lock, in degrees, each way",
                    "The wheel animates according to the scaled analog input",
                    "This number is purely cosmetic, default 450\\({}^{\\circ }\\)")
            .defineInRange("steeringWheelMaxAngle", 450.0, 30.0, 1080.0);

    static { BUILDER.pop(); }

    static final ModConfigSpec SPEC = BUILDER.build();


    // for the csv logging, dump the entire config as "section.name,\"value\"" lines, to help identify issues in bug reports
    public static List<String> dumpForLog() {
        List<String> out = new ArrayList<>();
        for (java.lang.reflect.Field field : Config.class.getDeclaredFields()) {
            if (!ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                ModConfigSpec.ConfigValue<?> value = (ModConfigSpec.ConfigValue<?>) field.get(null);
                String path = String.join(".", value.getPath());
                out.add(path + ",\"" + value.get() + "\"");
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    // Parse the gearRatios string into an array, cached so its only reparsed when the string changes;
    // drops unparseable entries
    private static String cachedGearString;
    private static double[] cachedGearRatios = DEFAULT_GEAR_RATIOS;

    public static double[] gearRatios() {
        String raw = GEAR_RATIOS.get();
        if (!raw.equals(cachedGearString)) {
            cachedGearString = raw;
            cachedGearRatios = parseGearRatios(raw);
        }
        return cachedGearRatios;
    }

    private static double[] parseGearRatios(String raw) {
        List<Double> ratios = new ArrayList<>();
        for (String part : raw.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            try {
                double value = Double.parseDouble(part);
                if (value > 0.0) {
                    ratios.add(value);
                }
            } catch (NumberFormatException ignored) {
                // skip garbage entries rather than blow up the whole drivetrain
            }
        }
        if (ratios.isEmpty()) {
            return DEFAULT_GEAR_RATIOS;
        }
        double[] out = new double[ratios.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ratios.get(i);
        }
        return out;
    }
}
