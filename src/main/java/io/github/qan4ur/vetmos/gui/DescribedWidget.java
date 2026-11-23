package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;

import java.util.List;

/** A setting widget with its description always visible underneath as small grey text. */
public class DescribedWidget extends Widget {
    private static final float SCALE = 0.75f;
    private static final int LINE_H = 7;

    final SettingWidget<?> inner;
    private String cachedText;
    private int cachedWidth = -1;
    private List<OrderedText> lines = List.of();

    public DescribedWidget(SettingWidget<?> inner) {
        this.inner = inner;
    }

    @Override
    public boolean visible() {
        return inner.visible();
    }

    private List<OrderedText> lines() {
        String text = inner.setting().displayDescription();
        int width = Math.max(40, w - 2);
        if (text == null || text.isBlank()) return List.of();
        if (!text.equals(cachedText) || width != cachedWidth) {
            cachedText = text;
            cachedWidth = width;
            lines = Paint.wrap(text, Math.round(width / SCALE));
        }
        return lines;
    }

    @Override
    public int height() {
        int n = lines().size();
        return inner.height() + (n > 0 ? n * LINE_H + 3 : 0) + 4;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        inner.x = x;
        inner.y = y;
        inner.w = w;
        inner.render(ctx, mx, my, tip);
        int ty = y + inner.height() + 1;
        var m = ctx.getMatrices();
        for (OrderedText line : lines()) {
            m.pushMatrix();
            m.translate(x, ty);
            m.scale(SCALE, SCALE);
            Paint.line(ctx, line, 0, 0, Palette.TEXT_MUTED);
            m.popMatrix();
            ty += LINE_H;
        }
        // thin separator between settings
        Paint.rect(ctx, x, y + height() - 2, w, 1, 0x12FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return inner.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        inner.mouseReleased(mx, my, button);
    }

    @Override
    public void mouseDragged(double mx, double my, int button) {
        inner.mouseDragged(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        return inner.mouseScrolled(mx, my, amount);
    }
}
