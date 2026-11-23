package io.github.qan4ur.vetmos.hud;

import io.github.qan4ur.vetmos.setting.DoubleSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * A module that draws on screen. Implementations extend {@link io.github.qan4ur.vetmos.module.Module}.
 * The HUD element can be dragged with the mouse and resized with the mouse wheel in the HUD editor.
 *
 * <p>Everything is in GUI units at scale 1. Before {@link #renderHud} is called the matrices are translated to the
 * top-left corner of the element and scaled by {@link #hudScale()}.</p>
 */
public interface HudModule {
    double getHudWidth();

    double getHudHeight();

    void renderHud(DrawContext context, float tickDelta, boolean inEditor);

    /** Whether to draw right now (outside the HUD editor). */
    default boolean shouldRenderHud() {
        return true;
    }

    /** A DoubleSetting named "scale" (default 1, range ~0.3..4). */
    DoubleSetting hudScale();

    /** Position used the first time and after "reset position". */
    default HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.LEFT, HudPosition.AnchorY.TOP, 4, 4);
    }
}
