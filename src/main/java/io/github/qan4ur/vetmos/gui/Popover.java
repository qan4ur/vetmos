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
        // fade + scale from the anchor (top-right corner, where the button is)
        float sc = 0.95f + 0.05f * e;
        m.translate(x + w, y - (1 - e) * 4);
        m.scale(sc, sc);
        m.translate(-(x + w), -y);
        Paint.panel(ctx, x, y, w, h, Paint.POPOVER_BG);
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
