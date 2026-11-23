package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.mixin.HitColorOverlayAccessor;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Recolors the entity hurt tint. In 1.21.7 the tint lives in the 16x16 overlay texture owned by {@link OverlayTexture}:
 * rows y &lt; 8 are the hurt color (vanilla ARGB 0xB2FF0000), rows y &gt;= 8 are the white flash ramp (left untouched).
 */
public class HitColor extends Module {
    /** Vanilla hurt color (OverlayTexture constructor, ldc -1291911168). */
    private static final int VANILLA_HURT_ARGB = 0xB2FF0000;

    private final ColorSetting color = color("Цвета", "hit-color", "Цвет удара",
        "Оттенок сущностей при получении урона. Прозрачность задаёт силу.", ColorSetting.argb(255, 255, 255, 110));

    private int lastApplied;
    private boolean applied;

    public HitColor() {
        super("hit-color", "Цвет удара", "Меняет красный оттенок урона у сущностей на свой цвет.", Category.COMBAT);
        color.onChange(() -> { if (isEnabled()) apply(); });
    }

    @Override
    protected void onEnable() {
        applied = false;
        apply();
    }

    @Override
    protected void onDisable() {
        writeHurtRows(VANILLA_HURT_ARGB);
        applied = false;
    }

    /** Also covers the case where the module was enabled from the config before the game renderer existed. */
    @Override
    public void onTick() {
        if (!applied || color.get() != lastApplied) apply();
    }

    private void apply() {
        int argb = color.get();
        if (writeHurtRows(argb)) {
            lastApplied = argb;
            applied = true;
        }
    }

    private boolean writeHurtRows(int argb) {
        if (mc.gameRenderer == null || mc.gameRenderer.getOverlayTexture() == null) return false;

        NativeImageBackedTexture texture = ((HitColorOverlayAccessor) mc.gameRenderer.getOverlayTexture()).vetmos$getTexture();
        if (texture == null) return false;
        NativeImage image = texture.getImage();
        if (image == null) return false;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                image.setColorArgb(x, y, argb);
            }
        }

        texture.upload();
        return true;
    }
}
