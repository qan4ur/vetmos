package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.modules.hud.PanelRender;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;

/** Cardinal direction and yaw, with an optional compass strip. */
public class DirectionHud extends InfoHudModule {
    private static final String[] RU4 = { "С", "В", "Ю", "З" };
    private static final String[] EN4 = { "N", "E", "S", "W" };
    private static final String[] RU8 = { "С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ" };
    private static final String[] EN8 = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
    private static final String[] AXIS = { "-Z", "+X", "+Z", "-X" };

    private final BoolSetting yaw = bool(G, "yaw", "Угол", "Показывать угол поворота в градусах.", true);
    private final BoolSetting eight = bool(G, "eight-directions", "8 направлений", "Учитывать промежуточные стороны (СВ, ЮЗ...).", false);
    private final BoolSetting axis = bool(G, "axis", "Ось", "Показывать ось, вдоль которой вы смотрите (+X, -Z...).", false);
    private final BoolSetting strip = bool(G, "compass", "Полоса компаса", "Рисовать полосу компаса под текстом.", false);
    private final BoolSetting fullNames = bool(G, "letters-in-english", "Буквы N/E/S/W", "Английские буквы сторон света вместо русских (в английском интерфейсе всегда).", false);

    private float heading;

    public DirectionHud() {
        super("direction-hud", "Направление", "Показывает сторону света и угол поворота, по желанию — полосу компаса.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.TOP, 0, 4);
    }

    /** 0 = north, 90 = east, 180 = south, 270 = west. */
    private float heading(boolean preview, float tickDelta) {
        if (preview) return 152.5f;
        float y = mc.player.getYaw(tickDelta);
        return MathHelper.floorMod(y + 180f, 360f);
    }

    private boolean english() {
        return fullNames.get() || io.github.qan4ur.vetmos.util.Lang.isEnglish();
    }

    private String[] names4() {
        return english() ? EN4 : RU4;
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        heading = heading(preview, mc.getRenderTickCounter().getTickProgress(true));
        String dir = eight.get()
            ? (english() ? EN8 : RU8)[Math.round(heading / 45f) % 8]
            : names4()[Math.round(heading / 90f) % 4];
        StringBuilder sb = new StringBuilder(dir);
        if (axis.get()) sb.append(" (").append(AXIS[Math.round(heading / 90f) % 4]).append(')');
        if (yaw.get()) sb.append("  ").append(String.format(Locale.ROOT, "%.1f°", heading));
        out.add(new Line(label("direction", "Курс"), sb.toString()));
    }

    @Override
    protected int extraHeight() {
        return strip.get() ? (isCompact() ? 11 : 13) : 0;
    }

    @Override
    protected int extraWidth() {
        return strip.get() ? (isCompact() ? 90 : 110) : 0;
    }

    @Override
    protected void renderExtra(DrawContext ctx, int x, int y, int w, boolean preview) {
        TextRenderer tr = mc.textRenderer;
        int h = extraHeight();
        int cx = x + w / 2;
        double range = 75; // degrees visible on each side
        double ppd = (w / 2.0) / range;
        int text = textColor.get();
        boolean sh = shadow.get();

        // baseline
        ctx.fill(x, y + h - 1, x + w, y + h, PanelRender.withAlpha(Theme.MID, 0.5));

        int first = (int) Math.ceil((heading - range) / 15.0) * 15;
        for (int d = first; d <= heading + range; d += 15) {
            int px = (int) Math.round(cx + (d - heading) * ppd);
            double fade = 1 - Math.abs(d - heading) / range;
            fade = PanelRender.clamp01(fade * 1.6);
            int norm = Math.floorMod(d, 360);
            if (norm % 90 == 0) {
                String s = names4()[norm / 90];
                int c = norm == 0 ? Theme.accent() : text;
                PanelRender.text(ctx, s, px - tr.getWidth(s) / 2 + 1, y, PanelRender.withAlpha(c, fade), sh);
                ctx.fill(px, y + h - 3, px + 1, y + h - 1, PanelRender.withAlpha(text, fade));
            } else if (norm % 45 == 0) {
                ctx.fill(px, y + h - 4, px + 1, y + h - 1, PanelRender.withAlpha(Theme.GREY, fade * 0.9));
            } else {
                ctx.fill(px, y + h - 2, px + 1, y + h - 1, PanelRender.withAlpha(Theme.MID, fade));
            }
        }

        // centre marker
        int accent = Theme.accent();
        ctx.fill(cx, y + h - 6, cx + 1, y + h, accent);
        ctx.fill(cx - 1, y + h - 6, cx + 2, y + h - 5, accent);
    }
}
