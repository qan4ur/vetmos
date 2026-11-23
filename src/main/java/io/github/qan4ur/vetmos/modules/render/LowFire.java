package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import net.minecraft.entity.effect.StatusEffects;

/** Lowers / fades the first-person fire overlay. See LowFireOverlayMixin. */
public class LowFire extends Module {
    private static final String GENERAL = "Основное";

    private final DoubleSetting offset = decimal(GENERAL, "offset", "Опустить", "Насколько опустить огонь на экране.", 0.25, 0, 0.6, 2);

    private final IntSetting opacity = integer(GENERAL, "opacity", "Непрозрачность", "Непрозрачность огня, % (в ванилле 90%).", 90, 0, 100);

    private final BoolSetting hideWithResistance = bool(GENERAL, "hide-fire-resistance", "Скрывать с огнестойкостью",
        "Полностью убирать огонь, пока действует эффект огнестойкости.", true);

    public LowFire() {
        super("low-fire", "Низкий огонь", "Опускает огонь на экране при горении и делает его прозрачнее.", Category.RENDER);
    }

    private static LowFire active() {
        LowFire m = ModuleManager.get(LowFire.class);
        return m != null && m.isEnabled() ? m : null;
    }

    /** True when the overlay should not be drawn at all. */
    public static boolean shouldHide() {
        LowFire m = active();
        if (m == null) return false;
        if (m.opacity.get() <= 0) return true;
        return m.hideWithResistance.get() && mc.player != null && mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
    }

    /** How far down to move the overlay (0 when disabled). */
    public static float offset() {
        LowFire m = active();
        return m != null ? m.offset.getFloat() : 0f;
    }

    /** Alpha for the overlay vertices, given the vanilla value. */
    public static float alpha(float vanilla) {
        LowFire m = active();
        return m != null ? m.opacity.get() / 100f : vanilla;
    }
}
