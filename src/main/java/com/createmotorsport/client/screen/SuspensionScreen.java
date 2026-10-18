package com.createmotorsport.client.screen;

import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.SteerChannel;
import com.createmotorsport.client.screen.part.PartScreens;
import com.createmotorsport.client.widget.Theme;
import com.createmotorsport.menu.PartKind;
import com.createmotorsport.menu.SuspensionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspensionScreen extends AbstractContainerScreen<SuspensionMenu> {

    public SuspensionScreen(SuspensionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WirelessPanel.WIDTH;
        this.imageHeight = SuspensionMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = SuspensionMenu.INV_Y - 12;
        this.inventoryLabelX = SuspensionMenu.INV_X;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(WirelessPanel.back(this, leftPos, topPos, imageWidth,
                () -> PartScreens.open(menu.getSuspensionPos(), PartKind.SUSPENSION, menu.canTune())));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        WirelessPanel.frame(g, leftPos, topPos, imageWidth, imageHeight, SuspensionMenu.INV_X, SuspensionMenu.INV_Y);
        for (SteerChannel channel : SuspensionBlockEntity.CHANNELS) {
            WirelessPanel.row(g, font, leftPos,
                    topPos + SuspensionMenu.CHANNEL_Y0 + channel.ordinal() * SuspensionMenu.CHANNEL_ROW_H, imageWidth,
                    channel.getDisplayName(), SuspensionMenu.CHANNEL_SLOT_A_X, SuspensionMenu.CHANNEL_SLOT_B_X);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, Theme.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Theme.TEXT_DIM, false);
    }
}
