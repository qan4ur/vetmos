package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/**
 * A row inside an expanded module. The owner sets {@link #x}, {@link #y} and {@link #w} every frame before
 * {@link #render}; input handlers use the positions from the last frame.
 */
public abstract class Widget {
    /** Mouse coordinate passed to widgets when the mouse is outside their clip area. */
    public static final int NO_MOUSE = -100000;
    public static final int ROW = 13;

    public int x, y, w;
    protected float hover;

    public abstract int height();

    public boolean visible() {
        return true;
    }

    public abstract void render(DrawContext ctx, int mx, int my, Tooltip tip);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {}

    public void mouseDragged(double mx, double my, int button) {}

    public boolean mouseScrolled(double mx, double my, double amount) {
        return false;
    }

    public boolean isHovered(double mx, double my) {
        return Paint.inside(mx, my, x, y, w, height());
    }

    protected void updateHover(boolean hovered) {
        hover = Anim.approach(hover, hovered ? 1 : 0, 18);
    }

    /** Subtle highlight behind a hovered row. */
    protected void drawHoverBg(DrawContext ctx, int h) {
        if (hover > 0.01f) Paint.round(ctx, x - 2, y, w + 4, h, 2, Anim.withAlpha(0xFFFFFFFF, Math.round(14 * hover)));
    }

    protected static int textColor(float hover) {
        return Anim.mix(Palette.TEXT_IDLE, Palette.TEXT, hover);
    }

    public static void click(float pitch) {
        try {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.35f));
        } catch (Throwable ignored) {}
    }
}
