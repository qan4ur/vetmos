package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import io.github.qan4ur.vetmos.util.Lang;

import java.util.List;
import java.util.Locale;

/** Movement speed of the player (horizontal by default), averaged over a few ticks. */
public class SpeedHud extends InfoHudModule {
    public enum Unit {
        BPS("Блоков/с"), KMH("Км/ч");

        private final String title;

        Unit(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:SpeedHud.Unit." + name(), title); }
    }

    private final EnumSetting<Unit> unit = enumeration(G, "unit", "Единицы", "Блоки в секунду или километры в час.", Unit.BPS);
    private final BoolSetting vertical = bool(G, "vertical", "С вертикалью", "Учитывать вертикальную скорость (падение, прыжки).", false);
    private final IntSetting smoothing = integer(G, "smoothing", "Сглаживание", "За сколько тиков усреднять скорость.", 5, 1, 20);
    private final IntSetting decimals = integer(G, "decimals", "Знаков после запятой", "Точность значения.", 1, 0, 2);

    private final double[] samples = new double[20];
    private int sampleIndex, sampleCount;
    private double lastX, lastY, lastZ;
    private boolean hasLast;

    public SpeedHud() {
        super("speed-hud", "Скорость", "Показывает скорость движения в блоках в секунду.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.TOP, -4, 42);
    }

    @Override
    protected void onEnable() {
        hasLast = false;
        sampleCount = 0;
    }

    @Override
    public void onLeave() {
        hasLast = false;
        sampleCount = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            hasLast = false;
            return;
        }
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (hasLast) {
            double dx = x - lastX, dy = vertical.get() ? y - lastY : 0, dz = z - lastZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > 50) d = 0; // teleport
            samples[sampleIndex] = d * 20;
            sampleIndex = (sampleIndex + 1) % samples.length;
            sampleCount = Math.min(sampleCount + 1, samples.length);
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasLast = true;
    }

    private double speed() {
        int n = Math.min(smoothing.get(), sampleCount);
        if (n <= 0) return 0;
        double sum = 0;
        for (int i = 1; i <= n; i++) sum += samples[Math.floorMod(sampleIndex - i, samples.length)];
        return sum / n;
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        double bps = preview ? 5.61 : speed();
        boolean kmh = unit.get() == Unit.KMH;
        double v = kmh ? bps * 3.6 : bps;
        String u = kmh ? label("kmh", "км/ч") : label("bps", "б/с");
        out.add(new Line(label("speed", "Скорость"), String.format(Locale.ROOT, "%." + decimals.get() + "f", v) + " " + u));
    }
}
