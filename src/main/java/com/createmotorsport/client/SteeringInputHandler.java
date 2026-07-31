package com.createmotorsport.client;

import com.createmotorsport.Config;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity.SteeringControl;
import com.createmotorsport.network.SetDrivingPacket;
import com.createmotorsport.network.SteeringInputPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;


// Input handling copied from universal keyboard mod
public final class SteeringInputHandler {
    private static final Set<Integer> HELD_KEYS = new HashSet<>();
    private static BlockPos activePos;
    private static int[] activeKeyCodes = new int[0];
    private static int lastSentMask = -1;
    private static int lastThrottle = -1;
    private static int lastBrake = -1;
    private static int lastSteer = -999;

    // Fake steering input for keypresses to apply a gamma too, so they can have an assist too
    private static float keySteer = 0.0f;

    private SteeringInputHandler() {
    }

    public static boolean isDriving() {
        return activePos != null;
    }

    public static BlockPos getActivePos() {
        return activePos;
    }


    // Client: start capture for wheel at (pos), called by menu button
    public static void startDriving(BlockPos pos) {
        activePos = pos.immutable();
        HELD_KEYS.clear();
        lastSentMask = -1;
        lastThrottle = -1;
        lastBrake = -1;
        lastSteer = -999;
        keySteer = 0.0f;
        PacketDistributor.sendToServer(new SetDrivingPacket(activePos, true));
    }

    public static void onKeyInput(InputEvent.Key event) {
        if (activePos == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        int key = event.getKey();
        int action = event.getAction();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return;
        }
        if (action == GLFW.GLFW_PRESS && MotorsportKeybinds.isExit(key, event.getScanCode())) {
            stop();
            return;
        }
        if (MotorsportKeybinds.isFreeLook(key, event.getScanCode())) {
            return;
        }
        // F1-F12 and slash key passthrough for POV, chat, and other keybinds
        if (isSafePassthroughKey(key)) {
            return;
        }

        // Record keys bound to a control so the tick can build the pressed-control mask
        if (isBound(key)) {
            if (action == GLFW.GLFW_PRESS) {
                HELD_KEYS.add(key);
            } else if (action == GLFW.GLFW_RELEASE) {
                HELD_KEYS.remove(key);
            }
        }
        // Capture the rest of the keyboard so they can be used to control car and nothing else
        if (action != GLFW.GLFW_RELEASE) {
            suppressVanillaKey(mc, key, event.getScanCode());
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (activePos == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stop();
            return;
        }
        if (!(mc.level.getBlockEntity(activePos) instanceof SteeringWheelBlockEntity wheel) || wheel.isRemoved()) {
            stop();
            return;
        }

        activeKeyCodes = new int[SteeringWheelBlockEntity.CONTROLS.length];
        for (int i = 0; i < activeKeyCodes.length; i++) {
            activeKeyCodes[i] = wheel.getKeyCode(i);
        }

        GamepadInput.poll();
        MouseInput.poll();

        // Lock the camera while driving with the mouse
        boolean mouseLock = MouseInput.enabled() && hasMouseControl() && !MotorsportKeybinds.freeLookHeld();
        MouseSteerCamera.update(mouseLock);

        int mask = 0;
        for (int i = 0; i < activeKeyCodes.length; i++) {
            if (isControlDown(activeKeyCodes[i])) {
                mask |= (1 << i);
            }
        }


        // I tried to do a fake steering input for key presses, so that there is something for a gamma to apply to
        // so far it seems like its working well enough
        float pedalGamma = (float) Config.PEDAL_INPUT_GAMMA.getAsDouble();
        float steerGamma = (float) Config.STEER_INPUT_GAMMA.getAsDouble();
        float throttle = (float) Math.pow(strength(SteeringControl.THROTTLE), pedalGamma);
        float brake = (float) Math.pow(strength(SteeringControl.BRAKE), pedalGamma);
        float steerKeyRamp = (float) Config.KEYPRESS_STEERING_RAMP.getAsDouble();

        float steerTarget = strength(SteeringControl.STEER_LEFT) - strength(SteeringControl.STEER_RIGHT);
        float steerInput;
        if (isAnalogSteer()) {
            steerInput = steerTarget;
            keySteer = steerTarget;
        } else {
            if (steerTarget > keySteer) {
                keySteer = Math.min(steerTarget, keySteer + steerKeyRamp);
            } else if (steerTarget < keySteer) {
                keySteer = Math.max(steerTarget, keySteer - steerKeyRamp);
            }
            steerInput = keySteer;
        }
        float steer = (float) (Math.signum(steerInput) * Math.pow(Math.abs(steerInput), steerGamma));

        int throttlePct = Math.round(Math.min(1.0F, throttle) * 100.0F);
        int brakePct = Math.round(Math.min(1.0F, brake) * 100.0F);
        int steerPct = Math.round(Math.max(-1.0F, Math.min(1.0F, steer)) * 100.0F);

        if (mask != lastSentMask || Math.abs(throttlePct - lastThrottle) >= 2
                || Math.abs(brakePct - lastBrake) >= 2 || Math.abs(steerPct - lastSteer) >= 2) {
            lastSentMask = mask;
            lastThrottle = throttlePct;
            lastBrake = brakePct;
            lastSteer = steerPct;
            PacketDistributor.sendToServer(new SteeringInputPacket(activePos, mask, throttlePct, brakePct, steerPct));
        }

        showActionBar(player, mask);
    }

    private static boolean isControlDown(int code) {
        if (code < 0) {
            return false;
        }
        if (GamepadCodes.isGamepadCode(code)) {
            return GamepadInput.isDown(code);
        }
        if (MouseCodes.isMouseCode(code)) {
            return MouseInput.isDown(code);
        }
        return HELD_KEYS.contains(code);
    }

    private static boolean hasMouseControl() {
        for (int code : activeKeyCodes) {
            if (MouseCodes.isMouseCode(code)) {
                return true;
            }
        }
        return false;
    }


    // True analog steering as opposed to the fake one used for keypress
    private static boolean isAnalogSteer() {
        return isAnalogControl(SteeringControl.STEER_LEFT) || isAnalogControl(SteeringControl.STEER_RIGHT);
    }

    private static boolean isAnalogControl(SteeringControl control) {
        int idx = control.ordinal();
        if (idx >= activeKeyCodes.length) {
            return false;
        }
        int code = activeKeyCodes[idx];
        return code >= 0 && (GamepadCodes.isAnalogCode(code) || MouseCodes.isAnalog(code));
    }

    // analog strength, keypress or button is just 1
    private static float strength(SteeringControl control) {
        int idx = control.ordinal();
        if (idx >= activeKeyCodes.length) {
            return 0.0F;
        }
        int code = activeKeyCodes[idx];
        if (code < 0) {
            return 0.0F;
        }
        if (GamepadCodes.isGamepadCode(code)) {
            return GamepadInput.analogMagnitude(code);
        }
        if (MouseCodes.isMouseCode(code)) {
            return MouseInput.analogMagnitude(code);
        }
        return HELD_KEYS.contains(code) ? 1.0F : 0.0F;
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (activePos != null && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            stop();
        }
    }

    private static void showActionBar(LocalPlayer player, int mask) {
        StringBuilder sb = new StringBuilder("Driving, [" + MotorsportKeybinds.exitKeyName().getString() + "] to quit");
        boolean first = true;
        for (SteeringControl control : SteeringWheelBlockEntity.CONTROLS) {
            if ((mask & (1 << control.ordinal())) != 0) {
                sb.append(first ? " — " : ", ");
                sb.append(control.getDisplayName());
                first = false;
            }
        }
        player.displayClientMessage(Component.literal(sb.toString()), true);
    }

    private static boolean isSafePassthroughKey(int keyCode) {
        return (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12)
                || keyCode == GLFW.GLFW_KEY_SLASH;
    }

    private static boolean isBound(int key) {
        for (int code : activeKeyCodes) {
            if (code == key) {
                return true;
            }
        }
        return false;
    }

    private static void suppressVanillaKey(Minecraft mc, int key, int scanCode) {
        for (KeyMapping mapping : mc.options.keyMappings) {
            if (mapping.matches(key, scanCode)) {
                mapping.consumeClick();
                mapping.setDown(false);
            }
        }
    }

    // Client: stop capturing and tell server we arent driving
    public static void stop() {
        BlockPos pos = activePos;
        activePos = null;
        activeKeyCodes = new int[0];
        MouseSteerCamera.reset();
        HELD_KEYS.clear();
        lastSentMask = -1;
        lastThrottle = -1;
        lastBrake = -1;
        lastSteer = -999;
        keySteer = 0.0f;
        if (pos != null && Minecraft.getInstance().player != null) {
            PacketDistributor.sendToServer(new SteeringInputPacket(pos, 0, 0, 0, 0));
            PacketDistributor.sendToServer(new SetDrivingPacket(pos, false));
        }
    }
}
