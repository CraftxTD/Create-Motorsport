package com.createmotorsport.client;

// copied from Universal Keyboard Controller, where I copied the idea from some other place
public final class GamepadCodes {

    private GamepadCodes() {
    }

    public static final int BASE = 2000;
    public static final int RANGE = 200;

    private static final int BUTTON_OFF = 0;   // 0..14
    public static final int BUTTON_COUNT = 15;
    private static final int LT_OFF = 20;
    private static final int RT_OFF = 21;
    private static final int STICK_OFF = 30;   // 30..37 (8 directions)

    public static final int TRIGGER_LT = BASE + LT_OFF;
    public static final int TRIGGER_RT = BASE + RT_OFF;

    // GLFW gamepad axis indices for the standard mapping
    public static final int AXIS_LEFT_X = 0;
    public static final int AXIS_LEFT_Y = 1;
    public static final int AXIS_RIGHT_X = 2;
    public static final int AXIS_RIGHT_Y = 3;
    public static final int AXIS_LT = 4;
    public static final int AXIS_RT = 5;

    private static int offset(int code) {
        return code - BASE;
    }

    public static boolean isGamepadCode(int code) {
        return code >= BASE && code < BASE + RANGE;
    }

    public static int button(int i) {
        return BASE + BUTTON_OFF + i;
    }

    public static int stick(int k) {
        return BASE + STICK_OFF + k; // k 0..7
    }

    public static boolean isButton(int code) {
        int o = offset(code);
        return o >= BUTTON_OFF && o < BUTTON_OFF + BUTTON_COUNT;
    }

    public static boolean isTrigger(int code) {
        int o = offset(code);
        return o == LT_OFF || o == RT_OFF;
    }

    public static boolean isStick(int code) {
        int o = offset(code);
        return o >= STICK_OFF && o < STICK_OFF + 8;
    }

    // Triggers and sticks analog 0-1 but buttons cant, just on/off
    public static boolean isAnalogCode(int code) {
        return isGamepadCode(code) && (isTrigger(code) || isStick(code));
    }

    public static int buttonIndex(int code) {
        return offset(code) - BUTTON_OFF;
    }

    public static int triggerAxis(int code) {
        return offset(code) == LT_OFF ? AXIS_LT : AXIS_RT;
    }

    // Stick axis codes
    public static int stickAxis(int code) {
        return switch ((offset(code) - STICK_OFF) / 2) {
            case 0 -> AXIS_LEFT_X;
            case 1 -> AXIS_LEFT_Y;
            case 2 -> AXIS_RIGHT_X;
            default -> AXIS_RIGHT_Y;
        };
    }

    public static boolean stickPositive(int code) {
        return ((offset(code) - STICK_OFF) & 1) == 0;
    }

    // Short label for the bind button in the menu
    // ↑→↓←🎮🕹, I'll do the controller/joystick emoji prefix when I add the advanced controller input next
    public static String name(int code) {
        if (!isGamepadCode(code)) {
            return "?" + code;
        }
        if (isButton(code)) {
            return switch (buttonIndex(code)) {
                case 0 -> "A"; case 1 -> "B"; case 2 -> "X"; case 3 -> "Y";
                case 4 -> "LB"; case 5 -> "RB"; case 6 -> "Back"; case 7 -> "Start";
                case 8 -> "Guide"; case 9 -> "LStick"; case 10 -> "RStick";
                case 11 -> "D↑"; case 12 -> "D→"; case 13 -> "D↓"; case 14 -> "D←";
                default -> "Btn" + buttonIndex(code);
            };
        }
        if (code == TRIGGER_LT) {
            return "LT";
        }
        if (code == TRIGGER_RT) {
            return "RT";
        }
        return switch (offset(code) - STICK_OFF) {
            case 0 -> "LS→"; case 1 -> "LS←"; case 2 -> "LS↓"; case 3 -> "LS↑";
            case 4 -> "RS→"; case 5 -> "RS←"; case 6 -> "RS↓"; case 7 -> "RS↑";
            default -> "Stick";
        };
    }
}
