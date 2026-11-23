package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Accent palette: preset swatches, the language switch, a custom colour picker and the gradient options. */
public class PalettePopover extends Popover {
    private static final int SW = 14, GAP = 4;
    private final InterfaceModule ui;
    private final List<Widget> widgets = new ArrayList<>();
    private final float[] swHover = new float[Theme.PRESETS.length];
    private final float[] langHover = new float[2];

    public PalettePopover() {
        w = 168;
        ui = ModuleManager.get(InterfaceModule.class);
        if (ui != null) {
            widgets.add(new ColorWidget(ui.accent).label(() -> Lang.ui("custom-color", "Свой цвет")));
            widgets.add(new BoolWidget(ui.gradient).label(() -> Lang.ui("gradient", "Градиент")));
            widgets.add(new ColorWidget(ui.accent2));
            widgets.add(new SliderWidget(ui.gradientSpeed));
        }
    }

    private int swatchX(int i) {
        int total = Theme.PRESETS.length * (SW + GAP) - GAP;
        return x + (w - total) / 2 + i * (SW + GAP);
    }

    private int langBoxX(int i) {
        return x + w - 8 - (2 - i) * 24;
    }

    @Override
    protected int contentHeight() {
        int hh = 62;
        for (Widget wd : widgets) if (wd.visible()) hh += wd.height();
        return hh + 6;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, Tooltip tip) {
        caption(ctx, Lang.ui("accent", "Акцент"), x + 8, y + 6);
        int accent = Palette.accent();
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            int sx = swatchX(i), sy = y + 16;
            boolean hov = Paint.inside(mx, my, sx - 1, sy - 1, SW + 2, SW + 2);
            swHover[i] = Anim.approach(swHover[i], hov ? 1 : 0, 18);
            boolean sel = (accent & 0xFFFFFF) == (Theme.PRESETS[i] & 0xFFFFFF);
            int grow = Math.round(swHover[i]);
            if (sel || swHover[i] > 0.02f) Paint.glow(ctx, sx - grow, sy - grow, SW + grow * 2, SW + grow * 2, 4, Anim.withAlpha(Theme.PRESETS[i], sel ? 110 : Math.round(80 * swHover[i])), 3);
            Paint.round(ctx, sx - grow, sy - grow, SW + grow * 2, SW + grow * 2, 4, Theme.PRESETS[i]);
            if (sel) {
                Paint.outline(ctx, sx - 2, sy - 2, SW + 4, SW + 4, 5, 0xFFFFFFFF);
                int dot = Palette.luminance(Theme.PRESETS[i]) > 0.6f ? 0xFF101010 : 0xFFFFFFFF;
                Paint.circle(ctx, sx + SW / 2, sy + SW / 2, 2, dot);
            }
            if (hov) tip.offer("preset" + i, (Lang.isEnglish() ? Theme.PRESET_NAMES_EN : Theme.PRESET_NAMES_RU)[i]);
        }

        // language switch
        int ly = y + 40;
        Paint.text(ctx, Lang.ui("language", "Язык"), x + 8, ly + 3, Palette.TEXT_IDLE);
        boolean en = Lang.isEnglish();
        for (int i = 0; i < 2; i++) {
            int bx = langBoxX(i);
            boolean hov = Paint.inside(mx, my, bx, ly, 22, 14);
            langHover[i] = Anim.approach(langHover[i], hov ? 1 : 0, 18);
            boolean sel = (i == 1) == en;
            Paint.round(ctx, bx, ly, 22, 14, 3, sel ? Palette.accentA(70) : Anim.mix(Palette.CONTROL, Palette.CONTROL_HOVER, langHover[i]));
            Paint.outline(ctx, bx, ly, 22, 14, 3, sel ? Palette.accent() : Anim.mix(Palette.CONTROL_LINE, Palette.CONTROL_LINE_HOVER, langHover[i]));
            Paint.textCentered(ctx, i == 0 ? "RU" : "EN", bx + 11, ly + 3, sel ? Palette.TEXT : Palette.TEXT_MUTED);
            if (hov) tip.offer("lang" + i, i == 0 ? "Русский" : "English");
        }

        if (ui == null) return;
        int cy = y + 60;
        for (Widget wd : widgets) {
            if (!wd.visible()) continue;
            wd.x = x + 8;
            wd.y = cy;
            wd.w = w - 16;
            wd.render(ctx, mx, my, tip);
            cy += wd.height();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOver(mx, my)) return false;
        if (ui == null) return true;
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            if (button == 0 && Paint.inside(mx, my, swatchX(i) - 1, y + 15, SW + 2, SW + 2)) {
                ui.accent.set(Theme.PRESETS[i]);
                Widget.click(1.2f);
                return true;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (button == 0 && Paint.inside(mx, my, langBoxX(i), y + 40, 22, 14)) {
                ui.language.set(i == 0 ? InterfaceModule.Language.RU : InterfaceModule.Language.EN);
                Widget.click(1.2f);
                return true;
            }
        }
        for (Widget wd : widgets) if (wd.visible() && wd.mouseClicked(mx, my, button)) return true;
        return true;
    }

    @Override
    public void mouseDragged(double mx, double my, int button) {
        for (Widget wd : widgets) wd.mouseDragged(mx, my, button);
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        for (Widget wd : widgets) wd.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) return false;
        for (Widget wd : widgets) if (wd.visible() && wd.mouseScrolled(mx, my, amount)) return true;
        return true;
    }
}
