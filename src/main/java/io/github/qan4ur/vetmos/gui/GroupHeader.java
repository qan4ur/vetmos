package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/** Small caption above a group of settings ("ЦВЕТА ────"); also used as a plain spacer when the title is null. */
public class GroupHeader extends Widget {
    private final java.util.function.Supplier<String> title;
    private final BooleanSupplier visible;
    private final int height;

    public GroupHeader(java.util.function.Supplier<String> title, BooleanSupplier visible) {
        this.title = title;
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
        String title = this.title.get().toUpperCase(Locale.ROOT);
        Paint.textScaled(ctx, title, x, y + 4, scale, Palette.TEXT_MUTED);
        int tw = Math.round(Paint.width(title) * scale);
        int lx = x + tw + 4;
        if (lx < x + w) Paint.rect(ctx, lx, y + 7, x + w - lx, 1, 0x26FFFFFF);
    }
}
