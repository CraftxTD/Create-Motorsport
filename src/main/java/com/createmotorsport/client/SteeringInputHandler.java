package com.createmotorsport.client;

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

        // esc to quit
        if (key == GLFW.GLFW_KEY_ESCAPE) {
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

        int mask = 0;
        for (int i = 0; i < activeKeyCodes.length; i++) {
            if (activeKeyCodes[i] >= 0 && HELD_KEYS.contains(activeKeyCodes[i])) {
                mask |= (1 << i);
            }
        }

        if (mask != lastSentMask) {
            lastSentMask = mask;
            PacketDistributor.sendToServer(new SteeringInputPacket(activePos, mask));
        }

        showActionBar(player, mask);
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (activePos != null && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            stop();
        }
    }

    private static void showActionBar(LocalPlayer player, int mask) {
        StringBuilder sb = new StringBuilder("Driving, [esc] to quit");
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
        HELD_KEYS.clear();
        lastSentMask = -1;
        if (pos != null && Minecraft.getInstance().player != null) {
            PacketDistributor.sendToServer(new SteeringInputPacket(pos, 0));
            PacketDistributor.sendToServer(new SetDrivingPacket(pos, false));
        }
    }
}
