package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Drawing helpers for the Vetmos GUI: rounded rectangles made of non-overlapping strips (so translucent colors stay
 * even), soft glows, gradients, outlines and text. Every color is multiplied by the global {@link #alpha}, which the
 * screens use for their fade animation.
 */
public final class Paint {
    /** Global opacity multiplier (0..1). */
    public static float alpha = 1;

    private static final int MAX_R = 16;
    private static final int[][] INSETS = new int[MAX_R + 1][];

    static {
        for (int r = 0; r <= MAX_R; r++) {
            INSETS[r] = new int[r];
            for (int i = 0; i < r; i++) {
                double dy = r - i - 0.5;
                INSETS[r][i] = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
        }
    }

    private Paint() {}

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    /** Applies the global alpha to an ARGB color. */
    public static int a(int color) {
        if (alpha >= 1) return color;
        return Anim.alpha(color, alpha);
    }

    private static boolean invisible(int color) {
        return (color >>> 24) == 0;
    }

    public static void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int c = a(color);
        if (invisible(c)) return;
        ctx.fill(x, y, x + w, y + h, c);
    }

    public static void gradientV(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        ctx.fillGradient(x, y, x + w, y + h, a(top), a(bottom));
    }

    /** Horizontal gradient made of thin vertical strips. */
    public static void gradientH(DrawContext ctx, int x, int y, int w, int h, int left, int right) {
        if (w <= 0 || h <= 0) return;
        int step = Math.max(1, w / 48);
        for (int i = 0; i < w; i += step) {
            int sw = Math.min(step, w - i);
            rect(ctx, x + i, y, sw, h, Anim.mix(left, right, w <= 1 ? 0 : (i + sw / 2f) / (w - 1)));
        }
    }

    private static int[] insets(int r) {
        return INSETS[Math.max(0, Math.min(MAX_R, r))];
    }

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        roundGradient(ctx, x, y, w, h, r, color, color, true, true);
    }

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color, boolean top, boolean bottom) {
        roundGradient(ctx, x, y, w, h, r, color, color, top, bottom);
    }

    public static void roundGradient(DrawContext ctx, int x, int y, int w, int h, int r, int topColor, int bottomColor) {
        roundGradient(ctx, x, y, w, h, r, topColor, bottomColor, true, true);
    }

    /** Rounded rectangle with a vertical gradient; {@code top}/{@code bottom} choose which corners are rounded. */
    public static void roundGradient(DrawContext ctx, int x, int y, int w, int h, int r, int topColor, int bottomColor,
                                     boolean top, boolean bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        int[] in = insets(r);
        int rt = top ? r : 0, rb = bottom ? r : 0;
        if (rt + rb > h) {
            rt = h / 2;
            rb = h - rt;
        }
        float hh = Math.max(1, h - 1);
        for (int i = 0; i < rt; i++) {
            int ins = in[Math.min(i, in.length - 1)];
            rect(ctx, x + ins, y + i, w - ins * 2, 1, Anim.mix(topColor, bottomColor, i / hh));
        }
        int midH = h - rt - rb;
        if (midH > 0) {
            if (topColor == bottomColor) rect(ctx, x, y + rt, w, midH, topColor);
            else gradientV(ctx, x, y + rt, w, midH, Anim.mix(topColor, bottomColor, rt / hh), Anim.mix(topColor, bottomColor, (h - rb) / hh));
        }
        for (int i = 0; i < rb; i++) {
            int ins = in[Math.min(i, in.length - 1)];
            int row = h - 1 - i;
            rect(ctx, x + ins, y + row, w - ins * 2, 1, Anim.mix(topColor, bottomColor, row / hh));
        }
    }

    /** 1px outline that follows the rounded shape. */
    public static void outline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 1 || h <= 1) return;
        r = Math.max(0, Math.min(Math.min(r, MAX_R), Math.min(w, h) / 2));
        if (r == 0) {
            rect(ctx, x, y, w, 1, color);
            rect(ctx, x, y + h - 1, w, 1, color);
            rect(ctx, x, y + 1, 1, h - 2, color);
            rect(ctx, x + w - 1, y + 1, 1, h - 2, color);
            return;
        }
        int[] in = insets(r);
        // top and bottom lines
        rect(ctx, x + in[0], y, w - in[0] * 2, 1, color);
        rect(ctx, x + in[0], y + h - 1, w - in[0] * 2, 1, color);
        // corner rows
        for (int i = 1; i < r; i++) {
            int from = in[i], to = Math.max(in[i] + 1, in[i - 1]);
            int len = to - from;
            rect(ctx, x + from, y + i, len, 1, color);
            rect(ctx, x + w - to, y + i, len, 1, color);
            rect(ctx, x + from, y + h - 1 - i, len, 1, color);
            rect(ctx, x + w - to, y + h - 1 - i, len, 1, color);
        }
        // sides
        rect(ctx, x, y + r, 1, h - r * 2, color);
        rect(ctx, x + w - 1, y + r, 1, h - r * 2, color);
    }

    /** Soft glow / shadow around a rounded box: several expanding layers with falling alpha. */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int r, int color, int size) {
        int baseA = color >>> 24;
        if (baseA == 0 || size <= 0) return;
        for (int i = size; i >= 1; i--) {
            float f = 1 - (float) i / (size + 1);
            int c = Anim.withAlpha(color, Math.round(baseA * f * f / size * 2.2f));
            if ((c >>> 24) == 0) continue;
            outline(ctx, x - i, y - i, w + i * 2, h + i * 2, r + i, c);
        }
    }

    /** Filled disc. */
    public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
        round(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** Checkerboard used under translucent color swatches. */
    public static void checker(DrawContext ctx, int x, int y, int w, int h, int cell) {
        rect(ctx, x, y, w, h, 0xFFBDBDBD);
        for (int yy = 0; yy < h; yy += cell) {
            for (int xx = ((yy / cell) % 2) * cell; xx < w; xx += cell * 2) {
                rect(ctx, x + xx, y + yy, Math.min(cell, w - xx), Math.min(cell, h - yy), 0xFF7A7A7A);
            }
        }
    }

    // ---------------------------------------------------------------- text

    public static void text(DrawContext ctx, String s, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 4) return;
        ctx.drawTextWithShadow(font(), s, x, y, c);
    }

    public static void text(DrawContext ctx, Text s, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 4) return;
        ctx.drawTextWithShadow(font(), s, x, y, c);
    }

    public static void textNoShadow(DrawContext ctx, String s, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 4) return;
        ctx.drawText(font(), s, x, y, c, false);
    }

    public static void textCentered(DrawContext ctx, String s, int cx, int y, int color) {
        text(ctx, s, cx - font().getWidth(s) / 2, y, color);
    }

    public static void textCentered(DrawContext ctx, Text s, int cx, int y, int color) {
        text(ctx, s, cx - font().getWidth(s) / 2, y, color);
    }

    public static void textRight(DrawContext ctx, String s, int right, int y, int color) {
        text(ctx, s, right - font().getWidth(s), y, color);
    }

    /** Text scaled around its top-left corner. */
    public static void textScaled(DrawContext ctx, String s, float x, float y, float scale, int color) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        text(ctx, s, 0, 0, color);
        m.popMatrix();
    }

    public static int width(String s) {
        return font().getWidth(s);
    }

    /** Trims the text to the width, adding an ellipsis when cut. */
    public static String trim(String s, int maxWidth) {
        if (width(s) <= maxWidth) return s;
        return font().trimToWidth(s, Math.max(0, maxWidth - width("…"))) + "…";
    }

    public static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
