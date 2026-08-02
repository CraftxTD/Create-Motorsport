package com.createmotorsport.client;

import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.SteerChannel;
import com.createmotorsport.menu.SuspensionMenu;
import com.createmotorsport.network.AdjustLiftPacket;
import com.createmotorsport.network.ToggleAxleEndPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;


// Screens that I quickly made based on my keyboard mods style, we will replace with better ones
@OnlyIn(Dist.CLIENT)
public class SuspensionScreen extends AbstractContainerScreen<SuspensionMenu> {
    private static final int FREQ_A_TINT = 0x55FF3333;
    private static final int FREQ_B_TINT = 0x553333FF;
    private static final int PANEL = 0xFF1A1A1A;
    private static final int BORDER = 0xFF555555;
    private static final int ROW = 0xFF2A2A2A;
    private static final int SLOT_EDGE = 0xFF3D3D3D;
    private static final int SLOT_BASE = 0xFF1F1F1F;
    private static final int AXLE_FRONT = 0xFF35506A;
    private static final int AXLE_REAR = 0xFF6A5535;

    // toggle button for front vs rear axle
    private static final int BTN_W = 74;
    private static final int BTN_H = 14;
    private static final int BTN_X = 92;
    private static final int BTN_Y = 16;

    // lift height
    private static final int LIFT_Y = 34;
    private static final int LIFT_BTN_W = 14;
    private static final int LIFT_BTN_H = 14;
    private static final int LIFT_MINUS_X = 92;
    private static final int LIFT_PLUS_X = BTN_X + BTN_W - LIFT_BTN_W;
    private static final int LIFT_BTN_COLOR = 0xFF3A3A3A;

    public SuspensionScreen(SuspensionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = SuspensionMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = SuspensionMenu.INV_Y - 12;
        this.inventoryLabelX = SuspensionMenu.INV_X;
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

        for (SteerChannel channel : SuspensionBlockEntity.CHANNELS) {
            int y = t + SuspensionMenu.CHANNEL_Y0 + channel.ordinal() * SuspensionMenu.CHANNEL_ROW_H;
            g.fill(l + 8, y - 2, l + w - 8, y + 16, ROW);
            g.drawString(font, channel.getDisplayName(), l + 12, y + 4, 0xFFFFFFFF, false);
            drawSlot(g, l + SuspensionMenu.CHANNEL_SLOT_A_X, y, FREQ_A_TINT);
            drawSlot(g, l + SuspensionMenu.CHANNEL_SLOT_B_X, y, FREQ_B_TINT);
        }

        boolean front = isFrontAxle();
        int bx = l + BTN_X;
        int by = t + BTN_Y;
        g.fill(bx, by, bx + BTN_W, by + BTN_H, front ? AXLE_FRONT : AXLE_REAR);
        g.fill(bx, by, bx + BTN_W, by + 1, BORDER);
        g.fill(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, BORDER);
        String label = front ? "Front Axle" : "Rear Axle";
        g.drawString(font, label, bx + (BTN_W - font.width(label)) / 2, by + 3, 0xFFFFFFFF, false);

        // lift height
        int ly = t + LIFT_Y;
        g.drawString(font, "Ride Lift", l + 12, ly + 3, 0xFFAAAAAA, false);
        drawLiftButton(g, l + LIFT_MINUS_X, ly, "-");
        drawLiftButton(g, l + LIFT_PLUS_X, ly, "+");
        String liftLabel = getLiftSteps() + "/" + SuspensionBlockEntity.LIFT_STEPS;
        int valueLeft = l + LIFT_MINUS_X + LIFT_BTN_W;
        int valueRight = l + LIFT_PLUS_X;
        g.drawString(font, liftLabel, valueLeft + (valueRight - valueLeft - font.width(liftLabel)) / 2, ly + 3,
                0xFFFFFFFF, false);

        int invY = t + SuspensionMenu.INV_Y;
        g.fill(l + SuspensionMenu.INV_X - 4, invY - 2, l + w - 8, t + h - 4, 0xFF222222);
    }

    private boolean isFrontAxle() {
        BlockPos pos = menu.getSuspensionPos();
        if (pos != null && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(pos) instanceof SuspensionBlockEntity be) {
            return be.isFrontAxle();
        }
        return false;
    }

    private int getLiftSteps() {
        BlockPos pos = menu.getSuspensionPos();
        if (pos != null && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(pos) instanceof SuspensionBlockEntity be) {
            return be.getLiftSteps();
        }
        return 0;
    }

    private void drawLiftButton(GuiGraphics g, int x, int y, String sign) {
        g.fill(x, y, x + LIFT_BTN_W, y + LIFT_BTN_H, LIFT_BTN_COLOR);
        g.fill(x, y, x + LIFT_BTN_W, y + 1, BORDER);
        g.fill(x, y + LIFT_BTN_H - 1, x + LIFT_BTN_W, y + LIFT_BTN_H, BORDER);
        g.drawString(font, sign, x + (LIFT_BTN_W - font.width(sign)) / 2, y + 3, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bx = leftPos + BTN_X;
        int by = topPos + BTN_Y;
        if (button == 0 && mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H) {
            BlockPos pos = menu.getSuspensionPos();
            if (pos != null) {
                PacketDistributor.sendToServer(new ToggleAxleEndPacket(pos));
            }
            return true;
        }
        int ly = topPos + LIFT_Y;
        if (button == 0 && my >= ly && my < ly + LIFT_BTN_H) {
            int delta = 0;
            if (mx >= leftPos + LIFT_MINUS_X && mx < leftPos + LIFT_MINUS_X + LIFT_BTN_W) {
                delta = -1;
            } else if (mx >= leftPos + LIFT_PLUS_X && mx < leftPos + LIFT_PLUS_X + LIFT_BTN_W) {
                delta = 1;
            }
            if (delta != 0) {
                BlockPos pos = menu.getSuspensionPos();
                if (pos != null) {
                    PacketDistributor.sendToServer(new AdjustLiftPacket(pos, delta));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
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
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFFFFFFF, false);
        g.drawString(font, "Wireless steering", 8, 20, 0xFF888888, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFAAAAAA, false);
    }
}
