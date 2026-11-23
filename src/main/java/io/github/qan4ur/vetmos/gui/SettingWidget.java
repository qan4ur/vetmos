package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.Setting;
import net.minecraft.client.gui.DrawContext;

import java.util.Objects;

/** Base of the widgets bound to a setting: tooltip with the description and a small "↺" reset button. */
public abstract class SettingWidget<S extends Setting<?>> extends Widget {
    protected static final int RESET_W = 9;
    protected final S setting;
    private float resetHover;
    private float resetSpin;

    protected SettingWidget(S setting) {
        this.setting = setting;
    }

    @Override
    public boolean visible() {
        return setting.isVisible();
    }

    protected boolean modified() {
        return !Objects.equals(setting.get(), setting.getDefault());
    }

    /** Right edge of the widget content (left of the reset button). */
    protected int right() {
        return x + w - RESET_W - 2;
    }

    protected boolean resetHit(double mx, double my) {
        return Paint.inside(mx, my, x + w - RESET_W, y, RESET_W + 2, ROW);
    }

    /** Draws the reset button and offers the tooltip. Call at the end of render. */
    protected void finish(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean rh = resetHit(mx, my);
        resetHover = Anim.approach(resetHover, rh ? 1 : 0, 18);
        resetSpin = Anim.approach(resetSpin, 0, 9);
        boolean mod = modified();
        int base = mod ? Palette.accent() : 0x55A0A0A0;
        int color = Anim.mix(base, Palette.accentBright(), resetHover);
        var m = ctx.getMatrices();
        float cx = x + w - RESET_W / 2f, cy = y + ROW / 2f;
        m.pushMatrix();
        m.translate(cx, cy);
        m.rotate((float) (-resetSpin * Math.PI * 2));
        float s = 1 + resetHover * 0.15f;
        m.scale(s, s);
        Paint.text(ctx, "↺", -Paint.width("↺") / 2, -4, color);
        m.popMatrix();

        if (rh) tip.offer(this.setting.name + "#reset", "Сбросить «" + setting.title + "» к значению по умолчанию.");
        else if (isHovered(mx, my)) tip.offer(setting, setting.title, setting.description);
    }

    /** Handles a click on the reset button. */
    protected boolean handleReset(double mx, double my, int button) {
        if (button == 0 && resetHit(mx, my)) {
            setting.reset();
            resetSpin = 1;
            click(1.4f);
            return true;
        }
        return false;
    }
}
