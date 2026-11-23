package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Zoom on a vanilla key bind (default C), hold or toggle. The module being enabled means "zoom available".
 * Mixins: ZoomGameRendererMixin (FOV + hand), ZoomMouseMixin (wheel + sensitivity).
 */
public class Zoom extends Module {
    private static final String GENERAL = "Основное";
    private static final String ANIM = "Анимация";
    private static final String WHEEL = "Колёсико мыши";
    private static final String VIEW = "Вид";

    private static final Identifier VIGNETTE_TEXTURE = Identifier.ofVanilla("textures/misc/vignette.png");

    public enum Mode {
        HOLD("Удержание"), TOGGLE("Переключение");
        private final String title;
        Mode(String title) { this.title = title; }
        @Override
        public String toString() { return Lang.tr("enum:Zoom.Mode." + name(), title); }
    }

    public enum Easing {
        LINEAR("Линейная"), EASE_OUT("Плавное замедление"), EASE_IN_OUT("Плавно-плавно"), EXPO("Экспонента"), BACK("Пружина");
        private final String title;
        Easing(String title) { this.title = title; }
        @Override
        public String toString() { return Lang.tr("enum:Zoom.Easing." + name(), title); }

        double apply(double t) {
            t = Math.max(0, Math.min(1, t));
            return switch (this) {
                case LINEAR -> t;
                case EASE_OUT -> 1 - Math.pow(1 - t, 3);
                case EASE_IN_OUT -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
                case EXPO -> t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
                case BACK -> {
                    double c1 = 1.70158, c3 = c1 + 1;
                    yield 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
                }
            };
        }
    }

    // ---------------------------------------------------------------- settings

    private final io.github.qan4ur.vetmos.setting.KeySetting zoomKey = key(GENERAL, "key", "Клавиша зума",
        "Клавиша (или кнопка мыши), по которой работает зум. Не конфликтует с ванильными биндами.", GLFW.GLFW_KEY_C);
    private final EnumSetting<Mode> mode = enumeration(GENERAL, "mode", "Режим",
        "Удержание: зум, пока зажата клавиша. Переключение: нажал — включил, нажал ещё раз — выключил.", Mode.HOLD);
    private final DoubleSetting factor = decimal(GENERAL, "factor", "Кратность",
        "Во сколько раз приближать.", 4.0, 1.5, 50.0, 1);
    private final BoolSetting resetOnRelease = bool(GENERAL, "reset-on-release", "Сбрасывать кратность",
        "После отпускания зум возвращается к базовой кратности; иначе запоминается кратность, выбранная колёсиком.", true);

    private final BoolSetting smooth = bool(ANIM, "smooth", "Плавный зум",
        "Плавное приближение и отдаление.", true);
    private final DoubleSetting speed = decimal(ANIM, "speed", "Скорость анимации",
        "Насколько быстро проходит приближение (больше — быстрее).", 6.0, 1.0, 20.0, 1);
    private final EnumSetting<Easing> easing = enumeration(ANIM, "easing", "Сглаживание",
        "Кривая анимации зума.", Easing.EASE_OUT);

    private final BoolSetting scroll = bool(WHEEL, "scroll", "Менять колёсиком",
        "Во время зума колёсико мыши меняет кратность (слоты хотбара не листаются).", true);
    private final DoubleSetting scrollStep = decimal(WHEEL, "scroll-step", "Шаг",
        "На сколько процентов меняется кратность за одно деление колёсика.", 25.0, 5.0, 100.0, 0);
    private final DoubleSetting scrollMin = decimal(WHEEL, "scroll-min", "Минимум",
        "Минимальная кратность при прокрутке.", 1.5, 1.1, 10.0, 1);
    private final DoubleSetting scrollMax = decimal(WHEEL, "scroll-max", "Максимум",
        "Максимальная кратность при прокрутке.", 50.0, 2.0, 100.0, 1);

    private final BoolSetting reduceSensitivity = bool(VIEW, "reduce-sensitivity", "Снижать чувствительность",
        "Чувствительность мыши уменьшается пропорционально приближению.", true);
    private final BoolSetting cinematic = bool(VIEW, "cinematic", "Кинематографичная камера",
        "Во время зума включается плавная камера (как на F8).", false);
    private final BoolSetting hideHand = bool(VIEW, "hide-hand", "Скрывать руку",
        "Не рисовать руку и предмет в ней во время зума.", true);
    private final BoolSetting vignette = bool(VIEW, "vignette", "Виньетка",
        "Лёгкое затемнение краёв экрана во время зума.", false);
    private final DoubleSetting vignetteStrength = decimal(VIEW, "vignette-strength", "Сила виньетки",
        "Насколько сильно затемнять края.", 0.5, 0.1, 1.0, 2);
    private final BoolSetting vignetteRed = bool(VIEW, "vignette-red", "Красный оттенок",
        "Виньетка с красным оттенком кровавой луны.", true);

    // ---------------------------------------------------------------- state

    private boolean keyWasDown;
    private boolean active;
    private boolean toggled;
    /** Zoom level chosen for the current / last zoom (base factor or changed with the wheel). */
    private double targetLevel = -1;
    /** Smoothed zoom level (follows targetLevel). */
    private double shownLevel = 1;
    /** 0 = not zoomed, 1 = fully zoomed (before easing). */
    private double progress;
    private long lastFrameNanos;
    /** Final FOV divisor of the last frame. */
    private double currentFactor = 1;

    private boolean smoothCamApplied;
    private boolean prevSmoothCam;

    public Zoom() {
        super("zoom", "Зум", "Встроенный зум: держи клавишу (по умолчанию C, меняется в настройках) - камера приближается.", Category.RENDER, true, Keys.NONE);
        scrollStep.visibleWhen(scroll::get);
        scrollMin.visibleWhen(scroll::get);
        scrollMax.visibleWhen(scroll::get);
        speed.visibleWhen(smooth::get);
        easing.visibleWhen(smooth::get);
        vignetteStrength.visibleWhen(vignette::get);
        vignetteRed.visibleWhen(vignette::get);
        mode.onChange(() -> toggled = false);
    }

    @Override
    public void onInit() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = client.currentScreen == null && zoomKey.isPressed();
            boolean pressedNow = down && !keyWasDown;
            keyWasDown = down;
            if (!isEnabled() || client.player == null || client.world == null) {
                toggled = false;
                return;
            }
            if (mode.get() == Mode.TOGGLE && pressedNow) toggled = !toggled;
        });
        HudElementRegistry.addFirst(Identifier.of("vetmos", "zoom_vignette"), (ctx, tc) -> renderVignette(ctx));
    }

    @Override
    protected void onDisable() {
        toggled = false;
        setActive(false);
        progress = 0;
        currentFactor = 1;
    }

    @Override
    public void onLeave() {
        toggled = false;
        setActive(false);
    }

    // ---------------------------------------------------------------- logic

    private boolean wantsZoom() {
        if (!isEnabled() || mc.player == null || mc.world == null) return false;
        if (mode.get() == Mode.HOLD) return mc.currentScreen == null && zoomKey.isPressed();
        return toggled;
    }

    private void setActive(boolean value) {
        if (active == value) return;
        active = value;
        if (value) {
            if (resetOnRelease.get() || targetLevel < 0) targetLevel = factor.get();
            if (!smooth.get()) shownLevel = targetLevel;
            if (cinematic.get() && !smoothCamApplied) {
                prevSmoothCam = mc.options.smoothCameraEnabled;
                mc.options.smoothCameraEnabled = true;
                smoothCamApplied = true;
            }
        } else if (smoothCamApplied) {
            mc.options.smoothCameraEnabled = prevSmoothCam;
            smoothCamApplied = false;
        }
    }

    /** Advances the animation; called every frame from the FOV hook. Returns the FOV divisor. */
    private double update() {
        long now = System.nanoTime();
        double dt = lastFrameNanos == 0 ? 0 : Math.min(0.1, (now - lastFrameNanos) / 1e9);
        lastFrameNanos = now;

        setActive(wantsZoom());
        if (targetLevel < 0) targetLevel = factor.get();
        if (shownLevel < 1) shownLevel = 1;

        if (smooth.get()) {
            double rate = speed.get();
            progress += (active ? 1 : -1) * dt * rate / 2.0;
            progress = Math.max(0, Math.min(1, progress));
            double k = 1 - Math.exp(-dt * rate * 1.5);
            shownLevel = Math.exp(Math.log(shownLevel) + (Math.log(targetLevel) - Math.log(shownLevel)) * k);
        } else {
            progress = active ? 1 : 0;
            shownLevel = targetLevel;
        }

        double e = smooth.get() ? easing.get().apply(progress) : progress;
        // Interpolate in log space so the zoom feels linear to the eye; BACK may overshoot slightly.
        currentFactor = Math.max(0.5, Math.exp(Math.log(shownLevel) * e));
        return currentFactor;
    }

    private boolean onScroll(double vertical) {
        if (!active || !scroll.get() || vertical == 0) return false;
        double step = 1 + scrollStep.get() / 100.0;
        double min = Math.min(scrollMin.get(), scrollMax.get());
        double max = Math.max(scrollMin.get(), scrollMax.get());
        targetLevel = vertical > 0 ? targetLevel * step : targetLevel / step;
        targetLevel = Math.max(min, Math.min(max, targetLevel));
        if (!smooth.get()) shownLevel = targetLevel;
        return true;
    }

    private void renderVignette(DrawContext ctx) {
        if (!isEnabled() || !vignette.get() || currentFactor <= 1.001) return;
        double fullAt = Math.max(1.5, targetLevel);
        float amount = (float) Math.max(0, Math.min(1, Math.log(currentFactor) / Math.log(fullAt)));
        float d = amount * vignetteStrength.getFloat();
        if (d <= 0.003f) return;
        int color = vignetteRed.get() ? ColorHelper.fromFloats(1f, d * 0.35f, d, d) : ColorHelper.fromFloats(1f, d, d, d);
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        ctx.drawTexture(RenderPipelines.VIGNETTE, VIGNETTE_TEXTURE, 0, 0, 0f, 0f, w, h, w, h, color);
    }

    // ---------------------------------------------------------------- mixin entry points

    private static Zoom instance() {
        return ModuleManager.get(Zoom.class);
    }

    /** FOV divisor for this frame (1 = no zoom). */
    public static double fovDivisor() {
        Zoom m = instance();
        if (m == null) return 1;
        if (!m.isEnabled()) {
            m.currentFactor = 1;
            return 1;
        }
        return m.update();
    }

    /** Mouse delta multiplier (1 = vanilla). */
    public static double sensitivityMultiplier() {
        Zoom m = instance();
        if (m == null || !m.isEnabled() || !m.reduceSensitivity.get() || m.currentFactor <= 1) return 1;
        return 1 / m.currentFactor;
    }

    public static boolean shouldHideHand() {
        Zoom m = instance();
        return m != null && m.isEnabled() && m.hideHand.get() && (m.active || m.currentFactor > 1.15);
    }

    /** Called on mouse wheel in game; returns true when the scroll was consumed by the zoom. */
    public static boolean handleScroll(double vertical) {
        Zoom m = instance();
        return m != null && m.isEnabled() && m.onScroll(vertical);
    }
}
