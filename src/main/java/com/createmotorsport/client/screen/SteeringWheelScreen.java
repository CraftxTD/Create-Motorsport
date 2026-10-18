package com.createmotorsport.client.screen;

import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity.SteeringControl;
import com.createmotorsport.client.screen.part.PartScreens;
import com.createmotorsport.client.widget.Theme;
import com.createmotorsport.menu.PartKind;
import com.createmotorsport.menu.SteeringWheelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SteeringWheelScreen extends AbstractContainerScreen<SteeringWheelMenu> {

    public SteeringWheelScreen(SteeringWheelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SteeringWheelMenu.WIDTH;
        this.imageHeight = SteeringWheelMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = SteeringWheelMenu.INV_Y - 12;
        this.inventoryLabelX = SteeringWheelMenu.INV_X;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(WirelessPanel.back(this, leftPos, topPos, imageWidth,
                () -> PartScreens.open(menu.getWheelPos(), PartKind.STEERING_WHEEL, false)));
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
        WirelessPanel.frame(g, l, t, imageWidth, imageHeight, SteeringWheelMenu.INV_X, SteeringWheelMenu.INV_Y);
        for (SteeringControl control : SteeringWheelBlockEntity.CONTROLS) {
            int i = control.ordinal();
            int y = t + SteeringWheelMenu.rowY(i);
            int cx = l + SteeringWheelMenu.columnX(i);
            g.fill(cx, y - 2, cx + SteeringWheelMenu.COLUMN_W, y + 16, Theme.ROW);
            g.drawString(font, control.getDisplayName(), l + SteeringWheelMenu.labelX(i), y + 4, Theme.TEXT, false);
            WirelessPanel.slot(g, l + SteeringWheelMenu.slotAX(i), y, Theme.FREQ_A);
            WirelessPanel.slot(g, l + SteeringWheelMenu.slotBX(i), y, Theme.FREQ_B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, Theme.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Theme.TEXT_DIM, false);
    }
}
