package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;

/** Rounded outlined button that fills with red on hover and flashes when pressed. */
public class ButtonWidget extends Widget {
    private final java.util.function.Supplier<String> label;
    private final java.util.function.Supplier<String> description;
    private final Runnable action;
    private float flash;

    /** Label and description are suppliers so they follow the interface language live. */
    public ButtonWidget(java.util.function.Supplier<String> label, java.util.function.Supplier<String> description, Runnable action) {
        this.label = label;
        this.description = description;
        this.action = action;
    }

    @Override
    public int height() {
        return 16;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean hov = isHovered(mx, my);
        updateHover(hov);
        flash = Anim.approach(flash, 0, 6);
        int bx = x, by = y + 2, bw = w, bh = height() - 3;
        draw(ctx, bx, by, bw, bh, label.get(), hover, flash);
        if (hov && description != null) tip.offer(this, description.get());
    }

    /** Shared look, also used by the top bar buttons. */
    public static void draw(DrawContext ctx, int bx, int by, int bw, int bh, String label, float hover, float flash) {
        float f = Math.max(hover * 0.7f, flash);
        if (f > 0.02f) Paint.glow(ctx, bx, by, bw, bh, 3, Anim.alpha(Palette.glow(), f * 0.6f), 3);
        Paint.roundGradient(ctx, bx, by, bw, bh, 3, Anim.mix(0xFF202020, Anim.mix(0xFF2A2A2A, Palette.accent(), 0.35f), f), Anim.mix(0xFF141414, Anim.mix(0xFF1A1A1A, Palette.accent(), 0.2f), f));
        Paint.outline(ctx, bx, by, bw, bh, 3, Anim.mix(Palette.CONTROL_LINE, Palette.accent(), f));
        String l = Paint.trim(label, bw - 6);
        Paint.textCentered(ctx, l, bx + bw / 2, by + (bh - 8) / 2 + 1, Anim.mix(Palette.TEXT_IDLE, Palette.TEXT, hover));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            action.run();
            flash = 1;
            click(1.1f);
            return true;
        }
        return false;
    }
}
