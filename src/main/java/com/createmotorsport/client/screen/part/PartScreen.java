package com.createmotorsport.client.screen.part;

import com.createmotorsport.client.screen.tune.TuneScreen;
import com.createmotorsport.client.screen.tune.TuneSource;
import com.createmotorsport.client.widget.FlatButton;
import com.createmotorsport.client.widget.HoverTip;
import com.createmotorsport.client.widget.Theme;
import com.createmotorsport.network.OpenWirelessPacket;
import com.createmotorsport.network.PartStatusRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class PartScreen extends Screen {

    protected static final int W = 220;
    protected static final int PAD = 8;
    protected static final int BTN_H = 14;
    protected static final int GAP = 4;
    private static final int STATUS_LINES = 6;
    private static final int LINE_H = 11;
    private static final int POLL_TICKS = 10;

    protected final BlockPos pos;
    protected final boolean canTune;
    protected int left;
    protected int top;
    private int panelH;
    private int buttonsY;
    private int ticks;

    protected PartScreen(Component title, BlockPos pos, boolean canTune) {
        super(title);
        this.pos = pos;
        this.canTune = canTune;
    }

    protected abstract List<List<Button>> rows();

    protected record Button(java.util.function.Supplier<Component> label, int color, Runnable onPress,
                            boolean enabled, @Nullable Component tip) {

        protected Button(java.util.function.Supplier<Component> label, int color, Runnable onPress,
                         boolean enabled) {
            this(label, color, onPress, enabled, null);
        }

        protected Button(java.util.function.Supplier<Component> label, int color, Runnable onPress) {
            this(label, color, onPress, true, null);
        }

        protected Button withTip(String tip) {
            return new Button(label, color, onPress, enabled, Component.literal(tip));
        }
    }

    protected Button tune() {
        return new Button(() -> Component.literal("Tune"), Theme.ACCENT, () -> minecraft.setScreen(
                new TuneScreen(this, Component.literal("Tune: ").append(title), TuneSource.block(pos), canTune)));
    }

    protected Button wireless() {
        return new Button(() -> Component.literal("Redstone Links (optional)"), Theme.NEUTRAL,
                () -> PacketDistributor.sendToServer(new OpenWirelessPacket(pos)));
    }

    @Override
    protected void init() {
        List<List<Button>> rows = rows();
        int statusH = STATUS_LINES * LINE_H + 6;
        labels.clear();
        panelH = 20 + statusH + PAD + rows.size() * (BTN_H + GAP) + PAD - GAP;
        left = (this.width - W) / 2;
        top = (this.height - panelH) / 2;
        buttonsY = top + 20 + statusH + PAD;
        int y = buttonsY;
        for (List<Button> row : rows) {
            int n = row.size();
            int w = (W - 2 * PAD - (n - 1) * GAP) / n;
            int x = left + PAD;
            for (Button b : row) {
                FlatButton widget = new FlatButton(x, y, w, BTN_H, b.label().get(), b.color(), fb -> b.onPress().run());
                widget.active = b.enabled();
                widget.hoverTip(b.tip());
                addRenderableWidget(widget);
                labels.add(new java.util.AbstractMap.SimpleEntry<>(widget, b));
                x += w + GAP;
            }
            y += BTN_H + GAP;
        }
        PacketDistributor.sendToServer(new PartStatusRequestPacket(pos));
    }

    private final List<java.util.Map.Entry<FlatButton, Button>> labels = new java.util.ArrayList<>();

    @Override
    public void tick() {
        super.tick();
        if (++ticks % POLL_TICKS == 0) {
            PacketDistributor.sendToServer(new PartStatusRequestPacket(pos));
        }
        for (var e : labels) {
            e.getKey().setMessage(e.getValue().label().get());
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) == null) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, title, left + PAD, top + 6, Theme.TEXT, false);
        int y = top + 22;
        List<Component> lines = PartScreens.statusFor(pos);
        if (lines.isEmpty()) {
            g.drawString(font, "...", left + PAD + 4, y + 2, Theme.TEXT_MUTED, false);
        }
        for (int i = 0; i < Math.min(STATUS_LINES, lines.size()); i++) {
            g.drawString(font, lines.get(i), left + PAD + 4, y + 2 + i * LINE_H, Theme.READOUT, false);
        }
        HoverTip.forHovered(g, font, children(), mx, my);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(left, top, left + W, top + panelH, Theme.PANEL);
        g.fill(left, top, left + W, top + 1, Theme.BORDER);
        g.fill(left, top + panelH - 1, left + W, top + panelH, Theme.BORDER);
        g.fill(left, top, left + 1, top + panelH, Theme.BORDER);
        g.fill(left + W - 1, top, left + W, top + panelH, Theme.BORDER);
        g.fill(left + PAD, top + 20, left + W - PAD, buttonsY - PAD, Theme.INSET);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    protected <T extends BlockEntity> T blockEntity(Class<T> type) {
        BlockEntity be = minecraft != null && minecraft.level != null ? minecraft.level.getBlockEntity(pos) : null;
        return type.isInstance(be) ? type.cast(be) : null;
    }
}
