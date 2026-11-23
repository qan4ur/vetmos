package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.minecraft.client.gui.DrawContext;

import java.util.*;

/** A module row (LMB toggles, RMB expands) with its settings unfolding below. */
public class ModuleEntry {
    public static final int ROW_H = 15;
    private static final int PAD_X = 6;

    public final Module module;
    private final List<Widget> widgets = new ArrayList<>();
    private final Anim.Toggle expand;
    private float hover, on, focusPulse;

    // layout of the last frame
    private int x, y, w;
    private int contentH;
    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

    public ModuleEntry(Module module, boolean expanded) {
        this.module = module;
        this.expand = new Anim.Toggle(0.24f, expanded);
        this.on = module.isEnabled() ? 1 : 0;
        build();
    }

    private void build() {
        Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
        for (Setting<?> s : module.getSettings()) groups.computeIfAbsent(s.group == null ? "Основное" : s.group, g -> new ArrayList<>()).add(s);

        widgets.add(new GroupHeader(2));
        for (Map.Entry<String, List<Setting<?>>> e : groups.entrySet()) {
            List<Setting<?>> list = e.getValue();
            widgets.add(new GroupHeader(e.getKey(), () -> {
                for (Setting<?> s : list) if (s.isVisible()) return true;
                return false;
            }));
            for (Setting<?> s : list) {
                Widget w = create(s);
                if (w != null) widgets.add(w);
            }
        }
        widgets.add(new GroupHeader(7));
        widgets.add(new KeybindWidget(module));
        widgets.add(new ButtonWidget("Сбросить по умолчанию", "Вернуть все настройки модуля к значениям по умолчанию (клавиша и состояние сохраняются).", module::resetSettings));
        if (module instanceof HudModule) {
            widgets.add(new ButtonWidget("Сбросить позицию", "Вернуть элемент HUD на место по умолчанию.", () -> HudManager.resetPosition(module)));
        }
    }

    private static Widget create(Setting<?> s) {
        if (s instanceof BoolSetting b) return new BoolWidget(b);
        if (s instanceof IntSetting i) return new SliderWidget(i);
        if (s instanceof DoubleSetting d) return new SliderWidget(d);
        if (s instanceof EnumSetting<?> en) return new EnumWidget(en);
        if (s instanceof ColorSetting c) return new ColorWidget(c);
        return null;
    }

    public boolean isExpanded() {
        return expand.on();
    }

    public void setExpanded(boolean expanded, boolean instant) {
        if (instant) expand.snap(expanded);
        else expand.set(expanded);
    }

    public void pulse() {
        focusPulse = 1;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase(Locale.ROOT).trim();
        return module.title.toLowerCase(Locale.ROOT).contains(q) || module.name.toLowerCase(Locale.ROOT).contains(q)
            || module.name.replace('-', ' ').toLowerCase(Locale.ROOT).contains(q);
    }

    private int fullContentHeight() {
        int h = 0;
        for (Widget wd : widgets) if (wd.visible()) h += wd.height();
        return h + 4;
    }

    /** Height this entry takes this frame (row + animated settings). */
    public int height() {
        return ROW_H + contentHeightNow();
    }

    private int contentHeightNow() {
        float e = expand.eased();
        if (e <= 0) return 0;
        return Math.round(fullContentHeight() * e);
    }

    /**
     * @param clipTop/clipBottom vertical bounds of the visible panel body (mouse outside is ignored)
     * @return the height drawn
     */
    public int render(DrawContext ctx, int x, int y, int w, int mx, int my, Tooltip tip, int clipTop, int clipBottom) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
        expand.update();
        boolean mouseIn = my >= clipTop && my < clipBottom;
        boolean rowHov = mouseIn && Paint.inside(mx, my, x, y, w, ROW_H);
        hover = Anim.approach(hover, rowHov ? 1 : 0, 18);
        on = Anim.approach(on, module.isEnabled() ? 1 : 0, 12);
        focusPulse = Anim.approach(focusPulse, 0, 1.2f);

        // row background
        if (on > 0.01f) {
            Paint.gradientH(ctx, x, y, w, ROW_H, Palette.accentA(Math.round(62 * on)), Palette.accentA(Math.round(16 * on)));
            Paint.rect(ctx, x, y, w, 1, Palette.accentA(Math.round(36 * on)));
        }
        if (hover > 0.01f) Paint.rect(ctx, x, y, w, ROW_H, Anim.withAlpha(0xFFFFFFFF, Math.round(16 * hover)));
        if (focusPulse > 0.01f) {
            float p = focusPulse * (0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 90.0));
            Paint.outline(ctx, x, y, w, ROW_H, 0, Palette.accentA(Math.round(255 * p)));
        }

        // accent bar
        int barH = Math.round((ROW_H - 4) * Anim.easeOutCubic(on));
        if (barH > 0) {
            int by = y + (ROW_H - barH) / 2;
            Paint.glow(ctx, x, by, 2, barH, 1, Anim.alpha(Palette.glow(), on), 2);
            Paint.rect(ctx, x, by, 2, barH, Palette.accent());
        }

        // title
        int titleColor = Anim.mix(Anim.mix(0xFFB4B4B4, 0xFFECECEC, hover), Palette.TEXT, on);
        int tx = x + PAD_X + Math.round(on * 2);
        Paint.text(ctx, Paint.trim(module.title, w - PAD_X - 18), tx, y + 4, titleColor);

        // chevron (rotates when expanded)
        float e = expand.eased();
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x + w - 8, y + ROW_H / 2f);
        m.rotate((float) (e * Math.PI / 2));
        int chev = Anim.mix(Anim.mix(0xFF6A6A6A, 0xFFFFFFFF, hover), Palette.accent(), e * (1 - on * 0.5f));
        if (on > 0.5f) chev = Anim.mix(chev, Palette.TEXT, 0.6f);
        Paint.text(ctx, "›", -Paint.width("›") / 2, -4, chev);
        m.popMatrix();

        if (rowHov) tip.offer(module, module.title, module.description);

        // settings
        contentH = contentHeightNow();
        if (contentH > 0) {
            int top = y + ROW_H;
            ctx.enableScissor(x, top, x + w, top + contentH);
            Paint.gradientV(ctx, x, top, w, contentH, 0x60000000, 0x38000000);
            Paint.rect(ctx, x, top, 1, contentH, Anim.mix(0x50FFFFFF, Palette.accent(), on * 0.7f));
            float old = Paint.alpha;
            Paint.alpha = old * Anim.clamp01(e * 1.5f);
            boolean contentMouse = mouseIn && Paint.inside(mx, my, x, top, w, contentH);
            int wmx = contentMouse ? mx : Widget.NO_MOUSE, wmy = contentMouse ? my : Widget.NO_MOUSE;
            int cy = top + 1;
            for (Widget wd : widgets) {
                if (!wd.visible()) continue;
                wd.x = x + PAD_X;
                wd.y = cy;
                wd.w = w - PAD_X - 5;
                if (cy < top + contentH && cy + wd.height() > top) wd.render(ctx, wmx, wmy, tip);
                cy += wd.height();
            }
            Paint.alpha = old;
            ctx.disableScissor();
        }
        return ROW_H + contentH;
    }

    private boolean inClip(double my) {
        return my >= clipTop && my < clipBottom;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!inClip(my)) return false;
        if (Paint.inside(mx, my, x, y, w, ROW_H)) {
            if (button == 0) {
                module.toggle();
                Widget.click(module.isEnabled() ? 1.25f : 0.9f);
            } else if (button == 1) {
                expand.set(!expand.on());
                Widget.click(expand.on() ? 1.1f : 0.85f);
            }
            return true;
        }
        if (contentH > 0 && expand.on() && Paint.inside(mx, my, x, y + ROW_H, w, contentH)) {
            for (Widget wd : widgets) {
                if (wd.visible() && wd.mouseClicked(mx, my, button)) return true;
            }
            return true;
        }
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        for (Widget wd : widgets) wd.mouseReleased(mx, my, button);
    }

    public void mouseDragged(double mx, double my, int button) {
        for (Widget wd : widgets) if (wd.visible()) wd.mouseDragged(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inClip(my) || contentH <= 0 || !expand.on()) return false;
        for (Widget wd : widgets) if (wd.visible() && wd.mouseScrolled(mx, my, amount)) return true;
        return false;
    }
}
