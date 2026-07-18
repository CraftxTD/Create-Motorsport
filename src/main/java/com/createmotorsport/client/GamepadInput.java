package com.createmotorsport.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;


public final class GamepadInput {

    private static final float STICK_THRESHOLD = 0.35F;
    private static final float TRIGGER_THRESHOLD = 0.15F;

    private static GLFWGamepadState state;
    private static boolean present;
    private static final boolean[] buttons = new boolean[GamepadCodes.BUTTON_COUNT];
    private static final float[] axes = new float[6];
    private static final Set<Integer> captureLast = new HashSet<>();

    private GamepadInput() {
    }

    public static void poll() {
        if (state == null) {
            state = GLFWGamepadState.create();
        }
        for (int d = GLFW.GLFW_JOYSTICK_1; d <= GLFW.GLFW_JOYSTICK_LAST; d++) {
            if (GLFW.glfwJoystickIsGamepad(d) && GLFW.glfwGetGamepadState(d, state)) {
                present = true;
                for (int i = 0; i < GamepadCodes.BUTTON_COUNT; i++) {
                    buttons[i] = state.buttons(i) != 0;
                }
                FloatBuffer ax = state.axes();
                for (int i = 0; i < axes.length; i++) {
                    axes[i] = ax.get(i);
                }
                return;
            }
        }
        present = false;
        for (int i = 0; i < GamepadCodes.BUTTON_COUNT; i++) {
            buttons[i] = false;
        }
        for (int i = 0; i < axes.length; i++) {
            axes[i] = 0.0F;
        }
    }

    public static boolean hasGamepad() {
        return present;
    }

    public static float analogMagnitude(int code) {
        if (!present || !GamepadCodes.isGamepadCode(code)) {
            return 0.0F;
        }
        if (GamepadCodes.isButton(code)) {
            int i = GamepadCodes.buttonIndex(code);
            return (i >= 0 && i < GamepadCodes.BUTTON_COUNT && buttons[i]) ? 1.0F : 0.0F;
        }
        if (GamepadCodes.isTrigger(code)) {
            float raw = (axes[GamepadCodes.triggerAxis(code)] + 1.0F) * 0.5F; // -1 rest to +1 pulled
            return raw <= TRIGGER_THRESHOLD ? 0.0F : ramp(raw, TRIGGER_THRESHOLD);
        }
        if (GamepadCodes.isStick(code)) {
            float v = axes[GamepadCodes.stickAxis(code)];
            float dir = GamepadCodes.stickPositive(code) ? v : -v;
            return dir <= STICK_THRESHOLD ? 0.0F : ramp(dir, STICK_THRESHOLD);
        }
        return 0.0F;
    }

    public static boolean isDown(int code) {
        return analogMagnitude(code) > 0.0F;
    }

    public static void beginCapture() {
        poll();
        captureLast.clear();
        captureLast.addAll(pressedSet());
    }

    public static int pollCapture() {
        poll();
        Set<Integer> now = pressedSet();
        int found = -1;
        for (int code : now) {
            if (!captureLast.contains(code)) {
                found = code;
                break;
            }
        }
        captureLast.clear();
        captureLast.addAll(now);
        return found;
    }

    private static Set<Integer> pressedSet() {
        Set<Integer> set = new HashSet<>();
        if (!present) {
            return set;
        }
        for (int i = 0; i < GamepadCodes.BUTTON_COUNT; i++) {
            if (buttons[i]) {
                set.add(GamepadCodes.button(i));
            }
        }
        if ((axes[GamepadCodes.AXIS_LT] + 1.0F) * 0.5F >= TRIGGER_THRESHOLD) {
            set.add(GamepadCodes.TRIGGER_LT);
        }
        if ((axes[GamepadCodes.AXIS_RT] + 1.0F) * 0.5F >= TRIGGER_THRESHOLD) {
            set.add(GamepadCodes.TRIGGER_RT);
        }
        addStick(set, axes[GamepadCodes.AXIS_LEFT_X], axes[GamepadCodes.AXIS_LEFT_Y], 0);
        addStick(set, axes[GamepadCodes.AXIS_RIGHT_X], axes[GamepadCodes.AXIS_RIGHT_Y], 4);
        return set;
    }

    private static void addStick(Set<Integer> set, float x, float y, int kBase) {
        if (x >= STICK_THRESHOLD) {
            set.add(GamepadCodes.stick(kBase));       // right (+x)
        }
        if (x <= -STICK_THRESHOLD) {
            set.add(GamepadCodes.stick(kBase + 1));   // left  (-x)
        }
        if (y >= STICK_THRESHOLD) {
            set.add(GamepadCodes.stick(kBase + 2));   // down  (+y)
        }
        if (y <= -STICK_THRESHOLD) {
            set.add(GamepadCodes.stick(kBase + 3));   // up    (-y)
        }
    }

    private static float ramp(float raw, float threshold) {
        float m = (raw - threshold) / (1.0F - threshold);
        return m < 0.0F ? 0.0F : Math.min(1.0F, m);
    }
}
