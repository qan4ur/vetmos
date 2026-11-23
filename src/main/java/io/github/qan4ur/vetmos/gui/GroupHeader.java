package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/** Small caption above a group of settings ("ЦВЕТА ────"); also used as a plain spacer when the title is null. */
public class GroupHeader extends Widget {
    private final String title;
    private final BooleanSupplier visible;
    private final int height;

    public GroupHeader(String title, BooleanSupplier visible) {
        this.title = title == null ? null : title.toUpperCase(Locale.ROOT);
        this.visible = visible;
        this.height = 12;
    }

    /** Empty spacer of the given height. */
    public GroupHeader(int height) {
        this.title = null;
        this.visible = () -> true;
        this.height = height;
    }

    @Override
    public boolean visible() {
        return visible.getAsBoolean();
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        if (title == null) {
            Paint.rect(ctx, x, y + height / 2, w, 1, 0x20FFFFFF);
            return;
        }
        float scale = 0.75f;
        Paint.textScaled(ctx, title, x, y + 4, scale, Anim.mix(Palette.TEXT_MUTED, Palette.accent(), 0.25f));
        int tw = Math.round(Paint.width(title) * scale);
        int lx = x + tw + 4;
        if (lx < x + w) Paint.gradientH(ctx, lx, y + 7, x + w - lx, 1, 0x40FFFFFF, 0x00FFFFFF);
    }
}
