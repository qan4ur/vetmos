package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.DrawContext;

/** A small floating card under a top-bar button (palette, profiles). Fades and slides in. */
public abstract class Popover {
    protected final Anim.Toggle open = new Anim.Toggle(0.16f, false);
    protected int x, y, w, h;

    public void show(int anchorX, int anchorY) {
        x = anchorX;
        y = anchorY;
        open.set(true);
    }

    public void hide() {
        open.set(false);
        onHide();
    }

    protected void onHide() {}

    public boolean isOpen() {
        return open.on();
    }

    public boolean isOver(double mx, double my) {
        return open.on() && Paint.inside(mx, my, x, y, w, h);
    }

    /** Content height for the current state. */
    protected abstract int contentHeight();

    protected abstract void renderContent(DrawContext ctx, int mx, int my, Tooltip tip);

    public void render(DrawContext ctx, int mx, int my, Tooltip tip, int vw, int vh) {
        open.update();
        if (open.raw() <= 0) return;
        h = contentHeight();
        x = Math.max(4, Math.min(vw - w - 4, x));
        float e = Anim.easeOutCubic(open.raw());
        if (!open.on()) mx = my = Widget.NO_MOUSE;
        float old = Paint.alpha;
        Paint.alpha = old * e;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0, (1 - e) * -5);
        Paint.glow(ctx, x, y, w, h, 5, 0xC0000000, 6);
        Paint.roundGradient(ctx, x, y, w, h, 5, 0xF81A1A1A, 0xF80E0E0E);
        Paint.outline(ctx, x, y, w, h, 5, Palette.OUTLINE);
        SettingsWindow.accentLine(ctx, x, y, w);
        renderContent(ctx, mx, my, tip);
        m.popMatrix();
        Paint.alpha = old;
    }

    public abstract boolean mouseClicked(double mx, double my, int button);

    public void mouseDragged(double mx, double my, int button) {}

    public void mouseReleased(double mx, double my, int button) {}

    public boolean mouseScrolled(double mx, double my, double amount) {
        return isOver(mx, my);
    }

    /** Text field of the popover that should receive typing, or null. */
    public SearchField focusedField() {
        return null;
    }

    protected static void caption(DrawContext ctx, String text, int x, int y) {
        Paint.textScaled(ctx, text.toUpperCase(java.util.Locale.ROOT), x, y, 0.7f, Palette.TEXT_MUTED);
    }
}
