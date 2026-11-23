package io.github.qan4ur.vetmos.modules.world;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;

/**
 * World border visuals only (the border itself, its collision and damage are untouched).
 * Hook: WorldBorderRenderingMixin on net.minecraft.client.render.WorldBorderRendering.render.
 */
public class WorldBorder extends Module {
    private static final String GENERAL = "Основное";

    private final EnumSetting<Mode> mode = enumeration(GENERAL, "mode", "Режим",
        "Скрыть: не рисовать границу. Статичная: свой цвет без мерцания и бегущей анимации.", Mode.STATIC);

    private final ColorSetting color = color(GENERAL, "color", "Цвет",
        "Цвет границы (и когда она растёт или сужается).", ColorSetting.rgb(240, 240, 240))
        .visibleWhen(() -> mode.get() == Mode.STATIC);

    private final IntSetting alpha = integer(GENERAL, "alpha", "Прозрачность",
        "Максимальная непрозрачность границы (0–255).", 160, 0, 255)
        .visibleWhen(() -> mode.get() == Mode.STATIC);

    private final BoolSetting fade = bool(GENERAL, "fade-with-distance", "Затухание по расстоянию",
        "Граница проявляется по мере приближения, как в ванилле. Выкл — постоянная непрозрачность.", true)
        .visibleWhen(() -> mode.get() == Mode.STATIC);

    public WorldBorder() {
        super("world-border", "Граница мира", "Скрывает границу мира или рисует её статичным цветом.", Category.WORLD);
    }

    // Called from the mixin

    public boolean hide() {
        return isEnabled() && mode.get() == Mode.HIDE;
    }

    public boolean isStatic() {
        return isEnabled() && mode.get() == Mode.STATIC;
    }

    /** RGB color (vanilla only reads red/green/blue from the stage color). */
    public int getColor(int original) {
        if (!isStatic()) return original;
        return 0xFF000000 | (color.get() & 0xFFFFFF);
    }

    public double getAlpha(double original) {
        if (!isStatic()) return original;
        double max = alpha.get() / 255.0;
        return fade.get() ? original * max : max;
    }

    public enum Mode {
        HIDE("Скрыть"),
        STATIC("Статичная");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:Mode." + name(), title);
        }
    }
}
