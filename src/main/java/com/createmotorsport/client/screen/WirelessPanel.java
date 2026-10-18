package com.createmotorsport.client.screen;

import com.createmotorsport.client.widget.FlatButton;
import com.createmotorsport.client.widget.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WirelessPanel {

    public static final int WIDTH = 176;

    private WirelessPanel() {
    }

    public static FlatButton back(Screen screen, int left, int top, int width, Runnable reopen) {
        return new FlatButton(left + width - 8 - 40, top + 4, 40, 11, Component.literal("Back"), Theme.NEUTRAL,
                b -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.closeContainer();
                    }
                    reopen.run();
                });
    }

    public static void frame(GuiGraphics g, int l, int t, int w, int h, int invX, int invY) {
        g.fill(l, t, l + w, t + h, Theme.PANEL);
        g.fill(l, t, l + w, t + 1, Theme.BORDER);
        g.fill(l, t + h - 1, l + w, t + h, Theme.BORDER);
        g.fill(l, t, l + 1, t + h, Theme.BORDER);
        g.fill(l + w - 1, t, l + w, t + h, Theme.BORDER);
        g.fill(l + invX - 4, t + invY - 2, l + invX + 9 * 18 + 4, t + h - 4, Theme.INSET);
    }

    public static void row(GuiGraphics g, Font font, int l, int y, int w, String label, int ax, int bx) {
        g.fill(l + 8, y - 2, l + w - 8, y + 16, Theme.ROW);
        g.drawString(font, label, l + 12, y + 4, Theme.TEXT, false);
        slot(g, l + ax, y, Theme.FREQ_A);
        slot(g, l + bx, y, Theme.FREQ_B);
    }

    public static void slot(GuiGraphics g, int x, int y, int tint) {
        g.fill(x - 1, y - 1, x + 17, y, Theme.SLOT_EDGE);
        g.fill(x - 1, y, x, y + 17, Theme.SLOT_EDGE);
        g.fill(x, y, x + 16, y + 16, Theme.SLOT);
        g.fill(x, y, x + 16, y + 16, tint);
        g.fill(x + 16, y - 1, x + 17, y + 17, Theme.SLOT_EDGE);
        g.fill(x - 1, y + 16, x + 17, y + 17, Theme.SLOT_EDGE);
    }
}
