package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;

import java.util.List;

/** Frames per second. */
public class FpsHud extends InfoHudModule {
    private final BoolSetting colorize = bool(G, "color-by-value", "Цвет по значению", "Красить число: низкий FPS жёлтым и красным.", false);
    private final IntSetting lowFps = integer(G, "low-fps", "Низкий FPS", "Ниже этого значения число красится в цвет тревоги.", 30, 5, 240)
        .visibleWhen(colorize::get);

    public FpsHud() {
        super("fps-hud", "FPS", "Показывает количество кадров в секунду.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.LEFT, HudPosition.AnchorY.TOP, 4, 4);
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        int fps = preview ? 144 : mc.getCurrentFps();
        int color = 0;
        if (colorize.get()) {
            int low = lowFps.get();
            color = fps < low ? Quality.BAD : fps < low * 2 ? Quality.MEDIUM : Quality.GOOD;
        }
        out.add(new Line(label("fps", "FPS"), String.valueOf(fps), color));
    }
}
