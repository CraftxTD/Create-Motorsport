package com.createmotorsport.client;

import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity.SteeringControl;
import com.createmotorsport.menu.SteeringWheelMenu;
import com.createmotorsport.network.SetDriveModePacket;
import com.createmotorsport.network.SetSteeringKeyPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// Screen in the same style as my keyboard mod that I could paste over and get working quickly.
// temporary, will replace with more themed one
@OnlyIn(Dist.CLIENT)
public class SteeringWheelScreen extends AbstractContainerScreen<SteeringWheelMenu> {
    private static final int FREQ_A_TINT = 0x55FF3333;
    private static final int FREQ_B_TINT = 0x553333FF;
    private static final int PANEL = 0xFF1A1A1A;
    private static final int BORDER = 0xFF555555;
    private static final int ROW = 0xFF2A2A2A;
    private static final int BIND = 0xFF3A3A3A;
    private static final int BIND_ACTIVE = 0xFF4A6A4A;
    private static final int SLOT_EDGE = 0xFF3D3D3D;
    private static final int SLOT_BASE = 0xFF1F1F1F;
    private static final int DRIVE_START = 0xFF356A35;
    private static final int DRIVE_STOP = 0xFF6A3535;

    // "Start / Stop Driving" button's offsets from the panel's top-left
    private static final int BTN_W = 74;
    private static final int BTN_H = 14;
    private static final int BTN_Y = 4;
    // "Drive: FWD/RWD/AWD" button, just below it
    private static final int MODE_Y = BTN_Y + BTN_H + 2;
    private static final int MODE_COLOR = 0xFF3A4A5A;

    private int capturingControl = -1;

    public SteeringWheelScreen(SteeringWheelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SteeringWheelMenu.WIDTH;
        this.imageHeight = SteeringWheelMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = SteeringWheelMenu.INV_Y - 12;
        this.inventoryLabelX = SteeringWheelMenu.INV_X;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int l = leftPos;
        int t = topPos;
        int w = imageWidth;
        int h = imageHeight;

        g.fill(l, t, l + w, t + h, PANEL);
        g.fill(l, t, l + w, t + 1, BORDER);
        g.fill(l, t + h - 1, l + w, t + h, BORDER);
        g.fill(l, t, l + 1, t + h, BORDER);
        g.fill(l + w - 1, t, l + w, t + h, BORDER);

        for (SteeringControl control : SteeringWheelBlockEntity.CONTROLS) {
            int i = control.ordinal();
            int y = t + SteeringWheelMenu.rowY(i);
            int cx = l + SteeringWheelMenu.columnX(i);
            g.fill(cx, y - 2, cx + SteeringWheelMenu.COLUMN_W, y + 16, ROW);
            g.drawString(font, control.getDisplayName(), l + SteeringWheelMenu.labelX(i), y + 4, 0xFFFFFFFF, false);

            int bx = l + SteeringWheelMenu.bindX(i);
            g.fill(bx, y - 1, bx + SteeringWheelMenu.BIND_W, y + 15, capturingControl == i ? BIND_ACTIVE : BIND);
            String label = capturingControl == i ? "press key" : keyName(keyCodeFor(i));
            g.drawString(font, label, bx + 3, y + 4, 0xFFDDDDDD, false);

            drawSlot(g, l + SteeringWheelMenu.slotAX(i), y, FREQ_A_TINT);
            drawSlot(g, l + SteeringWheelMenu.slotBX(i), y, FREQ_B_TINT);
        }

        int invY = t + SteeringWheelMenu.INV_Y;
        g.fill(l + SteeringWheelMenu.INV_X - 4, invY - 2, l + SteeringWheelMenu.INV_X + 9 * 18 + 4, t + h - 4, 0xFF222222);

        boolean driving = isDriving();
        int bx = l + w - BTN_W - 6;
        int by = t + BTN_Y;
        g.fill(bx, by, bx + BTN_W, by + BTN_H, driving ? DRIVE_STOP : DRIVE_START);
        g.fill(bx, by, bx + BTN_W, by + 1, BORDER);
        g.fill(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, BORDER);
        String label = driving ? "Stop Driving" : "Start Driving";
        g.drawString(font, label, bx + (BTN_W - font.width(label)) / 2, by + 3, 0xFFFFFFFF, false);

        int my2 = t + MODE_Y;
        g.fill(bx, my2, bx + BTN_W, my2 + BTN_H, MODE_COLOR);
        g.fill(bx, my2, bx + BTN_W, my2 + 1, BORDER);
        g.fill(bx, my2 + BTN_H - 1, bx + BTN_W, my2 + BTN_H, BORDER);
        String modeLabel = "Drive: " + driveModeLabel();
        g.drawString(font, modeLabel, bx + (BTN_W - font.width(modeLabel)) / 2, my2 + 3, 0xFFFFFFFF, false);
    }

    private String driveModeLabel() {
        BlockPos pos = menu.getWheelPos();
        if (pos != null && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(pos) instanceof SteeringWheelBlockEntity wheel) {
            return wheel.getDriveMode().getLabel();
        }
        return "RWD";
    }

    private boolean modeButtonAt(double mx, double my) {
        int bx = leftPos + imageWidth - BTN_W - 6;
        int by = topPos + MODE_Y;
        return mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H;
    }

    private void drawSlot(GuiGraphics g, int x, int y, int tint) {
        g.fill(x - 1, y - 1, x + 17, y, SLOT_EDGE);
        g.fill(x - 1, y, x, y + 17, SLOT_EDGE);
        g.fill(x, y, x + 16, y + 16, SLOT_BASE);
        if (tint != 0) {
            g.fill(x, y, x + 16, y + 16, tint);
        }
        g.fill(x + 16, y - 1, x + 17, y + 17, SLOT_EDGE);
        g.fill(x - 1, y + 16, x + 17, y + 17, SLOT_EDGE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && driveButtonAt(mx, my)) {
            BlockPos pos = menu.getWheelPos();
            if (pos != null) {
                if (isDriving()) {
                    SteeringInputHandler.stop();
                } else {
                    // Starts client-side keyboard control capturing and closes menu
                    SteeringInputHandler.startDriving(pos);
                    this.onClose();
                }
            }
            return true;
        }
        if (button == 0 && modeButtonAt(mx, my)) {
            BlockPos pos = menu.getWheelPos();
            if (pos != null) {
                PacketDistributor.sendToServer(new SetDriveModePacket(pos));
            }
            return true;
        }
        int bindHit = bindButtonAt(mx, my);
        if (bindHit >= 0) {
            if (button == 1) {
                sendKey(bindHit, -1); // right-click clears
                capturingControl = -1;
            } else {
                capturingControl = capturingControl == bindHit ? -1 : bindHit;
                if (capturingControl >= 0) {
                    GamepadInput.beginCapture(); // baseline so held stick/trigger isn't grabbed instantly
                    MouseInput.beginCapture();
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // adds gamepad capture listening too
        if (capturingControl >= 0) {
            int code = GamepadInput.pollCapture();
            if (code < 0) {
                code = MouseInput.pollCapture();
            }
            if (code >= 0) {
                sendKey(capturingControl, code);
                capturingControl = -1;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingControl >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingControl = -1;
            } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                sendKey(capturingControl, -1);
                capturingControl = -1;
            } else {
                sendKey(capturingControl, keyCode);
                capturingControl = -1;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean driveButtonAt(double mx, double my) {
        int bx = leftPos + imageWidth - BTN_W - 6;
        int by = topPos + BTN_Y;
        return mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H;
    }

    private boolean isDriving() {
        BlockPos pos = menu.getWheelPos();
        return pos != null && SteeringInputHandler.isDriving() && pos.equals(SteeringInputHandler.getActivePos());
    }

    private int bindButtonAt(double mx, double my) {
        for (SteeringControl control : SteeringWheelBlockEntity.CONTROLS) {
            int i = control.ordinal();
            int y = topPos + SteeringWheelMenu.rowY(i);
            int bx = leftPos + SteeringWheelMenu.bindX(i);
            if (mx >= bx && mx < bx + SteeringWheelMenu.BIND_W && my >= y - 1 && my < y + 15) {
                return i;
            }
        }
        return -1;
    }

    private void sendKey(int control, int keyCode) {
        BlockPos pos = menu.getWheelPos();
        if (pos != null) {
            PacketDistributor.sendToServer(new SetSteeringKeyPacket(pos, control, keyCode));
        }
    }

    private int keyCodeFor(int control) {
        BlockPos pos = menu.getWheelPos();
        if (pos != null && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(pos) instanceof SteeringWheelBlockEntity wheel) {
            return wheel.getKeyCode(control);
        }
        return -1;
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0) {
            return "(none)";
        }
        if (GamepadCodes.isGamepadCode(keyCode)) {
            return GamepadCodes.name(keyCode);
        }
        if (MouseCodes.isMouseCode(keyCode)) {
            return MouseCodes.name(keyCode);
        }
        String glfwName = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfwName != null && !glfwName.isEmpty()) {
            return glfwName.toUpperCase();
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            default -> "K:" + keyCode;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFAAAAAA, false);
    }
}
