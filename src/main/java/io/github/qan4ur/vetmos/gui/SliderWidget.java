package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import io.github.qan4ur.vetmos.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * Slider for {@link IntSetting} and {@link DoubleSetting}. Drag to change; Shift + wheel changes by the smallest
 * step; Shift while dragging gives fine control.
 */
public class SliderWidget extends SettingWidget<Setting<?>> {
    private static final int H = 22;
    private boolean dragging;
    private float shown;
    private float knob;
    private double dragStartX, dragStartT;

    public SliderWidget(IntSetting setting) {
        super(setting);
        shown = (float) getT();
    }

    public SliderWidget(DoubleSetting setting) {
        super(setting);
        shown = (float) getT();
    }

    @Override
    public int height() {
        return H;
    }

    private double getT() {
        if (setting instanceof IntSetting s) return s.max == s.min ? 0 : (s.get() - s.min) / (double) (s.max - s.min);
        DoubleSetting s = (DoubleSetting) setting;
        return s.max == s.min ? 0 : (s.get() - s.min) / (s.max - s.min);
    }

    private void setT(double t) {
        t = Math.max(0, Math.min(1, t));
        if (setting instanceof IntSetting s) s.set((int) Math.round(s.min + t * (s.max - s.min)));
        else {
            DoubleSetting s = (DoubleSetting) setting;
            s.set(s.min + t * (s.max - s.min));
        }
    }

    private void step(int dir) {
        if (setting instanceof IntSetting s) s.set(s.get() + dir);
        else {
            DoubleSetting s = (DoubleSetting) setting;
            s.set(s.get() + dir * Math.pow(10, -s.decimals));
        }
    }

    private String valueText() {
        if (setting instanceof IntSetting s) return String.valueOf(s.get());
        return setting.asString();
    }

    private int trackX() {
        return x + 1;
    }

    private int trackW() {
        return right() - x - 2;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean hov = isHovered(mx, my) && !resetHit(mx, my);
        updateHover(hov || dragging);
        drawHoverBg(ctx, H);
        shown = Anim.approach(shown, (float) getT(), dragging ? 40 : 18);
        knob = Anim.approach(knob, dragging ? 1 : hov ? 0.6f : 0, 18);

        String value = valueText();
        int vw = Paint.width(value);
        Paint.text(ctx, Paint.trim(title(), right() - x - vw - 6), x, y + 3, textColor(hover));
        Paint.textRight(ctx, value, right(), y + 3, Anim.mix(Palette.TEXT_SECONDARY, Palette.TEXT, knob));

        int tx = trackX(), tw = trackW(), ty = y + 13;
        Paint.slider(ctx, tx, ty, tw, 7, shown, knob);

        finish(ctx, mx, my, tip);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleReset(mx, my, button)) return true;
        if (button == 0 && isHovered(mx, my)) {
            dragging = true;
            dragStartX = mx;
            if (Screen.hasShiftDown()) dragStartT = getT();
            else {
                setT((mx - trackX()) / (double) trackW());
                dragStartT = getT();
            }
            return true;
        }
        return false;
    }

    @Override
    public void mouseDragged(double mx, double my, int button) {
        if (!dragging) return;
        if (Screen.hasShiftDown()) setT(dragStartT + (mx - dragStartX) / (trackW() * 8.0));
        else {
            setT((mx - trackX()) / (double) trackW());
            dragStartX = mx;
            dragStartT = getT();
        }
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!Screen.hasShiftDown() || !isHovered(mx, my) || amount == 0) return false;
        step(amount > 0 ? 1 : -1);
        return true;
    }
}
