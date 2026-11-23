package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/** On/off switch with a sliding knob. */
public class BoolWidget extends SettingWidget<BoolSetting> {
    private float t;

    public BoolWidget(BoolSetting setting) {
        super(setting);
        t = setting.get() ? 1 : 0;
    }

    @Override
    public int height() {
        return ROW;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean hov = isHovered(mx, my) && !resetHit(mx, my);
        updateHover(hov);
        drawHoverBg(ctx, ROW);
        t = Anim.approach(t, setting.get() ? 1 : 0, 16);
        float e = Anim.easeInOutCubic(t);

        int tw = 16, th = 8;
        int tx = right() - tw, ty = y + (ROW - th) / 2;
        Paint.text(ctx, Paint.trim(title(), tx - x - 4), x, y + 3, textColor(hover));

        if (e > 0.02f) Paint.glow(ctx, tx, ty, tw, th, 4, Anim.alpha(Palette.glow(), e), 3);
        Paint.round(ctx, tx, ty, tw, th, 4, Anim.mix(Palette.TRACK, Palette.accent(), e));
        Paint.outline(ctx, tx, ty, tw, th, 4, Anim.mix(Palette.CONTROL_LINE, Anim.mix(Palette.CONTROL_LINE_HOVER, Palette.accentBright(), e), e * 0.6f + hover * 0.4f));
        int kx = Math.round(tx + 1 + e * (tw - th));
        Paint.round(ctx, kx, ty + 1, th - 2, th - 2, 3, Anim.mix(0xFF9A9A9A, Palette.onAccent(), e));

        finish(ctx, mx, my, tip);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleReset(mx, my, button)) return true;
        if (button == 0 && isHovered(mx, my)) {
            setting.toggle();
            click(setting.get() ? 1.3f : 1.0f);
            return true;
        }
        return false;
    }
}
