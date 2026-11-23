package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.setting.KeySetting;
import net.minecraft.client.gui.DrawContext;

/** A key setting row: title on the left, a key box on the right (click, then press a key or mouse button). */
public class KeySettingWidget extends SettingWidget<KeySetting> {
    private final KeybindWidget box;

    public KeySettingWidget(KeySetting setting) {
        super(setting);
        box = new KeybindWidget(setting::get, setting::set);
        box.compact = true;
        box.showLabel = false;
    }

    @Override
    public int height() {
        return ROW + 1;
    }

    private void layoutBox() {
        int bw = Math.max(34, box.preferredWidth());
        box.x = right() - bw;
        box.y = y;
        box.w = bw;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean hov = isHovered(mx, my) && !resetHit(mx, my);
        updateHover(hov);
        drawHoverBg(ctx, ROW);
        layoutBox();
        Paint.text(ctx, Paint.trim(title(), box.x - x - 4), x, y + 3, textColor(hover));
        box.render(ctx, mx, my, tip);
        finish(ctx, mx, my, tip);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleReset(mx, my, button)) return true;
        layoutBox();
        return box.mouseClicked(mx, my, button);
    }
}
