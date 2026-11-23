package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.modules.hud.PanelRender;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of the small text info elements (FPS, ping, coordinates...). A subclass fills a list of label/value lines
 * every frame; this class lays them out in two aligned columns on a monochrome panel with an accent top line.
 * Subclasses may draw extra content under the lines (see {@link #extraHeight()} / {@link #renderExtra}).
 */
public abstract class InfoHudModule extends Module implements HudModule {
    protected static final String G = "Основное", C = "Цвета";

    /** One row: label (may be empty) and value. {@code valueColor == 0} means the normal text colour. */
    protected record Line(String label, String value, int valueColor) {
        public Line(String label, String value) {
            this(label, value, 0);
        }
    }

    protected final BoolSetting background = bool(G, "background", "Фон", "Рисовать тёмную панель под текстом.", true);
    protected final BoolSetting accentLine = bool(G, "accent-line", "Акцентная линия", "Тонкая линия цвета акцента сверху панели.", true)
        .visibleWhen(background::get);
    protected final BoolSetting showLabel = bool(G, "label", "Подпись", "Показывать подпись перед значением.", true);
    protected final BoolSetting compact = bool(G, "compact", "Компактный вид", "Меньше отступов и строк.", false);
    protected final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    protected final ColorSetting bgColor = color(C, "background-color", "Фон", "Цвет панели.", Theme.PANEL)
        .visibleWhen(background::get);
    protected final ColorSetting textColor = color(C, "text", "Текст", "Цвет значений.", Theme.TEXT);
    protected final BoolSetting labelAccent = bool(C, "label-accent", "Подпись цветом акцента",
        "Красить подписи в цвет акцента интерфейса.", true).visibleWhen(showLabel::get);
    protected final ColorSetting labelColor = color(C, "label-color", "Подпись", "Цвет подписей (когда акцент выключен).", Theme.TEXT_SECONDARY)
        .visibleWhen(() -> showLabel.get() && !labelAccent.get());

    private final List<Line> lines = new ArrayList<>();
    private final PanelRender.FrameClock clock = new PanelRender.FrameClock();
    private double shownWidth = -1;
    private int targetWidth = 1, boxHeight = 1, labelColumn;

    protected InfoHudModule(String name, String title, String description) {
        super(name, title, description, Category.HUD);
    }

    /** Fills the rows to show. {@code preview} is true when there is no world (draw sample values). */
    protected abstract void collect(List<Line> out, boolean preview);

    /** Height of extra content drawn under the rows (0 = none). */
    protected int extraHeight() {
        return 0;
    }

    /** Minimum width the extra content needs (inner width, without padding). */
    protected int extraWidth() {
        return 0;
    }

    /** Draws extra content in the box (x, y, w, extraHeight()) in GUI units. */
    protected void renderExtra(DrawContext ctx, int x, int y, int w, boolean preview) {}

    /** Whether there is something to show while not in the editor (default: in a world). */
    protected boolean hasContent() {
        return true;
    }

    // ---------------------------------------------------------------- helpers for subclasses

    protected static boolean noWorld() {
        return mc.player == null || mc.world == null;
    }

    /** HUD label in the selected language; keys are {@code hud:<key>}. */
    protected static String label(String key, String russian) {
        return Lang.tr("hud:" + key, russian);
    }

    protected int labelColour() {
        return labelAccent.get() ? Theme.accent() : labelColor.get();
    }

    protected boolean isCompact() {
        return compact.get();
    }

    protected int padX() {
        return compact.get() ? 3 : 5;
    }

    protected int padY() {
        return compact.get() ? 2 : 4;
    }

    protected int lineHeight() {
        return compact.get() ? 9 : 11;
    }

    // ---------------------------------------------------------------- HudModule

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public boolean shouldRenderHud() {
        return !noWorld() && hasContent();
    }

    @Override
    public double getHudWidth() {
        layout();
        return shownWidth < 0 ? targetWidth : Math.max(Math.ceil(shownWidth), 1);
    }

    @Override
    public double getHudHeight() {
        return boxHeight;
    }

    private void layout() {
        lines.clear();
        collect(lines, noWorld());
        TextRenderer tr = mc.textRenderer;
        boolean labels = showLabel.get();
        int lw = 0, vw = 0;
        for (Line l : lines) {
            if (labels && !l.label().isEmpty()) lw = Math.max(lw, tr.getWidth(l.label() + ":"));
            vw = Math.max(vw, tr.getWidth(l.value()));
        }
        labelColumn = lw > 0 ? lw + 4 : 0;
        int inner = Math.max(labelColumn + vw, extraWidth());
        targetWidth = Math.max(inner + padX() * 2, 8);
        int content = lines.isEmpty() ? 0 : lines.size() * lineHeight() - (lineHeight() - 8);
        int extra = extraHeight();
        if (extra > 0) content += (content > 0 ? 3 : 0) + extra;
        boxHeight = Math.max(content + padY() * 2 + (background.get() && accentLine.get() ? 1 : 0), 8);
        if (shownWidth < 0) shownWidth = targetWidth;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        layout();
        double dt = clock.tick();
        // narrow quickly toward the new size, but grow at once so the text never overflows
        shownWidth = targetWidth > shownWidth ? targetWidth : PanelRender.approach(shownWidth, targetWidth, 10, dt);
        int w = (int) Math.ceil(shownWidth), h = boxHeight;

        boolean line = background.get() && accentLine.get();
        if (background.get()) {
            PanelRender.roundedRect(ctx, 0, 0, w, h, compact.get() ? 1 : 2, bgColor.get());
            if (line) {
                int in = compact.get() ? 1 : 2;
                PanelRender.hGradient(ctx, in, 0, w - in, 1, Theme.accentAt(0), Theme.accentAt(1));
            }
        }

        int x = padX(), y = padY() + (line ? 1 : 0);
        boolean labels = showLabel.get(), sh = shadow.get();
        int lc = labelColour(), tc = textColor.get();
        for (Line l : lines) {
            if (labels && !l.label().isEmpty()) PanelRender.text(ctx, l.label() + ":", x, y, lc, sh);
            PanelRender.text(ctx, l.value(), x + labelColumn, y, l.valueColor() != 0 ? l.valueColor() : tc, sh);
            y += lineHeight();
        }
        int extra = extraHeight();
        if (extra > 0) {
            if (!lines.isEmpty()) y += 3 - (lineHeight() - 8);
            renderExtra(ctx, x, y, w - padX() * 2, noWorld());
        }
    }
}
