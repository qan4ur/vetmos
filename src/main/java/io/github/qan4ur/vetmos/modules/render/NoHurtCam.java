package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.IntSetting;

/** Scales (or removes) the camera tilt when you take damage. See NoHurtCamGameRendererMixin. */
public class NoHurtCam extends Module {
    private static final String GENERAL = "Основное";

    private final IntSetting strength = integer(GENERAL, "strength", "Сила тряски",
        "Сколько от обычной тряски оставить: 0% — без тряски, 100% — как в ванилле.", 0, 0, 100);

    public NoHurtCam() {
        super("no-hurt-cam", "Без тряски от урона", "Убирает или ослабляет наклон камеры при получении урона.", Category.RENDER);
    }

    /** Multiplier for the hurt tilt angle (1 = vanilla). */
    public static float tiltMultiplier() {
        NoHurtCam m = ModuleManager.get(NoHurtCam.class);
        if (m == null || !m.isEnabled()) return 1f;
        return m.strength.get() / 100f;
    }
}
