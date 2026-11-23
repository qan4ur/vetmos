package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
import java.util.function.Consumer;

/** A module row: LMB toggles, RMB or the ⚙ button opens the settings window. */
public class ModuleEntry {
    public static final int ROW_H = 15;
    private static final int PAD_X = 6;
    private static final int GEAR_W = 13;

    public final Module module;
    private final Consumer<Module> openSettings;
    private float hover, gearHover, on, focusPulse;

    private int x, y, w;
    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

    public ModuleEntry(Module module, Consumer<Module> openSettings) {
        this.module = module;
        this.openSettings = openSettings;
        this.on = module.isEnabled() ? 1 : 0;
    }

    public void pulse() {
        focusPulse = 1;
    }

    /** Matches the displayed title, the Russian title and the command name. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase(Locale.ROOT).trim();
        return module.displayTitle().toLowerCase(Locale.ROOT).contains(q)
            || module.title.toLowerCase(Locale.ROOT).contains(q)
            || module.name.toLowerCase(Locale.ROOT).contains(q)
            || module.name.replace('-', ' ').toLowerCase(Locale.ROOT).contains(q);
    }

    public int height() {
        return ROW_H;
    }

    private boolean gearHit(double mx, double my) {
        return Paint.inside(mx, my, x + w - GEAR_W - 1, y, GEAR_W + 1, ROW_H);
    }

    public int render(DrawContext ctx, int x, int y, int w, int mx, int my, Tooltip tip, int clipTop, int clipBottom) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
        boolean mouseIn = my >= clipTop && my < clipBottom;
        boolean rowHov = mouseIn && Paint.inside(mx, my, x, y, w, ROW_H);
        boolean gh = rowHov && gearHit(mx, my);
        hover = Anim.approach(hover, rowHov ? 1 : 0, 18);
        gearHover = Anim.approach(gearHover, gh ? 1 : 0, 18);
        on = Anim.approach(on, module.isEnabled() ? 1 : 0, 12);
        focusPulse = Anim.approach(focusPulse, 0, 1.2f);

        if (on > 0.01f) {
            Paint.gradientH(ctx, x, y, w, ROW_H, Palette.accentA(Math.round(62 * on)), Palette.accentA(Math.round(16 * on)));
            Paint.rect(ctx, x, y, w, 1, Palette.accentA(Math.round(36 * on)));
        }
        if (hover > 0.01f) Paint.rect(ctx, x, y, w, ROW_H, Anim.withAlpha(0xFFFFFFFF, Math.round(16 * hover)));
        if (focusPulse > 0.01f) {
            float p = focusPulse * (0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 90.0));
            Paint.outline(ctx, x, y, w, ROW_H, 0, Palette.accentA(Math.round(255 * p)));
        }

        // accent bar (gradient-aware: the colour depends on the row position on screen)
        int barH = Math.round((ROW_H - 4) * Anim.easeOutCubic(on));
        if (barH > 0) {
            int by = y + (ROW_H - barH) / 2;
            int c = Theme.accentAt(Math.floorMod(y + x, 400) / 400f) | 0xFF000000;
            Paint.glow(ctx, x, by, 2, barH, 1, Anim.alpha(Anim.withAlpha(c, 100), on), 2);
            Paint.rect(ctx, x, by, 2, barH, c);
        }

        int titleColor = Anim.mix(Anim.mix(0xFFB4B4B4, 0xFFECECEC, hover), Palette.TEXT, on);
        int tx = x + PAD_X + Math.round(on * 2);
        Paint.text(ctx, Paint.trim(module.displayTitle(), w - PAD_X - GEAR_W - 4), tx, y + 4, titleColor);

        // gear button: faint until the row is hovered, turns a little when hovered itself
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x + w - GEAR_W / 2f - 1, y + ROW_H / 2f);
        m.rotate((float) (gearHover * Math.PI / 3));
        int gc = Anim.mix(Anim.mix(0x605E5E5E, 0xFFBDBDBD, hover), Palette.accent(), gearHover);
        Paint.text(ctx, "⚙", -Paint.width("⚙") / 2, -4, gc);
        m.popMatrix();

        if (gh) tip.offer(module.name + "#gear", Lang.ui("open-settings", "Открыть настройки модуля (или ПКМ по строке)."));
        else if (rowHov) tip.offer(module, module.displayTitle(), module.displayDescription());
        return ROW_H;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (my < clipTop || my >= clipBottom) return false;
        if (!Paint.inside(mx, my, x, y, w, ROW_H)) return false;
        if (button == 1 || (button == 0 && gearHit(mx, my))) {
            Widget.click(1.1f);
            openSettings.accept(module);
        } else if (button == 0) {
            module.toggle();
            Widget.click(module.isEnabled() ? 1.25f : 0.9f);
        }
        return true;
    }
}
