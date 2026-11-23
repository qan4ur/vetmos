package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;

import java.util.List;
import java.util.Objects;

/**
 * Hover tooltip with a short delay and a fade. During a frame the hovered element calls {@link #offer}; at the end
 * of the frame {@link #render} draws the box near the mouse.
 */
public final class Tooltip {
    private static final float DELAY = 0.35f;
    private static final int MAX_WIDTH = 180;

    private Object key, frameKey;
    private String text, frameText;
    private String title, frameTitle;
    private float hoverTime;
    private float fade;

    /** The element under the mouse offers its description for this frame. Later offers win. */
    public void offer(Object key, String title, String text) {
        if (text == null || text.isBlank()) return;
        frameKey = key;
        frameTitle = title;
        frameText = text;
    }

    public void offer(Object key, String text) {
        offer(key, null, text);
    }

    public void render(DrawContext ctx, int mx, int my, int screenW, int screenH) {
        if (frameKey == null || !Objects.equals(frameKey, key)) {
            hoverTime = 0;
            fade = 0;
        }
        key = frameKey;
        text = frameText;
        title = frameTitle;
        frameKey = null;
        frameText = null;
        frameTitle = null;
        if (key == null) return;

        hoverTime += Anim.dt;
        if (hoverTime < DELAY) return;
        fade = Anim.approach(fade, 1, 14);

        List<OrderedText> lines = Paint.wrap(text, MAX_WIDTH);
        int w = 0;
        for (OrderedText l : lines) w = Math.max(w, Paint.font().getWidth(l));
        if (title != null) w = Math.max(w, Paint.width(title));
        int lineH = 10;
        int h = lines.size() * lineH + (title != null ? 13 : 0) - 1;
        int bw = w + 12, bh = h + 10;

        int x = mx + 10, y = my + 12;
        if (x + bw > screenW - 4) x = mx - bw - 6;
        if (y + bh > screenH - 4) y = my - bh - 6;
        x = Math.max(4, x);
        y = Math.max(4, y);

        float old = Paint.alpha;
        Paint.alpha = old * fade;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0, (1 - fade) * 3);

        Paint.tooltipBox(ctx, x, y, bw, bh);

        int ty = y + 5;
        if (title != null) {
            Paint.text(ctx, title, x + 6, ty, Anim.mix(0xFFFFFFFF, Palette.accent(), 0.5f));
            ty += 13;
        }
        for (OrderedText l : lines) {
            Paint.line(ctx, l, x + 7, ty, 0xFFE6E6E6);
            ty += lineH;
        }

        m.popMatrix();
        Paint.alpha = old;
    }
}
