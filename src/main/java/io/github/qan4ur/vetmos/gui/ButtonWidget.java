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
        Paint.button(ctx, bx, by, bw, bh, Paint.styled(label), hover, flash, true);
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
