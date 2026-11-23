package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Color swatch that unfolds an inline picker: saturation/value square, hue bar and alpha bar. */
public class ColorWidget extends SettingWidget<ColorSetting> {
    private static final int PICKER_H = 84;
    private static final int SV_H = 46, BAR_H = 6;

    private final Anim.Toggle open = new Anim.Toggle(0.2f, false);
    private float hue, sat, val;
    private int last;
    private int drag; // 0 none, 1 sv, 2 hue, 3 alpha

    public ColorWidget(ColorSetting setting) {
        super(setting);
        sync(true);
    }

    private void sync(boolean force) {
        int v = setting.get();
        if (!force && v == last) return;
        last = v;
        float[] hsv = toHsv(v >> 16 & 255, v >> 8 & 255, v & 255);
        if (hsv[1] > 0.001f && hsv[2] > 0.001f) hue = hsv[0];
        if (hsv[2] > 0.001f) sat = hsv[1];
        val = hsv[2];
    }

    private void apply(int alpha) {
        int rgb = fromHsv(hue, sat, val);
        int v = (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
        setting.set(v);
        last = setting.get();
    }

    @Override
    public int height() {
        return ROW + Math.round(PICKER_H * open.eased());
    }

    private int px() {
        return x + 1;
    }

    private int pw() {
        return right() - x - 2;
    }

    private int svY() {
        return y + ROW + 2;
    }

    private int hueY() {
        return svY() + SV_H + 4;
    }

    private int alphaY() {
        return hueY() + BAR_H + 4;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        open.update();
        sync(false);
        boolean rowHov = Paint.inside(mx, my, x, y, w, ROW) && !resetHit(mx, my);
        updateHover(rowHov);
        drawHoverBg(ctx, ROW);

        int color = setting.get();
        int sw = 18, sh = 8, sx = right() - sw, sy = y + (ROW - sh) / 2;
        Paint.text(ctx, Paint.trim(title(), sx - x - 4), x, y + 3, textColor(hover));
        Paint.checker(ctx, sx, sy, sw, sh, 2);
        Paint.rect(ctx, sx, sy, sw, sh, color);
        Paint.outline(ctx, sx - 1, sy - 1, sw + 2, sh + 2, 0, Anim.mix(Palette.CONTROL_LINE, 0xFFFFFFFF, hover * 0.5f + open.eased() * 0.5f));

        if (open.raw() > 0) renderPicker(ctx, mx, my);

        // reset button; tooltip only for the header row (offered after finish so the color info wins)
        finish(ctx, mx, Paint.inside(mx, my, x, y, w, ROW) ? my : NO_MOUSE, tip);
        if (rowHov) tip.offer(setting, title(), hex(color) + ", A " + setting.alpha() + " — " + io.github.qan4ur.vetmos.util.Lang.ui("color-click", "нажмите, чтобы открыть палитру"));
    }

    private void renderPicker(DrawContext ctx, int mx, int my) {
        int h = height();
        ctx.enableScissor(x - 2, y + ROW, x + w + 2, y + h);
        float old = Paint.alpha;
        Paint.alpha = old * Anim.clamp01(open.eased() * 1.4f);

        int px = px(), pw = pw();
        // saturation / value square
        int svY = svY();
        for (int i = 0; i < pw; i++) {
            float s = pw <= 1 ? 0 : i / (float) (pw - 1);
            int top = fromHsv(hue, s, 1) | 0xFF000000;
            Paint.gradientV(ctx, px + i, svY, 1, SV_H, top, 0xFF000000);
        }
        Paint.outline(ctx, px - 1, svY - 1, pw + 2, SV_H + 2, 0, Palette.CONTROL_LINE);
        int cx = px + Math.round(sat * (pw - 1)), cy = svY + Math.round((1 - val) * (SV_H - 1));
        Paint.outline(ctx, cx - 2, cy - 2, 5, 5, 1, 0xFF000000);
        Paint.outline(ctx, cx - 1, cy - 1, 3, 3, 0, 0xFFFFFFFF);

        // hue bar
        int hy = hueY();
        for (int i = 0; i < pw; i++) {
            Paint.rect(ctx, px + i, hy, 1, BAR_H, fromHsv(pw <= 1 ? 0 : i / (float) (pw - 1), 1, 1) | 0xFF000000);
        }
        Paint.outline(ctx, px - 1, hy - 1, pw + 2, BAR_H + 2, 0, Palette.CONTROL_LINE);
        int hx = px + Math.round(hue * (pw - 1));
        Paint.rect(ctx, hx - 1, hy - 1, 3, BAR_H + 2, 0xFF000000);
        Paint.rect(ctx, hx, hy - 1, 1, BAR_H + 2, 0xFFFFFFFF);

        // alpha bar
        int ay = alphaY();
        int rgb = setting.get() & 0xFFFFFF;
        Paint.checker(ctx, px, ay, pw, BAR_H, 3);
        Paint.gradientH(ctx, px, ay, pw, BAR_H, rgb, 0xFF000000 | rgb);
        Paint.outline(ctx, px - 1, ay - 1, pw + 2, BAR_H + 2, 0, Palette.CONTROL_LINE);
        int ax = px + Math.round(setting.alpha() / 255f * (pw - 1));
        Paint.rect(ctx, ax - 1, ay - 1, 3, BAR_H + 2, 0xFF000000);
        Paint.rect(ctx, ax, ay - 1, 1, BAR_H + 2, 0xFFFFFFFF);

        // info line
        int iy = ay + BAR_H + 4;
        Paint.text(ctx, hex(setting.get()), px, iy, Palette.TEXT_SECONDARY);
        Paint.textRight(ctx, "A " + setting.alpha(), px + pw, iy, Palette.TEXT_MUTED);

        Paint.alpha = old;
        ctx.disableScissor();
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleReset(mx, my, button)) {
            sync(true);
            return true;
        }
        if (Paint.inside(mx, my, x, y, w, ROW)) {
            if (button == 0 || button == 1) {
                open.set(!open.on());
                click(open.on() ? 1.2f : 0.9f);
                return true;
            }
            return false;
        }
        if (!open.on() || button != 0) return false;
        int px = px(), pw = pw();
        if (Paint.inside(mx, my, px - 1, svY() - 1, pw + 2, SV_H + 2)) drag = 1;
        else if (Paint.inside(mx, my, px - 1, hueY() - 2, pw + 2, BAR_H + 4)) drag = 2;
        else if (Paint.inside(mx, my, px - 1, alphaY() - 2, pw + 2, BAR_H + 4)) drag = 3;
        else return isHovered(mx, my);
        mouseDragged(mx, my, button);
        return true;
    }

    @Override
    public void mouseDragged(double mx, double my, int button) {
        if (drag == 0) return;
        float tx = Anim.clamp01((float) ((mx - px()) / Math.max(1, pw() - 1)));
        int alpha = setting.alpha();
        switch (drag) {
            case 1 -> {
                sat = tx;
                val = 1 - Anim.clamp01((float) ((my - svY()) / (SV_H - 1)));
                apply(alpha);
            }
            case 2 -> {
                hue = tx;
                apply(alpha);
            }
            case 3 -> apply(Math.round(tx * 255));
            default -> {}
        }
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        drag = 0;
    }

    // ---------------------------------------------------------------- HSV

    static float[] toHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == rf) h = ((gf - bf) / d) % 6;
        else if (max == gf) h = (bf - rf) / d + 2;
        else h = (rf - gf) / d + 4;
        h /= 6;
        if (h < 0) h += 1;
        float s = max == 0 ? 0 : d / max;
        return new float[] { h, s, max };
    }

    static int fromHsv(float h, float s, float v) {
        h = (h % 1 + 1) % 1 * 6;
        int i = (int) Math.floor(h);
        float f = h - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }
}
