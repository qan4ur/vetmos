package io.github.qan4ur.vetmos.modules.hud.vanilla;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Lang;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Movable replacement of the vanilla scoreboard sidebar. Mirrors vanilla's logic (team-colour sidebar slots, hidden
 * entries, sorting, team-decorated names, per-objective / per-entry number formats) but lets the user hide the score
 * numbers and style the panel.
 */
public class ScoreboardHud extends Module implements HudModule {
    public enum ScoreAlign {
        LEFT("Слева"), CENTER("По центру"), RIGHT("Справа");

        private final String title;

        ScoreAlign(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:ScoreAlign." + name(), title); }
    }

    private static final String G = "Основное", C = "Цвета";

    /** Same order as vanilla: score descending, then holder name case-insensitive. */
    private static final Comparator<ScoreboardEntry> ORDER = Comparator.comparingInt((ScoreboardEntry e) -> e.value()).reversed()
        .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);

    private final BoolSetting hideNumbers = bool(G, "hide-numbers", "Скрыть числа", "Не показывать красные числа очков справа.", true);
    private final IntSetting maxLines = integer(G, "max-lines", "Макс. строк", "Сколько строк показывать (в ванилле 15).", 15, 1, 30);
    private final IntSetting lineSpacing = integer(G, "line-spacing", "Интервал строк", "Дополнительный отступ между строками.", 0, 0, 6);
    private final EnumSetting<ScoreAlign> align = enumeration(G, "align", "Выравнивание", "Выравнивание текста строк.", ScoreAlign.LEFT);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", false);
    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting background = color(C, "background", "Фон", "Цвет и прозрачность фона (строка заголовка чуть темнее).", ColorSetting.argb(0, 0, 0, 77));
    private final ColorSetting textColor = color(C, "text", "Текст", "Цвет текста без своего форматирования.", 0xFFFFFFFF);

    private record Row(Text name, Text score, int scoreWidth) {}

    private Text title;
    private final List<Row> rows = new ArrayList<>();
    private int contentWidth;

    public ScoreboardHud() {
        super("scoreboard-hud", "Скорборд", "Перемещаемый скорборд справа: можно скрыть красные числа и настроить фон.", Category.HUD);
    }

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (context, tickCounter) -> {
            if (!isEnabled()) original.render(context, tickCounter);
        });
    }

    // ---------------------------------------------------------------- data

    private ScoreboardObjective findObjective() {
        if (mc.world == null) return null;
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = null;
        if (mc.player != null) {
            Team team = scoreboard.getScoreHolderTeam(mc.player.getNameForScoreboard());
            if (team != null) {
                ScoreboardDisplaySlot slot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
                if (slot != null) objective = scoreboard.getObjectiveForSlot(slot);
            }
        }
        return objective != null ? objective : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private void refresh(boolean editor) {
        title = null;
        rows.clear();
        TextRenderer tr = mc.textRenderer;
        ScoreboardObjective objective = findObjective();
        if (objective != null) {
            Scoreboard scoreboard = objective.getScoreboard();
            NumberFormat format = objective.getNumberFormatOr(StyledNumberFormat.RED);
            List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream()
                .filter(e -> !e.hidden()).sorted(ORDER).limit(maxLines.get()).toList();
            for (ScoreboardEntry e : entries) {
                Team team = scoreboard.getScoreHolderTeam(e.owner());
                Text name = Team.decorateName(team, e.name());
                Text score = hideNumbers.get() ? null : e.formatted(format);
                rows.add(new Row(name, score, score == null ? 0 : tr.getWidth(score)));
            }
            title = objective.getDisplayName();
        } else if (editor) {
            sample(tr);
        }

        if (title == null) {
            contentWidth = 0;
            return;
        }
        int width = tr.getWidth(title);
        int colon = tr.getWidth(":");
        for (Row row : rows) width = Math.max(width, tr.getWidth(row.name) + (row.scoreWidth > 0 ? colon + row.scoreWidth : 0));
        contentWidth = width;
    }

    private void sample(TextRenderer tr) {
        title = Text.literal("Vetmos").formatted(Formatting.BOLD);
        Text[] lines = {
            Text.literal(Lang.tr("hud:sb-mode", "Режим: ")).formatted(Formatting.GRAY).append(Text.literal(Lang.tr("hud:sb-duels", "Дуэли")).formatted(Formatting.WHITE)),
            Text.literal(Lang.tr("hud:sb-kills", "Убийства: ")).formatted(Formatting.GRAY).append(Text.literal("7").formatted(Formatting.GREEN)),
            Text.literal(Lang.tr("hud:sb-deaths", "Смерти: ")).formatted(Formatting.GRAY).append(Text.literal("2").formatted(Formatting.RED)),
            Text.empty(),
            Text.literal(Lang.tr("hud:sb-online", "Онлайн: ")).formatted(Formatting.GRAY).append(Text.literal("128").formatted(Formatting.WHITE)),
            Text.literal("play.server.net").formatted(Formatting.YELLOW)
        };
        int n = Math.min(lines.length, maxLines.get());
        for (int i = 0; i < n; i++) {
            Text score = hideNumbers.get() ? null : StyledNumberFormat.RED.format(lines.length - i);
            rows.add(new Row(lines[i], score, score == null ? 0 : tr.getWidth(score)));
        }
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.CENTER, -1, 0);
    }

    @Override
    public boolean shouldRenderHud() {
        refresh(false);
        return title != null;
    }

    @Override
    public double getHudWidth() {
        refresh(io.github.qan4ur.vetmos.hud.HudManager.isEditorOpen());
        return title == null ? 60 : contentWidth + 4;
    }

    @Override
    public double getHudHeight() {
        return 10 + Math.max(rows.size(), title == null ? 3 : 0) * (9 + lineSpacing.get());
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        if (title == null) return;
        TextRenderer tr = mc.textRenderer;
        int lineH = 9 + lineSpacing.get();
        int w = contentWidth + 4;
        int h = 10 + rows.size() * lineH;

        int bg = background.get();
        int a = bg >>> 24;
        int titleBg = (Math.min(255, Math.round(a * 4f / 3f)) << 24) | (bg & 0xFFFFFF);
        if ((titleBg >>> 24) > 0) context.fill(0, 0, w, 10, titleBg);
        if (a > 0 && !rows.isEmpty()) context.fill(0, 10, w, h, bg);

        int color = textColor.get() | 0xFF000000;
        boolean sh = shadow.get();
        context.drawText(tr, title, 2 + contentWidth / 2 - tr.getWidth(title) / 2, 1, color, sh);

        int colon = tr.getWidth(":");
        int pad = lineSpacing.get() / 2;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = 10 + i * lineH + pad;
            int nameW = tr.getWidth(row.name);
            int column = contentWidth - (row.scoreWidth > 0 ? colon + row.scoreWidth : 0);
            int x = switch (align.get()) {
                case LEFT -> 2;
                case CENTER -> 2 + (column - nameW) / 2;
                case RIGHT -> 2 + column - nameW;
            };
            context.drawText(tr, row.name, x, y, color, sh);
            if (row.score != null && row.scoreWidth > 0) context.drawText(tr, row.score, w - row.scoreWidth, y, color, sh);
        }
    }
}
