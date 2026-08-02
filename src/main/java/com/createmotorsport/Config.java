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

    public static final ModConfigSpec.DoubleValue DRIVETRAIN_TORQUE_SCALE = BUILDER
            .comment("Converts real crank torque (Nm) into Sable's world scale,",
                    "to account for Minecraft-scale car mass")
            .defineInRange("drivetrainTorqueScale", 0.035, 0.0001, 10.0);

    public static final ModConfigSpec.DoubleValue DRIVELINE_EFFICIENCY = BUILDER
            .comment("Driveline efficiency %",
                    "Real cars are 0.85-0.95",
                    "Default 0.93")
            .defineInRange("drivelineEfficiency", 0.93, 0.5, 1.0);

    public static final ModConfigSpec.DoubleValue ENGINE_BRAKE_FRACTION = BUILDER
            .comment("Passive Engine Drag as percent of peak torque",
                    "Default 0.15")
            .defineInRange("engineBrakeFraction", 0.15, 0.0, 0.3);


    static { BUILDER.pop(); }

    // =======================================================================
    // PHYSICS
    // =======================================================================
    static { BUILDER.push("physics"); }

    public static final ModConfigSpec.DoubleValue DIFFERENTIAL_ANTISLIP_TORQUE = BUILDER
            .comment("Limited-slip differential lock: 0 = open diff (inside wheel spins up freely), 200+ is like",
                    "a near-locked spool (both wheels forced to the same speed); higher = more traction off the",
                    "line but more understeer; default is 200")
            .defineInRange("differentialAntiSlipTorque", 200.0, 0.0, 100000.0);

    public static final ModConfigSpec.DoubleValue CENTER_DIFF_FRONT_BIAS = BUILDER
            .comment("AWD only; the center differential's front torque share",
                    "0.5 = even split; lower is rear biased, so more oversteer",
                    "Default 0.5")
            .defineInRange("centerDiffFrontBias", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue CENTER_DIFF_LOCK = BUILDER
            .comment("AWD only; 0 = open center diff",
                    "0.5 = up to half the torque can be transferred",
                    "1.0 = locked center",
                    "Default 0.5")
            .defineInRange("centerDiffLock", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue SIM_SLIP_LIMIT = BUILDER
            .comment("SIM tire model only; caps how far up the slip curve the slip is allowed to travel, so the curve",
                    "keeps most of its grip until here, then that grip is held; LOWER = more arcade-y,",
                    "HIGHER = easier to spin out; default 1.2")
            .defineInRange("simSlipLimit", 1.2, 0.8, 8.0);

    public static final ModConfigSpec.DoubleValue SIM_LATERAL_GRIP = BUILDER
            .comment("SIM tire model only; lateral (sideways) grip as a multiple of longitudinal grip.",
                    "Changing from 1.0 creates a friction ellipse instead of a circle",
                    "1.0 = equal both ways (friction circle)",
                    ">1 = more sideways grip",
                    "Default is 2.0 for now, but needs refining I think")
            .defineInRange("simLateralGrip", 2, 0.3, 3.0);

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
            .comment("Tire model 2 & 3 only; below this speed (m/s) the saturated slip-angle force would jitter or spin a",
                    "car and never stop, so lateral grip blends back to the simple arcade cancellation",
                    "The Fiala model will work better with a lower blend, but turn it up to 2m/s if using model 2",
                    "Default 2.0 m/s (~7 km/h)")
            .defineInRange("simLowSpeedBlend", 1.0, 0.0, 15.0);

    public static final ModConfigSpec.DoubleValue SIM_LOWSPEED_REF = BUILDER
            .comment("Tire model 3 only; floor (in m/s) on the speed that is used as the denominator when calculating slip",
                    "ratio and slip angle, so they stay finite as the car slows. Raising this value helps with twitchy behavior",
                    "near a stop at the cost of less responsive grip low this speed. Default 1.0")
            .defineInRange("simLowSpeedRef", 1.0, 0.1, 5.0);

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

    public static final ModConfigSpec.IntValue TIRE_MODEL = BUILDER
            .comment("Which tire model to use:",
                    "1 = Arcade, 2 = Pacejka, 3 = Fiala")
            .defineInRange("tireModel", 2, 1, 3);

    // ---- Fiala brush tire model (SIM mode only) -------------------------------------------------
    public static final ModConfigSpec.DoubleValue FIALA_CSLIP = BUILDER
            .comment("Fiala tire model only; Fiala longitudinal slip stiffness (N per unit slip ratio)",
                    "Higher = grip peak at a lower slip %",
                    "This is calibrated from a specific tire I found in Project Chrono, where ",
                    "193929 N is scaled down to a 65kpg car. Will handle scale better later")
            .defineInRange("fialaSlipStiffness", 5000.0, 100.0, 1000000.0);

    public static final ModConfigSpec.DoubleValue FIALA_CALPHA = BUILDER
            .comment("Fiala tire model only; Fiala cornering stiffness (N per radian of slip angle)",
                    "Higher breaks away more suddenly.",
                    "Calibrated from a Chrono example to ~65kpg again")
            .defineInRange("fialaCorneringStiffness", 1600.0, 100.0, 2000000.0);

    public static final ModConfigSpec.DoubleValue FIALA_MU_MAX = BUILDER
            .comment("Fiala tire model only; peak friction coefficient;",
                    "Scales by surface friction, load sensitivity, and the tier grip value",
                    "Default 1.5")
            .defineInRange("fialaMuMax", 1.5, 0.1, 3.0);

    public static final ModConfigSpec.DoubleValue FIALA_MU_MIN = BUILDER
            .comment("Fiala tire model only; Fiala sliding friction coefficient",
                    "Default 0.9")
            .defineInRange("fialaMuMin", 0.9, 0.1, 3.0);

    public static final ModConfigSpec.DoubleValue FIALA_RELAX_LENGTH = BUILDER
            .comment("Fiala tire model only; tire relaxation length (m) which is the distance the tire must roll for",
                    "its slip force to build up, instead of responding instantly. Car wobbles if this is too low",
                    "Default 2.0")
            .defineInRange("fialaRelaxLength", 2.0, 0.05, 5.0);

    // ---- Pacejka slip curve shape (SIM tire model 2 only) ---------------------------------------
    public static final ModConfigSpec.DoubleValue PACEJKA_CORNERING_STIFFNESS = BUILDER
            .comment("Pacejka B/C/E done the same as Speed dreams for now:",
                    "C = 2 - asin(RFactor)*2/PI, B = Ca/C, E = EFactor",
                    "Default 30 is same as Speed Dreams. Old default was 19")
            .defineInRange("pacejkaCorneringStiffness", 30.0, 1, 100);

    public static final ModConfigSpec.DoubleValue PACEJKA_RFACTOR = BUILDER
            .comment("Pacejka B/C/E done the same as Speed dreams for now:",
                    "C = 2 - asin(RFactor)*2/PI, B = Ca/C, E = EFactor",
                    "Default 0.8 is same as Speed Dreams, old default was 0.1564")
            .defineInRange("pacejkaRFactor", 0.8, 0, 1);

    public static final ModConfigSpec.DoubleValue PACEJKA_EFACTOR = BUILDER
            .comment("Pacejka B/C/E done the same as Speed dreams for now:",
                    "C = 2 - asin(RFactor)*2/PI, B = Ca/C, E = EFactor",
                    "Default 30 is same as Speed Dreams, old default was 0.85")
            .defineInRange("pacejkaEFactor", 0.7, 0, 1);

    // ---- Tire grip per axle (all tire models) ---------------------------------------------------
    public static final ModConfigSpec.DoubleValue TIRE_GRIP_FRONT = BUILDER
            .comment("Friction coefficient of FRONT racing slicks.",
                    "Separated per axle as a simple way to adjust the",
                    "oversteering balance. Default 1.6")
            .defineInRange("tireGripFront", 1.6, 0.1, 2.5);

    public static final ModConfigSpec.DoubleValue TIRE_GRIP_REAR = BUILDER
            .comment("Friction coefficient of REAR racing slicks.",
                    "Separated per axle as a simple way to adjust the",
                    "oversteering balance. Default 1.4")
            .defineInRange("tireGripRear", 1.4, 0.1, 2.5);

    // ---- Tire temperature (Speed Dreams style; applies to all tire models) ----------------------
    public static final ModConfigSpec.BooleanValue TIRE_THERMAL_MODEL = BUILDER
            .comment("Use thermal modeling for tires? Default is false for now")
            .define("tireThermalModel", false);

    public static final ModConfigSpec.DoubleValue TIRE_OPT_TEMP = BUILDER
            .comment("Thermal model only; optimal tire temperature in Celsius for grip",
                    "Default 90")
            .defineInRange("tireOptTemp", 70.0, 20.0, 150.0);

    public static final ModConfigSpec.DoubleValue TIRE_AMBIENT_TEMP = BUILDER
            .comment("Thermal model only; ambient temperature in Celsius",
                    "Default 30")
            .defineInRange("tireAmbientTemp", 30.0, -40.0, 60.0);

    public static final ModConfigSpec.DoubleValue COLD_MU_FACTOR = BUILDER
            .comment("Thermal model only; grip fraction when the tire is ambient temp",
                    "0.6 = 60% grip when cold, default 0.6")
            .defineInRange("coldGripFactor", 0.6, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue TIRE_HEATING_RATE = BUILDER
            .comment("Thermal model only; how fast the slip energy heats the tire (deg C per joule of friction work)",
                    "Higher = tires warm up faster; needs tuning to our force scale, for now default 0.005")
            .defineInRange("tireHeatingRate", 0.005, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue TIRE_COOLING_RATE = BUILDER
            .comment("Thermal model only; how fast the tire cools toward ambient (per second, scaled up with",
                    "speed for airflow). Higher = harder to keep warm (cools faster). Default 0.02")
            .defineInRange("tireCoolingRate", 0.02, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue AERO_DOWNFORCE = BUILDER
            .comment("Configurable aerodynamic downforce for testing purposes",
                    "(load = this * speed_m/s^2)",
                    "Will be incorporated into spoilers/wings/diffusers later",
                    "0 = off. Default is 0.06")
            .defineInRange("aeroDownforce", 0.06, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue BRAKE_STRENGTH = BUILDER
            .comment("Peak braking torque per wheel at full brake, before grip limits it",
                    "Braking needs to be improved overall, so this will be updated later",
                    "Default 2000")
            .defineInRange("brakeStrength", 2000.0, 100.0, 20000.0);

    public static final ModConfigSpec.BooleanValue ABS_ENABLED = BUILDER
            .comment("Enable Anti-lock braking",
                    "Default true")
            .define("absEnabled", true);

    public static final ModConfigSpec.DoubleValue ABS_SLIP_THRESHOLD = BUILDER
            .comment("ABS only; braking slip ratio where the ABS starts releasing brake pressure.",
                    "Peak grip on the slip curve sits around 0.1-0.2, so keep it near here,",
                    "but if you want to change it, higher = allows more slip",
                    "Default 0.15")
            .defineInRange("absSlipThreshold", 0.15, 0.05, 0.5);

    public static final ModConfigSpec.DoubleValue ABS_MIN_SPEED = BUILDER
            .comment("ABS shuts off under this speed (m/s) so the car can stop",
                    "Default 1.5")
            .defineInRange("absMinSpeed", 1.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue WHEEL_MASS = BUILDER
            .comment("Configurable tire mass for testing purposes",
                    "Default 20kg")
            .defineInRange("tireMass", 20.0, 1, 100);


    public static final ModConfigSpec.DoubleValue STEERING_MAX_DEGREES = BUILDER
            .comment("Offroad cars use 32 degrees so I started with that, ",
                    "but F1 cars are more like ~20-22 degrees supposedly,",
                    "before considering the anti-ackermann adjustment",
                    "They are specific to the track, however, so this might be too",
                    "tight for Minecraft world. Default 26.0 for now")
            .defineInRange("suspensionSteeringMaxAngle", 26.0, 1.0, 360.0);

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
            .defineInRange("steerSpeedSensitivity", 0.003, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue KEYPRESS_STEERING_RAMP = BUILDER
            .comment("1 / RAMP / 20 seconds, is the formula for how long",
                    "It takes a key press to be considered a full steering lock",
                    "It also applies equally to the key release. Default is 0.30")
            .defineInRange("keypressSteeringRamp", 0.30, 0.01, 1.0);

    public static final ModConfigSpec.DoubleValue STEER_INPUT_GAMMA = BUILDER
            .comment("Gamepad Controller 'Assist': Exponent applied to analog steering input",
                    "1.0 = linear; softens small stick movements near centre for finer control")
            .defineInRange("steerInputGamma", 1.8, 1.0, 4.0);

    public static final ModConfigSpec.DoubleValue PEDAL_KEY_RAMP = BUILDER
            .comment("1 / RAMP / 20 seconds, is the formula for how long",
                    "It takes a key press to be considered a full pedal input",
                    "It also applies equally to the key release. Default is 0.30")
            .defineInRange("pedalKeyRamp", 0.3, 0.01, 1.0);

    public static final ModConfigSpec.DoubleValue PEDAL_INPUT_GAMMA = BUILDER
            .comment("Exponent applied to analog throttle and brake (trigger) input. 1.0 = linear;",
                    "~1.8 and the first half of the signal only asks for ~30% power, easier to feather the throttle")
            .defineInRange("pedalInputGamma", 1.8, 1.0, 4.0);

    public static final ModConfigSpec.BooleanValue ENABLE_ADVANCED_INPUT = BUILDER
            .comment("Enable Racing Wheel / Pedal support, or any other advanced controller",
                    "Leave off if you only use standard gamepads. Default is true")
            .define("enableAdvancedInput", true);

    public static final ModConfigSpec.DoubleValue ADVANCED_INPUT_DEADZONE = BUILDER
            .comment("Advanced input only; default 0.05")
            .defineInRange("advancedInputDeadzone", 0.05, 0.0, 0.6);

    public static final ModConfigSpec.DoubleValue STICK_DEADZONE = BUILDER
            .comment("Standard gamepad only; left/right thumbstick deadzone, default 0.10")
            .defineInRange("stickDeadzone", 0.10, 0.0, 0.6);

    public static final ModConfigSpec.DoubleValue TRIGGER_DEADZONE = BUILDER
            .comment("Standard gamepad only; left/right trigger deadzone, default 0.10")
            .defineInRange("triggerDeadzone", 0.10, 0.0, 0.6);

    public static final ModConfigSpec.DoubleValue STEERING_ANTI_ACKERMANN = BUILDER
            .comment("F1 cars use something called \"Anti-Ackermann\" steering",
                    "0 = both front wheels turn the same angle (parallel, typical steering)",
                    "Positive = anti-ackermann, so the outer wheel in a corner turns sharper than the inner,",
                    "Default 0.5.")
            .defineInRange("steeringAntiAckermann", 0.5, -1.0, 1.0);

    public static final ModConfigSpec.BooleanValue ENABLE_MOUSE_INPUT = BUILDER
            .comment("Enable the mouse to be used for inputs, like steering",
                    "The camera will be locked while driving if this is true, so hold ALT (rebindable)",
                    " to \"Free Look\", to move the camera while driving",
                    "Default false")
            .define("enableMouseInput", false);

    public static final ModConfigSpec.BooleanValue MOUSE_ABSOLUTE_MODE = BUILDER
            .comment("true = ABSOLUTE: mouse offset from center is the steer angle and holds",
                    "false = VELOCITY: mouse movement speed",
                    "Default true")
            .define("mouseAbsoluteMode", true);

    public static final ModConfigSpec.DoubleValue MOUSE_SENSITIVITY = BUILDER
            .comment("Mouse steering sensitivity",
                    "Default 1.0")
            .defineInRange("mouseSensitivity", 1.0, 0.05, 10.0);

    public static final ModConfigSpec.DoubleValue MOUSE_DEADZONE = BUILDER
            .comment("Default 0.0")
            .defineInRange("mouseDeadzone", 0.0, 0.0, 0.6);

    static { BUILDER.pop(); }

    // =========================================================================
    // ANIMATION
    // =======================================================================
    static { BUILDER.push("animation"); }

    public static final ModConfigSpec.DoubleValue STEERING_WHEEL_MAX_ANGLE = BUILDER
            .comment("How far the steering wheel rim turns at full lock, in degrees, each way",
                    "The wheel animates according to the scaled analog input",
                    "This number is purely cosmetic, default 200 degrees")
            .defineInRange("steeringWheelMaxAngle", 200.0, 30.0, 1080.0);

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
