package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.List;

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

    /** Corner radius of every UI shape (the reference look: only slightly rounded). */
    public static final int UI_R = 2;

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        roundGradient(ctx, x, y, w, h, r, color, color, true, true);
    }

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color, boolean top, boolean bottom) {
        roundGradient(ctx, x, y, w, h, r, color, color, top, bottom);
    }

    public static void roundGradient(DrawContext ctx, int x, int y, int w, int h, int r, int topColor, int bottomColor) {
        roundGradient(ctx, x, y, w, h, r, topColor, bottomColor, true, true);
    }

    /** Rectangle with slightly rounded corners (radius clamped to {@link #UI_R}) and a vertical gradient. */
    public static void roundGradient(DrawContext ctx, int x, int y, int w, int h, int r, int topColor, int bottomColor,
                                     boolean top, boolean bottom) {
        roundRaw(ctx, x, y, w, h, Math.min(r, UI_R), topColor, bottomColor, top, bottom);
    }

    private static void roundRaw(DrawContext ctx, int x, int y, int w, int h, int r, int topColor, int bottomColor,
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

    /** 1px outline (radius clamped to {@link #UI_R}). */
    public static void outline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        outlineRaw(ctx, x, y, w, h, Math.min(r, UI_R), color);
    }

    private static void outlineRaw(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
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
        rect(ctx, x + in[0], y, w - in[0] * 2, 1, color);
        rect(ctx, x + in[0], y + h - 1, w - in[0] * 2, 1, color);
        for (int i = 1; i < r; i++) {
            int from = in[i], to = Math.max(in[i] + 1, in[i - 1]);
            int len = to - from;
            rect(ctx, x + from, y + i, len, 1, color);
            rect(ctx, x + w - to, y + i, len, 1, color);
            rect(ctx, x + from, y + h - 1 - i, len, 1, color);
            rect(ctx, x + w - to, y + h - 1 - i, len, 1, color);
        }
        rect(ctx, x, y + r, 1, h - r * 2, color);
        rect(ctx, x + w - 1, y + r, 1, h - r * 2, color);
    }

    /**
     * Very faint halo around a box. The reference look has no heavy glows, so this is capped at 2 layers and a third
     * of the requested strength.
     */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int r, int color, int size) {
        int baseA = Math.round((color >>> 24) * 0.33f);
        size = Math.min(size, 2);
        if (baseA == 0 || size <= 0) return;
        for (int i = size; i >= 1; i--) {
            float f = 1 - (float) i / (size + 1);
            int c = Anim.withAlpha(color, Math.round(baseA * f));
            if ((c >>> 24) == 0) continue;
            outline(ctx, x - i, y - i, w + i * 2, h + i * 2, r + i, c);
        }
    }

    /** Filled disc (not clamped). */
    public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
        roundRaw(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color, color, true, true);
    }

    /** Disc outline (not clamped). */
    public static void ring(DrawContext ctx, int cx, int cy, int radius, int color) {
        outlineRaw(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
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

    // ---------------------------------------------------------------- reference-look building blocks

    public static final int PANEL_BG = 0xA6101012;
    public static final int PANEL_BG_SOLID = 0xD8101012;
    /** Modal windows (settings window, welcome card): nearly opaque so panels behind don't show through. */
    public static final int MODAL_BG = 0xE80E0E10;
    /** Popovers (palette, profiles). */
    public static final int POPOVER_BG = 0xEE0E0E10;
    /** HUD editor hint bar and sidebar: a bit more opaque than panels so they read over bright skies. */
    public static final int HUD_EDITOR_BG = 0xCC101012;
    public static final int FRAME_OUTER = 0x99B8B8B8;
    public static final int FRAME_INNER = 0x3CB8B8B8;

    /** Almost transparent near-black panel with a thin double light frame and 2px corners. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        panel(ctx, x, y, w, h, PANEL_BG);
    }

    public static void panel(DrawContext ctx, int x, int y, int w, int h, int bg) {
        if (w <= 2 || h <= 2) return;
        // faint drop shadow (one line below/right)
        rect(ctx, x + 2, y + h, w - 2, 1, 0x30000000);
        rect(ctx, x + w, y + 2, 1, h - 2, 0x30000000);
        roundRaw(ctx, x, y, w, h, UI_R, bg, bg, true, true);
        outlineRaw(ctx, x, y, w, h, UI_R, FRAME_OUTER);
        outlineRaw(ctx, x + 1, y + 1, w - 2, h - 2, 1, FRAME_INNER);
    }

    /** Slightly lighter inset area inside a modal (tab column, preview box). */
    public static void inset(DrawContext ctx, int x, int y, int w, int h) {
        roundRaw(ctx, x, y, w, h, 1, 0x16FFFFFF, 0x16FFFFFF, true, true);
        outlineRaw(ctx, x, y, w, h, 1, 0x2AFFFFFF);
    }

    /** Inventory-slot-like cell: dark translucent fill and a thin light frame. hover / active 0..1. */
    public static void slot(DrawContext ctx, int x, int y, int w, int h, float hover, float active) {
        int fill = Anim.mix(0x66101012, 0x8C2E2E32, hover);
        if (active > 0.01f) fill = Anim.mix(fill, Palette.accentA(Math.round(70 + 20 * hover)), active * 0.85f);
        roundRaw(ctx, x, y, w, h, 1, fill, fill, true, true);
        outlineRaw(ctx, x, y, w, h, 1, Anim.mix(0x24FFFFFF, 0x40FFFFFF, Math.max(hover, active * 0.6f)));
    }

    /** Flat button: translucent dark fill, centered white text and a 2px line along the bottom edge. */
    public static void button(DrawContext ctx, int x, int y, int w, int h, Text label, float hover, float flash, boolean enabled) {
        float f = Math.max(hover, flash);
        int fill = enabled ? Anim.mix(0xB0101010, 0xD0333333, f) : 0xA0080808;
        int line = enabled ? Anim.mix(0xFF9A9A9A, Anim.mix(0xFFFFFFFF, Palette.accent(), 0.5f), f) : 0xFF555555;
        roundRaw(ctx, x, y, w, h, 1, fill, fill, true, true);
        rect(ctx, x, y + h - 2, w, 2, line);
        if (flash > 0.01f) rect(ctx, x, y, w, h - 2, Anim.withAlpha(0xFFFFFFFF, Math.round(30 * flash)));
        Text t = trim(label, w - 6);
        textCentered(ctx, t, x + w / 2, y + (h - 2 - 8) / 2, enabled ? Anim.mix(0xFFE6E6E6, 0xFFFFFFFF, hover) : 0xFF7A7A7A);
    }

    /** Flat minimal switch: dark track with a thin outline, square knob; the track fills with the accent when on. */
    public static void toggle(DrawContext ctx, int x, int y, int w, int h, float on, float hover) {
        int track = Anim.mix(0xD0141416, Theme.accentAt(0.3f) | 0xFF000000, on);
        roundRaw(ctx, x, y, w, h, 1, track, track, true, true);
        outlineRaw(ctx, x, y, w, h, 1, Anim.mix(0x70FFFFFF, 0xC0FFFFFF, hover));
        int k = h - 2;
        int kx = Math.round(x + 1 + on * (w - 2 - k));
        int knob = Anim.mix(Anim.mix(0xFFBDBDBD, 0xFFE6E6E6, hover), Palette.onAccent(), on);
        roundRaw(ctx, kx, y + 1, k, k, 1, knob, knob, true, true);
    }

    /** Flat slider bar with a bottom line, a filled part and a solid rectangular handle. t / hover 0..1. */
    public static void slider(DrawContext ctx, int x, int y, int w, int h, float t, float hover) {
        roundRaw(ctx, x, y, w, h, 1, 0xB0101010, 0xB0101010, true, true);
        int fw = Math.round((w - 2) * t);
        if (fw > 0) gradientH(ctx, x + 1, y + 1, fw, h - 3,
            Anim.withAlpha(Theme.accentAt(0), 70), Anim.withAlpha(Theme.accentAt(Math.max(0.01f, t)), 130));
        rect(ctx, x, y + h - 2, w, 2, Anim.mix(0xFF9A9A9A, 0xFFFFFFFF, hover));
        int hw = 6 + Math.round(2 * hover);
        int hx = Math.max(x, Math.min(x + w - hw, x + 1 + fw - hw / 2));
        rect(ctx, hx, y - Math.round(hover), hw, h + Math.round(hover * 2), 0xFFE6E6E6);
        rect(ctx, hx, y + h - 2 + Math.round(hover), hw, 2, 0xFFB0B0B0);
    }

    /** Dark text-field cell with a thin frame (accent when focused). */
    public static void field(DrawContext ctx, int x, int y, int w, int h, float hover, float focus) {
        int fill = Anim.mix(0xB00C0C0E, 0xC0161618, Math.max(hover * 0.5f, focus));
        roundRaw(ctx, x, y, w, h, 1, fill, fill, true, true);
        outlineRaw(ctx, x, y, w, h, 1, Anim.mix(Anim.mix(0x40FFFFFF, 0x70FFFFFF, hover), Palette.accent(), focus));
    }

    /** Tooltip box: near-black fill and a 2px light-grey frame. */
    public static void tooltipBox(DrawContext ctx, int x, int y, int w, int h) {
        rect(ctx, x, y, w, h, 0xE0101014);
        outlineRaw(ctx, x, y, w, h, 0, 0xB0B8B8B8);
        outlineRaw(ctx, x + 1, y + 1, w - 2, h - 2, 0, 0x60B8B8B8);
    }

    // ---------------------------------------------------------------- text (always in the Vetmos UI font)

    /** Vertical offset added to every text draw, to tune the TTF baseline against the pixel font. */
    public static int TEXT_DY = 0;

    /** Styled text in the UI font. */
    public static Text styled(String s) {
        return io.github.qan4ur.vetmos.util.Fonts.text(s);
    }

    public static Text styledBold(String s) {
        return io.github.qan4ur.vetmos.util.Fonts.bold(s);
    }

    /** Applies the UI font to a text, using the bold face when the text is bold. */
    public static Text styled(Text t) {
        return io.github.qan4ur.vetmos.util.Fonts.apply(t, t.getStyle().isBold());
    }

    private static boolean shadow() {
        return !io.github.qan4ur.vetmos.util.Fonts.enabled(); // the clean TTF font looks better without the pixel shadow
    }

    public static void text(DrawContext ctx, String s, int x, int y, int color) {
        text(ctx, styled(s), x, y, color);
    }

    public static void bold(DrawContext ctx, String s, int x, int y, int color) {
        text(ctx, styledBold(s), x, y, color);
    }

    public static void text(DrawContext ctx, Text s, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 4) return;
        ctx.drawText(font(), styled(s), x, y + TEXT_DY, c, shadow());
    }

    /** A wrapped line (already styled by {@link #wrap}). */
    public static void line(DrawContext ctx, OrderedText line, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 4) return;
        ctx.drawText(font(), line, x, y + TEXT_DY, c, shadow());
    }

    /** A wrapped line scaled around its top-left corner. */
    public static void lineScaled(DrawContext ctx, OrderedText line, float x, float y, float scale, int color) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        line(ctx, line, 0, 0, color);
        m.popMatrix();
    }

    /** Wraps a string in the UI font. */
    public static List<OrderedText> wrap(String s, int width) {
        return font().wrapLines(styled(s == null ? "" : s), Math.max(1, width));
    }

    public static void textNoShadow(DrawContext ctx, String s, int x, int y, int color) {
        text(ctx, s, x, y, color);
    }

    public static void textCentered(DrawContext ctx, String s, int cx, int y, int color) {
        Text t = styled(s);
        text(ctx, t, cx - font().getWidth(t) / 2, y, color);
    }

    public static void textCentered(DrawContext ctx, Text s, int cx, int y, int color) {
        Text t = styled(s);
        text(ctx, t, cx - font().getWidth(t) / 2, y, color);
    }

    public static void textRight(DrawContext ctx, String s, int right, int y, int color) {
        Text t = styled(s);
        text(ctx, t, right - font().getWidth(t), y, color);
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

    /** Width in the UI font. */
    public static int width(String s) {
        return font().getWidth(styled(s));
    }

    public static int widthBold(String s) {
        return font().getWidth(styledBold(s));
    }

    public static int width(Text t) {
        return font().getWidth(styled(t));
    }

    /** Trims the text (measured in the UI font) to the width, adding an ellipsis when cut. */
    public static String trim(String s, int maxWidth) {
        if (width(s) <= maxWidth) return s;
        StringVisitable cut = font().trimToWidth(styled(s), Math.max(0, maxWidth - width("…")));
        return cut.getString() + "…";
    }

    public static Text trim(Text t, int maxWidth) {
        Text st = styled(t);
        if (font().getWidth(st) <= maxWidth) return st;
        String cut = font().trimToWidth(st, Math.max(0, maxWidth - width("…"))).getString();
        return io.github.qan4ur.vetmos.util.Fonts.apply(Text.literal(cut + "…").setStyle(t.getStyle()), t.getStyle().isBold());
    }

    public static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
