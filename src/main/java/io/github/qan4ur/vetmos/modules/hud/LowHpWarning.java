package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/** Pulses a red vignette at the screen edges when your health is low. Full-screen, so not a movable HUD element. */
public class LowHpWarning extends Module {
    private static final String G_MAIN = "Основное";
    private static final String G_STYLE = "Стиль";

    private final DoubleSetting threshold = decimal(G_MAIN, "threshold", "Порог",
        "Здоровье (в половинках сердец), ниже которого показывается предупреждение. 8 = 4 сердца.", 8.0, 1.0, 40.0, 1);

    private final BoolSetting includeAbsorption = bool(G_MAIN, "include-absorption", "Учитывать поглощение",
        "Считать золотые сердца поглощения здоровьем.", false);

    private final BoolSetting scaleWithHealth = bool(G_MAIN, "scale-with-health", "Сила по здоровью",
        "Чем меньше здоровья, тем ярче виньетка.", true);

    private final BoolSetting ignoreCreative = bool(G_MAIN, "ignore-creative", "Не в креативе",
        "Не показывать в творческом режиме и в режиме наблюдателя.", true);

    private final BoolSetting showInScreens = bool(G_MAIN, "show-in-screens", "Показывать в меню",
        "Рисовать, даже когда открыт инвентарь или другое меню (кроме чата).", false);

    private final EnumSetting<PulseMode> pulseMode = enumeration(G_STYLE, "pulse-mode", "Пульсация", "Как пульсирует виньетка.", PulseMode.HEARTBEAT);

    private final DoubleSetting pulseSpeed = decimal(G_STYLE, "pulse-speed", "Скорость пульса", "Пульсов в секунду.", 1.2, 0.1, 5.0)
        .visibleWhen(() -> pulseMode.get() != PulseMode.STATIC);

    private final BoolSetting fasterWhenLower = bool(G_STYLE, "faster-when-lower", "Быстрее при низком HP",
        "Пульс ускоряется, когда здоровья становится меньше.", true)
        .visibleWhen(() -> pulseMode.get() != PulseMode.STATIC);

    private final DoubleSetting minPulse = decimal(G_STYLE, "min-pulse", "Минимум пульса", "Яркость между ударами пульса (0-1).", 0.35, 0.0, 1.0)
        .visibleWhen(() -> pulseMode.get() != PulseMode.STATIC);

    private final IntSetting maxAlpha = integer(G_STYLE, "max-alpha", "Макс. непрозрачность", "Непрозрачность у самого края экрана.", 160, 0, 255);

    private final IntSetting thickness = integer(G_STYLE, "thickness", "Ширина", "Насколько далеко виньетка заходит в экран (в пикселях GUI).", 70, 5, 400);

    private final ColorSetting color = color(G_STYLE, "color", "Цвет", "Цвет виньетки (прозрачность задаёт «Макс. непрозрачность»).", Theme.SCARLET);

    private final BoolSetting screenTint = bool(G_STYLE, "screen-tint", "Подкраска экрана", "Слегка подкрашивает весь экран во время пульса.", false);

    private final IntSetting tintAlpha = integer(G_STYLE, "tint-alpha", "Сила подкраски", "Максимальная непрозрачность подкраски экрана.", 25, 0, 120)
        .visibleWhen(screenTint::get);

    /** Number of linear gradient segments used to approximate the eased falloff. */
    private static final int SEGMENTS = 8;

    private final PanelRender.FrameClock clock = new PanelRender.FrameClock();
    private double fade;
    private double phase;

    public LowHpWarning() {
        super("low-hp-warning", "Низкое HP", "Пульсирующая красная виньетка по краям экрана при низком здоровье.", Category.HUD);
    }

    @Override
    public void onInit() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Vetmos.id("low_hp_warning"), (ctx, tickCounter) -> {
            if (isEnabled()) ModuleManager.safe(this, () -> render(ctx));
        });
    }

    @Override
    protected void onEnable() {
        fade = 0;
        phase = 0;
    }

    private void render(DrawContext ctx) {
        double dt = clock.tick();
        if (mc.player == null) return;

        float hp = mc.player.getHealth();
        if (includeAbsorption.get()) hp += mc.player.getAbsorptionAmount();
        double limit = threshold.get();

        boolean excluded = ignoreCreative.get() && (mc.player.isCreative() || mc.player.isSpectator());
        boolean low = !excluded && mc.player.isAlive() && hp < limit;

        // severity 0..1: 0 at the threshold, 1 at zero health
        double severity = PanelRender.clamp01(1 - hp / limit);

        fade = PanelRender.approach(fade, low ? 1 : 0, low ? 6 : 3, dt);

        double speed = pulseSpeed.get() * (fasterWhenLower.get() ? 1 + severity : 1);
        phase = (phase + dt * speed) % 1.0;

        if (fade < 0.004) {
            fade = 0;
            return;
        }
        if (!PanelRender.shouldRender(showInScreens.get())) return;

        double pulse = switch (pulseMode.get()) {
            case SINE -> 0.5 - 0.5 * Math.cos(phase * Math.PI * 2);
            case HEARTBEAT -> heartbeat(phase);
            case STATIC -> 1;
        };
        double min = pulseMode.get() == PulseMode.STATIC ? 1 : minPulse.get();
        double intensity = min + (1 - min) * pulse;
        if (scaleWithHealth.get()) intensity *= 0.45 + 0.55 * severity;
        intensity *= fade;

        int alpha = (int) Math.round(maxAlpha.get() * PanelRender.clamp01(intensity));
        if (alpha <= 0) return;

        int base = color.get() & 0x00FFFFFF;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int t = Math.min(thickness.get(), Math.min(w, h) / 2);

        if (screenTint.get()) {
            int ta = (int) Math.round(tintAlpha.get() * PanelRender.clamp01(pulse * fade * (0.5 + 0.5 * severity)));
            if (ta > 0) ctx.fill(0, 0, w, h, (ta << 24) | base);
        }

        Matrix3x2fStack m = ctx.getMatrices();

        // Only rotations are used (no mirroring) so quad winding is preserved.
        // Top: opaque at y = 0 fading downwards
        edge(ctx, w, t, base, alpha);

        // Bottom: rotated 180 degrees around the screen. (u, v) -> (w - u, h - v)
        m.pushMatrix();
        m.translate(w, h);
        m.rotate((float) Math.PI);
        edge(ctx, w, t, base, alpha);
        m.popMatrix();

        // Left: (u, v) -> (v, h - u)
        m.pushMatrix();
        m.translate(0, h);
        m.rotate((float) (-Math.PI / 2));
        edge(ctx, h, t, base, alpha);
        m.popMatrix();

        // Right: (u, v) -> (w - v, u)
        m.pushMatrix();
        m.translate(w, 0);
        m.rotate((float) (Math.PI / 2));
        edge(ctx, h, t, base, alpha);
        m.popMatrix();
    }

    /**
     * Draws a band from (0, 0) to (len, depth) in local space, opaque at local y = 0 and clear at y = depth, using an
     * eased (quadratic) falloff built from several linear gradient segments so there is no visible banding.
     */
    private void edge(DrawContext ctx, int len, int depth, int rgb, int alpha) {
        int prevY = 0;
        int prevA = alpha;
        for (int i = 1; i <= SEGMENTS; i++) {
            double f = (double) i / SEGMENTS;
            int segY = (int) Math.round(depth * f);
            double falloff = (1 - f) * (1 - f);
            int segA = (int) Math.round(alpha * falloff);
            if (segY > prevY) ctx.fillGradient(0, prevY, len, segY, (prevA << 24) | rgb, (segA << 24) | rgb);
            prevY = segY;
            prevA = segA;
        }
    }

    /** Two quick thumps followed by a rest, in 0..1. */
    private static double heartbeat(double p) {
        double a = Math.exp(-Math.pow((p - 0.08) / 0.045, 2));
        double b = 0.7 * Math.exp(-Math.pow((p - 0.27) / 0.05, 2));
        return Math.min(1, a + b);
    }

    public enum PulseMode {
        SINE("Плавно"),
        HEARTBEAT("Сердцебиение"),
        STATIC("Без пульса");

        private final String title;

        PulseMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:PulseMode." + name(), title);
        }
    }
}
