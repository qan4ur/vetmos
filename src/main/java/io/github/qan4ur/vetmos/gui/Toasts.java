package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Small notifications at the bottom of the screen ("Код скопирован"). */
public final class Toasts {
    private static final float LIFE = 2.6f;
    private static final List<Toast> TOASTS = new ArrayList<>();

    private Toasts() {}

    private static final class Toast {
        final String text;
        final boolean error;
        float age;

        Toast(String text, boolean error) {
            this.text = text;
            this.error = error;
        }
    }

    public static void show(String text) {
        show(text, false);
    }

    public static void show(String text, boolean error) {
        TOASTS.add(new Toast(text, error));
        if (TOASTS.size() > 4) TOASTS.remove(0);
    }

    /** Draws the toasts centered above the given bottom y (virtual coordinates). */
    public static void render(DrawContext ctx, int centerX, int bottomY) {
        int y = bottomY;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast t = TOASTS.get(i);
            float in = Anim.easeOutCubic(t.age / 0.2f);
            float out = Anim.clamp01((LIFE - t.age) / 0.35f);
            float f = Math.min(in, out);
            int w = Paint.width(t.text) + 20, h = 16;
            int x = centerX - w / 2;
            int yy = y - h + Math.round((1 - in) * 8);
            float old = Paint.alpha;
            Paint.alpha = old * f;
            Paint.panel(ctx, x, yy, w, h, Paint.PANEL_BG_SOLID);
            int accent = t.error ? 0xFFE5484D : Palette.accent();
            Paint.rect(ctx, x + 2, yy + h - 2, w - 4, 1, Anim.withAlpha(accent, 170));
            Paint.circle(ctx, x + 8, yy + h / 2, 2, accent);
            Paint.text(ctx, t.text, x + 14, yy + 4, Palette.TEXT);
            Paint.alpha = old;
            y -= Math.round((h + 4) * f);
        }
        for (Iterator<Toast> it = TOASTS.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            t.age += Anim.dt;
            if (t.age > LIFE) it.remove();
        }
    }
}
