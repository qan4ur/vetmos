package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.EnumSetting;
import net.minecraft.client.gui.DrawContext;

/** Cycles enum values: LMB forward, RMB back. The value slides in from the direction of the change. */
public class EnumWidget extends SettingWidget<EnumSetting<?>> {
    private float slide;
    private float flash;
    private Object last;

    public EnumWidget(EnumSetting<?> setting) {
        super(setting);
        last = setting.get();
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
        if (last != setting.get()) {
            last = setting.get();
            flash = 1;
        }
        slide = Anim.approach(slide, 0, 14);
        flash = Anim.approach(flash, 0, 6);

        String value = String.valueOf(setting.get());
        int maxValueW = Math.max(30, (right() - x) / 2 + 10);
        value = Paint.trim(value, maxValueW);
        int vw = Paint.width(value);
        int boxW = vw + 16, boxX = right() - boxW, boxY = y + 1, boxH = ROW - 2;

        Paint.text(ctx, Paint.trim(title(), boxX - x - 4), x, y + 3, textColor(hover));
        Paint.round(ctx, boxX, boxY, boxW, boxH, 3, Anim.mix(Palette.CONTROL, Palette.CONTROL_HOVER, hover * 0.6f + flash * 0.4f));
        Paint.outline(ctx, boxX, boxY, boxW, boxH, 3, Anim.mix(Palette.CONTROL_LINE, Palette.accent(), hover * 0.5f + flash * 0.5f));
        int arrow = Anim.mix(Palette.TEXT_FAINT, Palette.accent(), hover);
        Paint.text(ctx, "‹", boxX + 3, y + 3, arrow);
        Paint.text(ctx, "›", boxX + boxW - 3 - Paint.width("›"), y + 3, arrow);

        ctx.enableScissor(boxX + 7, boxY, boxX + boxW - 7, boxY + boxH);
        Paint.text(ctx, value, boxX + 8 + Math.round(slide * 8), y + 3, Anim.mix(Palette.TEXT, Palette.accent(), flash * 0.7f));
        ctx.disableScissor();

        finish(ctx, mx, my, tip);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleReset(mx, my, button)) return true;
        if ((button == 0 || button == 1) && isHovered(mx, my)) {
            setting.cycle(button == 0);
            slide = button == 0 ? 1 : -1;
            click(button == 0 ? 1.2f : 1.0f);
            return true;
        }
        return false;
    }
}
