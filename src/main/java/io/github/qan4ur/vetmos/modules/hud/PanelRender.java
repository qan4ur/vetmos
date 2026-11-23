package io.github.qan4ur.vetmos.modules.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.joml.Matrix3x2fStack;

/**
 * Shared drawing / animation helpers for the Vetmos HUD modules (rounded panels, soft shadows, gradients, easing).
 * All coordinates are GUI units in the current DrawContext matrices.
 */
public final class PanelRender {
    private PanelRender() {}

    // ---------------------------------------------------------------- visibility / timing

    /** In a world, HUD not hidden, and no screen open (chat is allowed; other screens only when {@code showInScreens}). */
    public static boolean shouldRender(boolean showInScreens) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;
        if (mc.options.hudHidden) return false;
        Screen screen = mc.currentScreen;
        return screen == null || screen instanceof ChatScreen || showInScreens;
    }

    public static boolean isChatOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    /** Clamped frame delta in seconds. */
    public static double dt(double frameTime) {
        if (frameTime < 0 || Double.isNaN(frameTime)) return 0;
        return Math.min(frameTime, 0.1);
    }

    /** Frame clock: returns the clamped seconds since the previous call (0 on the first call). */
    public static final class FrameClock {
        private long last;

        public double tick() {
            long now = System.nanoTime();
            double d = last == 0 ? 0 : dt((now - last) / 1e9);
            last = now;
            return d;
        }
    }

    /** Frame-rate independent exponential approach. */
    public static double approach(double current, double target, double speed, double dt) {
        return current + (target - current) * (1 - Math.exp(-speed * dt));
    }

    public static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static double easeOutCubic(double t) {
        t = clamp01(t);
        double f = 1 - t;
        return 1 - f * f * f;
    }

    // ---------------------------------------------------------------- colors (ARGB)

    public static int withAlpha(int argb, double mul) {
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * clamp01(mul));
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    public static int lerpColor(double t, int from, int to) {
        t = clamp01(t);
        int a = (int) Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static boolean invisible(int argb) {
        return (argb >>> 24) == 0;
    }

    // ---------------------------------------------------------------- shapes

    /** Horizontal inset of corner row i (0 = outermost row) for a corner radius r. */
    private static int cornerInset(int r, int i) {
        double dy = r - i - 0.5;
        return r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)) + 0.3);
    }

    private static int rowInset(int r, int i, int h) {
        if (i < r) return cornerInset(r, i);
        if (i >= h - r) return cornerInset(r, h - 1 - i);
        return 0;
    }

    private static int clampRadius(int r, int w, int h) {
        return Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
    }

    /** Filled rounded rectangle built from non-overlapping rows (safe with translucent colors). */
    public static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1 || invisible(color)) return;
        r = clampRadius(r, x2 - x1, y2 - y1);
        if (r == 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int in = cornerInset(r, i);
            ctx.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, color);
            ctx.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, color);
        }
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
    }

    /** Rounded rectangle with a vertical gradient (top to bottom). */
    public static void roundedGradient(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int top, int bottom) {
        if (x2 <= x1 || y2 <= y1) return;
        int h = y2 - y1;
        r = clampRadius(r, x2 - x1, h);
        for (int i = 0; i < r; i++) {
            int in = cornerInset(r, i);
            ctx.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, lerpColor((i + 0.5) / h, top, bottom));
            ctx.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, lerpColor((h - i - 0.5) / h, top, bottom));
        }
        if (h - 2 * r > 0) ctx.fillGradient(x1, y1 + r, x2, y2 - r, lerpColor((double) r / h, top, bottom), lerpColor((double) (h - r) / h, top, bottom));
    }

    /** A ring of thickness t drawn just outside the rounded rect (x1, y1, x2, y2, r). Non-overlapping. */
    public static void ring(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int t, int color) {
        if (x2 <= x1 || y2 <= y1 || t <= 0 || invisible(color)) return;
        int w = x2 - x1, h = y2 - y1;
        r = clampRadius(r, w, h);
        int ox1 = x1 - t, oy1 = y1 - t, ox2 = x2 + t, oy2 = y2 + t;
        int oh = oy2 - oy1, or = r + t;

        for (int y = oy1; y < oy2; y++) {
            int oi = rowInset(or, y - oy1, oh);
            if (y < y1 || y >= y2) {
                hfill(ctx, ox1 + oi, y, ox2 - oi, color);
            } else {
                int ry = y - y1;
                if (ry >= r && ry < h - r) {
                    int end = y2 - r;
                    fillSafe(ctx, ox1, y, x1, end, color);
                    fillSafe(ctx, x2, y, ox2, end, color);
                    y = end - 1;
                    continue;
                }
                int ii = rowInset(r, ry, h);
                hfill(ctx, ox1 + oi, y, x1 + ii, color);
                hfill(ctx, x2 - ii, y, ox2 - oi, color);
            }
        }
    }

    private static void hfill(DrawContext ctx, int xa, int y, int xb, int color) {
        if (xb > xa) ctx.fill(xa, y, xb, y + 1, color);
    }

    private static void fillSafe(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        if (x2 > x1 && y2 > y1) ctx.fill(x1, y1, x2, y2, color);
    }

    /** Soft drop shadow made of stacked rings with a quadratic falloff (does not darken the inside). */
    public static void shadow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int size, int color) {
        for (int k = 0; k < size; k++) {
            double f = 1.0 - (double) k / size;
            ring(ctx, x1 - k, y1 - k, x2 + k, y2 + k, r + k, 1, withAlpha(color, f * f));
        }
    }

    /** Horizontal gradient (left to right) drawn as a rotated vertical gradient, so it is perfectly smooth. */
    public static void hGradient(DrawContext ctx, int x1, int y1, int x2, int y2, int left, int right) {
        if (x2 <= x1 || y2 <= y1) return;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x1, y2);
        m.rotate((float) (-Math.PI / 2));
        // local (u, v) -> screen (x1 + v, y2 - u); the gradient runs along v (left to right)
        ctx.fillGradient(0, 0, y2 - y1, x2 - x1, left, right);
        m.popMatrix();
    }

    /** Standard Vetmos panel: shadow, background, outline and an optional glow along the top edge. */
    public static void panel(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int bg, int outline, int shadowColor, int shadowSize, int glow) {
        if (shadowSize > 0) shadow(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1, r + 1, shadowSize, shadowColor);
        roundedRect(ctx, x1, y1, x2, y2, r, bg);
        if (!invisible(glow)) {
            int gh = Math.min(10, (y2 - y1) / 2);
            int in = Math.max(r, 1);
            ctx.fillGradient(x1 + in, y1 + 1, x2 - in, y1 + 1 + gh, glow, withAlpha(glow, 0));
            ctx.fill(x1 + in, y1, x2 - in, y1 + 1, withAlpha(glow, 1.0));
        }
        ring(ctx, x1, y1, x2, y2, r, 1, outline);
    }

    // ---------------------------------------------------------------- text

    /** Draws text unless it is (nearly) fully transparent; vanilla treats a tiny alpha as opaque in some paths. */
    public static void text(DrawContext ctx, String s, int x, int y, int color, boolean shadow) {
        if (((color >>> 24) & 0xFF) < 8) return;
        ctx.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, shadow);
    }
}
