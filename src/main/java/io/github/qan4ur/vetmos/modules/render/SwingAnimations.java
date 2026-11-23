package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

/**
 * First-person hand/item tweaks. Purely client-side rendering.
 * Hooks: SwingHeldItemRendererMixin (transforms + equip bob), SwingLivingEntityMixin (local swing duration).
 */
public class SwingAnimations extends Module {
    private static final String GENERAL = "Основное";
    private static final String MAIN = "Основная рука";
    private static final String OFF = "Вторая рука";

    // General

    public final EnumSetting<Preset> preset = enumeration(GENERAL, "preset", "Пресет",
        "«Классика 1.7» включает старый набор анимаций: взмах 6 тиков, без покачивания при смене и без провала после удара.", Preset.CUSTOM);

    private final IntSetting swingDuration = integer(GENERAL, "swing-duration", "Длительность взмаха",
        "Длина взмаха от первого лица в тиках (ванильно 6, больше — медленнее).", 6, 1, 30)
        .visibleWhen(this::custom);

    private final BoolSetting ignoreEffects = bool(GENERAL, "ignore-haste-fatigue", "Игнорировать спешку/усталость",
        "Использовать длительность взмаха, даже если Спешка или Утомление её меняют.", true)
        .visibleWhen(this::custom);

    private final BoolSetting noSwapAnimation = bool(GENERAL, "no-swap-animation", "Без анимации смены",
        "Убирает опускание/подъём предмета при смене слота (как в 1.7/1.8).", true)
        .visibleWhen(this::custom);

    private final BoolSetting noCooldownDip = bool(GENERAL, "no-cooldown-dip", "Без провала после удара",
        "Убирает опускание предмета после каждого удара из-за перезарядки атаки (как в 1.8).", true)
        .visibleWhen(this::custom);

    private final BoolSetting affectEmptyHand = bool(GENERAL, "affect-empty-hand", "Пустая рука",
        "Применять позицию, поворот и размер основной руки и к голой руке.", false);

    // Main hand

    private final DoubleSetting scaleMain = decimal(MAIN, "main-scale", "Размер", "Размер предмета в основной руке.", 1, 0.1, 3);
    private final DoubleSetting posMainX = decimal(MAIN, "main-offset-x", "Смещение X", "Смещение предмета в основной руке по X.", 0, -1, 1);
    private final DoubleSetting posMainY = decimal(MAIN, "main-offset-y", "Смещение Y", "Смещение предмета в основной руке по Y.", 0, -1, 1);
    private final DoubleSetting posMainZ = decimal(MAIN, "main-offset-z", "Смещение Z", "Смещение предмета в основной руке по Z.", 0, -1, 1);
    private final DoubleSetting rotMainX = decimal(MAIN, "main-rotation-x", "Поворот X", "Поворот предмета в основной руке по X (градусы).", 0, -180, 180, 0);
    private final DoubleSetting rotMainY = decimal(MAIN, "main-rotation-y", "Поворот Y", "Поворот предмета в основной руке по Y (градусы).", 0, -180, 180, 0);
    private final DoubleSetting rotMainZ = decimal(MAIN, "main-rotation-z", "Поворот Z", "Поворот предмета в основной руке по Z (градусы).", 0, -180, 180, 0);

    // Off hand

    private final DoubleSetting scaleOff = decimal(OFF, "off-scale", "Размер", "Размер предмета во второй руке.", 1, 0.1, 3);
    private final DoubleSetting posOffX = decimal(OFF, "off-offset-x", "Смещение X", "Смещение предмета во второй руке по X.", 0, -1, 1);
    private final DoubleSetting posOffY = decimal(OFF, "off-offset-y", "Смещение Y", "Смещение предмета во второй руке по Y.", 0, -1, 1);
    private final DoubleSetting posOffZ = decimal(OFF, "off-offset-z", "Смещение Z", "Смещение предмета во второй руке по Z.", 0, -1, 1);
    private final DoubleSetting rotOffX = decimal(OFF, "off-rotation-x", "Поворот X", "Поворот предмета во второй руке по X (градусы).", 0, -180, 180, 0);
    private final DoubleSetting rotOffY = decimal(OFF, "off-rotation-y", "Поворот Y", "Поворот предмета во второй руке по Y (градусы).", 0, -180, 180, 0);
    private final DoubleSetting rotOffZ = decimal(OFF, "off-rotation-z", "Поворот Z", "Поворот предмета во второй руке по Z (градусы).", 0, -180, 180, 0);

    public SwingAnimations() {
        super("swing-animations", "Анимации взмаха", "Позиция, размер и взмах предмета от первого лица (в стиле 1.7/1.8).", Category.RENDER);
    }

    private boolean custom() {
        return preset.get() == Preset.CUSTOM;
    }

    private boolean classic() {
        return preset.get() == Preset.CLASSIC_17;
    }

    // Called from mixins

    /** Only modifies the local player's swing, and only in first person. */
    public int modifySwingDuration(int original) {
        if (!isEnabled() || mc.options == null || !mc.options.getPerspective().isFirstPerson()) return original;

        if (classic()) return 6;
        if (!ignoreEffects.get() && original != 6) return original;
        return swingDuration.get();
    }

    public boolean noSwapAnimation() {
        return isEnabled() && (classic() || noSwapAnimation.get());
    }

    public boolean noCooldownDip() {
        return isEnabled() && (classic() || noCooldownDip.get());
    }

    public void transformItem(Hand hand, MatrixStack matrices) {
        if (!isEnabled()) return;

        if (hand == Hand.MAIN_HAND) {
            apply(matrices, posMainX.get(), posMainY.get(), posMainZ.get(), rotMainX.get(), rotMainY.get(), rotMainZ.get(), scaleMain.get());
        } else {
            apply(matrices, posOffX.get(), posOffY.get(), posOffZ.get(), rotOffX.get(), rotOffY.get(), rotOffZ.get(), scaleOff.get());
        }
    }

    public void transformEmptyHand(MatrixStack matrices) {
        if (!isEnabled() || !affectEmptyHand.get()) return;
        apply(matrices, posMainX.get(), posMainY.get(), posMainZ.get(), rotMainX.get(), rotMainY.get(), rotMainZ.get(), scaleMain.get());
    }

    private static void apply(MatrixStack matrices, double px, double py, double pz, double rx, double ry, double rz, double scale) {
        if (px != 0 || py != 0 || pz != 0) matrices.translate((float) px, (float) py, (float) pz);
        if (rx != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rx));
        if (ry != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) ry));
        if (rz != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rz));
        if (scale != 1) matrices.scale((float) scale, (float) scale, (float) scale);
    }

    public enum Preset {
        CUSTOM("Своя"),
        CLASSIC_17("Классика 1.7");

        private final String title;

        Preset(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
