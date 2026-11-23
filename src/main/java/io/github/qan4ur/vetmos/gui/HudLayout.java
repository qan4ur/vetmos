package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Shared HUD positioning code (HUD editor and the single-element move screen): snapping, moving, scaling, frames. */
public final class HudLayout {
    public static final int SNAP = 4;

    private HudLayout() {}

    public static List<Module> enabledHud() {
        List<Module> list = new ArrayList<>();
        for (Module m : HudManager.hudModules()) if (m.isEnabled()) list.add(m);
        return list;
    }

    /**
     * Snaps the box edge/center to the screen edges, the screen center and other enabled elements (except
     * {@code self}). Returns {pos, guide line coordinate or NaN}.
     */
    public static double[] snap(double pos, double size, boolean horizontal, int screenW, int screenH, Module self) {
        double screen = horizontal ? screenW : screenH;
        List<Double> targets = new ArrayList<>(List.of(0.0, screen / 2, screen));
        for (Module o : enabledHud()) {
            if (o == self) continue;
            double[] ob = HudManager.getBounds(o);
            double s = horizontal ? ob[0] : ob[1], l = horizontal ? ob[2] : ob[3];
            targets.add(s);
            targets.add(s + l);
            targets.add(s + l / 2);
        }
        double best = SNAP + 1, result = pos, guide = Double.NaN;
        double[] offsets = { 0, size / 2, size };
        for (double t : targets) {
            for (double off : offsets) {
                double d = Math.abs(pos + off - t);
                if (d < best) {
                    best = d;
                    result = t - off;
                    guide = t;
                }
            }
        }
        return new double[] { result, guide };
    }

    /** Moves the element so its top-left corner is at (left, top), clamped inside the screen. */
    public static void move(Module m, double left, double top, double w, double h, int screenW, int screenH) {
        left = Math.max(0, Math.min(screenW - w, left));
        top = Math.max(0, Math.min(screenH - h, top));
        HudPosition pos = HudManager.getPosition(m).copy();
        pos.setFromTopLeft(left, top, w, h, screenW, screenH);
        HudManager.setPosition(m, pos);
    }

    /** Changes the element's scale by ±0.05 keeping its center in place. Returns {oldW, oldH, newW, newH}. */
    public static double[] scaleBy(Module m, double amount, int screenW, int screenH) {
        DoubleSetting scale = ((HudModule) m).hudScale();
        double[] before = HudManager.getBounds(m);
        double cx = before[0] + before[2] / 2, cy = before[1] + before[3] / 2;
        scale.set(scale.get() + (amount > 0 ? 0.05 : -0.05));
        double[] after = HudManager.getBounds(m);
        move(m, cx - after[2] / 2, cy - after[3] / 2, after[2], after[3], screenW, screenH);
        return new double[] { before[2], before[3], after[2], after[3] };
    }

    /** Draws one element in preview mode at its bounds (for disabled / hidden elements). */
    public static void renderPreview(DrawContext ctx, Module m, boolean inEditor) {
        HudModule hud = (HudModule) m;
        double[] b = HudManager.getBounds(m);
        var mat = ctx.getMatrices();
        mat.pushMatrix();
        try {
            mat.translate((float) b[0], (float) b[1]);
            float s = (float) (double) hud.hudScale().get();
            mat.scale(s, s);
            hud.renderHud(ctx, MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true), inEditor);
        } catch (Throwable ignored) {
        } finally {
            mat.popMatrix();
        }
    }

    /** Accent frame with corner ticks and a title label around an element. hover/drag 0..1. */
    public static void drawFrame(DrawContext ctx, Module m, float hover, float drag, int screenW) {
        double[] b = HudManager.getBounds(m);
        int x = (int) Math.floor(b[0]) - 1, y = (int) Math.floor(b[1]) - 1;
        int w = (int) Math.ceil(b[0] + b[2]) + 1 - x, h = (int) Math.ceil(b[1] + b[3]) + 1 - y;
        float f = Math.max(hover, drag);

        Paint.rect(ctx, x, y, w, h, Palette.accentA(Math.round(14 + 26 * f)));
        Paint.outline(ctx, x, y, w, h, 0, Anim.mix(Palette.accentA(150), Palette.accentBright(), f));
        int c = Anim.mix(Palette.accent(), 0xFFFFFFFF, f * 0.7f);
        int t = Math.min(4, Math.min(w, h) / 2);
        Paint.rect(ctx, x - 1, y - 1, t + 1, 1, c);
        Paint.rect(ctx, x - 1, y - 1, 1, t + 1, c);
        Paint.rect(ctx, x + w - t, y - 1, t + 1, 1, c);
        Paint.rect(ctx, x + w, y - 1, 1, t + 1, c);
        Paint.rect(ctx, x - 1, y + h, t + 1, 1, c);
        Paint.rect(ctx, x - 1, y + h - t, 1, t + 1, c);
        Paint.rect(ctx, x + w - t, y + h, t + 1, 1, c);
        Paint.rect(ctx, x + w, y + h - t, 1, t + 1, c);

        float sc = 0.75f;
        String label = m.displayTitle();
        int lw = Math.round(Paint.width(label) * sc) + 6, lh = 9;
        int lx = Math.max(1, Math.min(screenW - lw - 1, x));
        int ly = y - lh - 1;
        if (ly < 1) ly = y + h + 1;
        Paint.rect(ctx, lx, ly, lw, lh, Anim.mix(0xB0101012, 0xD0303034, f));
        Paint.rect(ctx, lx, ly + lh - 1, lw, 1, Anim.mix(0xFF9A9A9A, Palette.accent(), f));
        Paint.textScaled(ctx, label, lx + 3, ly + 2, sc, Palette.TEXT);
    }
}
