package io.github.qan4ur.vetmos.modules.world;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;

/**
 * Client-side sky color, fog color/distance and time of day.
 * Hooks: SkyClientWorldMixin (sky color + rendered time of day), SkyFogRendererMixin (fog).
 * The real time of day is untouched: only what the client renders changes.
 * Fog is never touched while the camera entity has Blindness or Darkness.
 */
public class CustomSkyFog extends Module {
    private static final String SKY = "Небо";
    private static final String FOG = "Туман";
    private static final String TIME = "Время";

    // Sky

    private final BoolSetting customSky = bool(SKY, "custom-sky-color", "Свой цвет неба", "Заменить цвет неба.", true);

    private final ColorSetting skyColor = color(SKY, "sky-color", "Цвет неба", "Цвет неба.", ColorSetting.rgb(30, 30, 36))
        .visibleWhen(customSky::get);

    // Fog

    private final BoolSetting customFogColor = bool(FOG, "custom-fog-color", "Свой цвет тумана",
        "Заменить цвет тумана и горизонта (на воздухе).", true);

    private final ColorSetting fogColor = color(FOG, "fog-color", "Цвет тумана", "Цвет тумана.", ColorSetting.rgb(70, 70, 78))
        .visibleWhen(customFogColor::get);

    private final EnumSetting<FogMode> fogMode = enumeration(FOG, "fog-distance", "Дальность тумана",
        "Ванильная, умноженная или без тумана на воздухе.", FogMode.VANILLA);

    private final DoubleSetting fogMultiplier = decimal(FOG, "distance-multiplier", "Множитель дальности",
        "Множитель начала и конца тумана. Больше 1 — туман дальше.", 1.5, 0.1, 5, 2)
        .visibleWhen(() -> fogMode.get() == FogMode.MULTIPLIER);

    private final BoolSetting affectFluids = bool(FOG, "affect-fluids", "В жидкостях",
        "Менять дальность тумана и под водой, в лаве и в рыхлом снегу.", false)
        .visibleWhen(() -> fogMode.get() != FogMode.VANILLA);

    // Time

    private final BoolSetting fixedTime = bool(TIME, "fixed-time", "Фиксированное время",
        "Показывать мир в заданное время суток (только у тебя).", false);

    private final IntSetting time = integer(TIME, "time", "Время суток",
        "Время в тиках: 1000 — утро, 6000 — полдень, 13000 — закат, 18000 — полночь.", 18000, 0, 24000)
        .visibleWhen(fixedTime::get);

    public CustomSkyFog() {
        super("custom-sky-fog", "Небо и туман", "Свой цвет неба и тумана, дальность тумана и время суток на клиенте.", Category.WORLD);
    }

    // Called from mixins

    /** ARGB sky color, or null for vanilla. */
    public Integer getSkyColor() {
        if (!isEnabled() || !customSky.get()) return null;
        return skyColor.get();
    }

    /** Fixed time of day, or -1 for vanilla. */
    public long getFixedTime() {
        if (!isEnabled() || !fixedTime.get()) return -1;
        return time.get();
    }

    public Vector4f modifyFogColor(Vector4f original, CameraSubmersionType submersion) {
        if (!isEnabled() || !customFogColor.get() || isFluid(submersion) || hasVisionEffect()) return original;
        int c = fogColor.get();
        return new Vector4f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (c >>> 24) / 255f);
    }

    public boolean shouldModifyFogDistance(CameraSubmersionType submersion) {
        if (!isEnabled() || fogMode.get() == FogMode.VANILLA) return false;
        if (hasVisionEffect()) return false;
        return affectFluids.get() || !isFluid(submersion);
    }

    public boolean disableFog() {
        return fogMode.get() == FogMode.DISABLED;
    }

    public float fogMultiplier() {
        return fogMultiplier.getFloat();
    }

    private static boolean isFluid(CameraSubmersionType type) {
        return type == CameraSubmersionType.WATER || type == CameraSubmersionType.LAVA || type == CameraSubmersionType.POWDER_SNOW;
    }

    private boolean hasVisionEffect() {
        Entity camera = mc.getCameraEntity();
        return camera instanceof LivingEntity living && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS));
    }

    public enum FogMode {
        VANILLA("Ванильная"),
        MULTIPLIER("Множитель"),
        DISABLED("Без тумана");

        private final String title;

        FogMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:FogMode." + name(), title);
        }
    }
}
