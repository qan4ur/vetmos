package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.util.Lang;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Real time and / or the in-game time of day. */
public class ClockHud extends InfoHudModule {
    public enum Show {
        REAL("Реальное"), GAME("Игровое"), BOTH("Оба");

        private final String title;

        Show(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:ClockHud.Show." + name(), title); }
    }

    private final EnumSetting<Show> show = enumeration(G, "show", "Показывать", "Реальное время, игровое или оба.", Show.REAL);
    private final BoolSetting hour12 = bool(G, "12-hour", "12-часовой формат", "Время в формате AM/PM.", false);
    private final BoolSetting seconds = bool(G, "seconds", "Секунды", "Показывать секунды реального времени.", false);
    private final BoolSetting day = bool(G, "day", "Номер дня", "Добавлять номер игрового дня.", true)
        .visibleWhen(() -> show.get() != Show.REAL);

    public ClockHud() {
        super("clock-hud", "Время", "Показывает реальное и/или игровое время.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.TOP, -4, 4);
    }

    private String format(int h, int m, int s, boolean withSeconds) {
        String suffix = "";
        if (hour12.get()) {
            suffix = h < 12 ? " AM" : " PM";
            h = h % 12;
            if (h == 0) h = 12;
        }
        String t = (hour12.get() ? String.valueOf(h) : String.format(Locale.ROOT, "%02d", h)) + ":" + String.format(Locale.ROOT, "%02d", m);
        if (withSeconds) t += ":" + String.format(Locale.ROOT, "%02d", s);
        return t + suffix;
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        Show mode = show.get();
        if (mode != Show.GAME) {
            LocalTime now = LocalTime.now();
            out.add(new Line(label("time", "Время"), format(now.getHour(), now.getMinute(), now.getSecond(), seconds.get())));
        }
        if (mode != Show.REAL) {
            long t = preview ? 6000 + 24000L * 41 : mc.world.getTimeOfDay();
            long tod = Math.floorMod(t, 24000L);
            int h = (int) ((tod / 1000 + 6) % 24);
            int m = (int) (tod % 1000 * 60 / 1000);
            String v = format(h, m, 0, false);
            if (day.get()) v += "  " + label("day", "день") + " " + (Math.floorDiv(t, 24000L) + 1);
            out.add(new Line(label("game-time", "Игра"), v));
        }
    }
}
