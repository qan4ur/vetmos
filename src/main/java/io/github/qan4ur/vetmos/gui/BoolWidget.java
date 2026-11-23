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

        int tw = 18, th = 9;
        int tx = right() - tw, ty = y + (ROW - th) / 2;
        Paint.text(ctx, Paint.trim(title(), tx - x - 4), x, y + 3, textColor(hover));

        Paint.toggle(ctx, tx, ty, tw, th, e, hover);

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
