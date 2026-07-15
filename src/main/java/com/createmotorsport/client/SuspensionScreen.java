package com.createmotorsport.client;

import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.SteerChannel;
import com.createmotorsport.menu.SuspensionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


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

        int invY = t + SuspensionMenu.INV_Y;
        g.fill(l + SuspensionMenu.INV_X - 4, invY - 2, l + w - 8, t + h - 4, 0xFF222222);
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
