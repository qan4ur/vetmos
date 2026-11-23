package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import io.github.qan4ur.vetmos.util.Lang;

/** Draggable panel of one category. LMB on the header drags it, RMB folds it. The body scrolls with the wheel. */
public class CategoryPanel {
    public static final int WIDTH = 124;
    public static final int HEADER_H = 20;
    private static final int RADIUS = 5;

    public final Category category;
    public int x, y;
    private final List<ModuleEntry> entries = new ArrayList<>();
    private final Anim.Toggle fold;
    private float scroll, scrollTarget;
    private float headerHover;
    private boolean dragging;
    private double dragDX, dragDY;

    // layout of the last frame
    private int bodyH, contentH, maxBody;
    private List<ModuleEntry> shown = new ArrayList<>();
    private ModuleEntry pendingFocus;

    public CategoryPanel(Category category, int x, int y, boolean folded, Consumer<Module> openSettings) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.fold = new Anim.Toggle(0.25f, !folded);
        for (Module m : ModuleManager.inCategory(category)) entries.add(new ModuleEntry(m, openSettings));
    }

    public List<ModuleEntry> entries() {
        return entries;
    }

    public boolean isFolded() {
        return !fold.on();
    }

    /** True once the user dragged the panel (it then keeps its own position instead of the automatic layout). */
    public boolean moved;

    public boolean isDragging() {
        return dragging;
    }

    public int totalHeight() {
        return HEADER_H + bodyH;
    }

    /** Unfolds the panel, scrolls to the module and highlights it. */
    public void focus(ModuleEntry entry) {
        fold.snap(true);
        entry.pulse();
        pendingFocus = entry;
    }

    static String icon(Category c) {
        return switch (c.name()) { // by name so new categories don't break compilation
            case "COMBAT" -> "⚔";
            case "HUD" -> "▣";
            case "RENDER" -> "◈";
            case "WORLD" -> "☾";
            case "CLIENT" -> "⚙";
            default -> "•";
        };
    }

    public void render(DrawContext ctx, int mx, int my, Tooltip tip, String query, int screenW, int screenH) {
        render(ctx, mx, my, tip, query, screenW, screenH, Integer.MAX_VALUE);
    }

    /** @param bottomLimit the panel body never extends below this y (e.g. the top of a panel placed below it) */
    public void render(DrawContext ctx, int mx, int my, Tooltip tip, String query, int screenW, int screenH, int bottomLimit) {
        fold.update();
        x = Math.max(2, Math.min(screenW - WIDTH - 2, x));
        y = Math.max(32, Math.min(screenH - HEADER_H - 2, y));

        shown = new ArrayList<>();
        for (ModuleEntry e : entries) if (e.matches(query)) shown.add(e);

        contentH = 3;
        for (ModuleEntry e : shown) contentH += e.height();
        if (shown.isEmpty()) contentH = 18;
        contentH += 3;
        maxBody = Math.max(40, Math.min(screenH - y - HEADER_H - 22, bottomLimit - y - HEADER_H));

        if (pendingFocus != null) {
            int off = 3;
            for (ModuleEntry e : shown) {
                if (e == pendingFocus) break;
                off += e.height();
            }
            scrollTarget = off - 4;
            pendingFocus = null;
        }

        float maxScroll = Math.max(0, contentH - maxBody);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Anim.approach(scroll, scrollTarget, 16);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        float f = fold.eased();
        bodyH = Math.round(Math.min(contentH, maxBody) * f);
        int total = HEADER_H + bodyH;

        boolean hh = Paint.inside(mx, my, x, y, WIDTH, HEADER_H);
        headerHover = Anim.approach(headerHover, hh || dragging ? 1 : 0, 16);

        // shadow + glow
        Paint.glow(ctx, x, y, WIDTH, total, RADIUS, Palette.SHADOW, 6);
        Paint.glow(ctx, x, y, WIDTH, total, RADIUS, Palette.accentA(Math.round(28 + 40 * headerHover)), 3);

        // body
        if (bodyH > 0) {
            Paint.roundGradient(ctx, x, y + HEADER_H - RADIUS, WIDTH, bodyH + RADIUS, RADIUS, Palette.PANEL_TOP, Palette.PANEL_BOTTOM, false, true);
        }

        // header
        boolean roundBottom = bodyH <= 0;
        Paint.roundGradient(ctx, x, y, WIDTH, HEADER_H, RADIUS,
            Anim.mix(Palette.HEADER_TOP, Palette.HEADER_TOP_HOVER, headerHover), Anim.mix(Palette.HEADER_BOTTOM, Palette.HEADER_BOTTOM_HOVER, headerHover), true, roundBottom);
        Paint.rect(ctx, x + RADIUS, y + 1, WIDTH - RADIUS * 2, 1, Palette.SHEEN);
        if (bodyH > 0) {
            Paint.rect(ctx, x, y + HEADER_H - 1, WIDTH, 1, Anim.alpha(Palette.accentA(200), f));
            Paint.gradientV(ctx, x, y + HEADER_H, WIDTH, 4, Anim.alpha(Palette.accentA(40), f), Palette.accentA(0));
        }
        Paint.outline(ctx, x, y, WIDTH, total, RADIUS, Palette.OUTLINE);

        Text title = Text.literal(category.displayTitle()).formatted(Formatting.BOLD);
        int tw = Paint.font().getWidth(title);
        String ic = icon(category);
        int iw = Paint.width(ic);
        int startX = x + (WIDTH - tw - iw - 5) / 2;
        Paint.text(ctx, ic, startX, y + 6, Palette.accent());
        Paint.text(ctx, title, startX + iw + 5, y + 6, Palette.TEXT);

        int enabled = 0;
        for (ModuleEntry e : entries) if (e.module.isEnabled()) enabled++;
        String count = enabled + "/" + entries.size();
        SmallText.right(ctx, count, x + WIDTH - 6, y + 7, Palette.TEXT_MUTED);

        if (hh) tip.offer(this, category.displayTitle(), Lang.ui("panel-hint", "ЛКМ — перетащить панель, ПКМ — свернуть или развернуть.") + " " + Lang.ui("panel-enabled", "Включено модулей") + ": " + enabled + "/" + entries.size());

        // entries
        if (bodyH > 2) {
            int top = y + HEADER_H, bottom = y + HEADER_H + bodyH - 2;
            ctx.enableScissor(x + 1, top, x + WIDTH - 1, bottom);
            boolean bodyMouse = Paint.inside(mx, my, x, top, WIDTH, bottom - top);
            int emx = bodyMouse ? mx : Widget.NO_MOUSE, emy = bodyMouse ? my : Widget.NO_MOUSE;
            int cy = top + 3 - Math.round(scroll);
            boolean scrollable = contentH > maxBody;
            int ew = WIDTH - 4 - (scrollable ? 3 : 0);
            if (shown.isEmpty()) {
                Paint.textCentered(ctx, Lang.ui("nothing-found", "Ничего не найдено"), x + WIDTH / 2, top + 6, Palette.TEXT_MUTED);
            }
            for (ModuleEntry e : shown) {
                int h = e.height();
                if (cy + h >= top && cy <= bottom) cy += e.render(ctx, x + 2, cy, ew, emx, emy, tip, top, bottom);
                else {
                    e.render(ctx, x + 2, -10000, ew, Widget.NO_MOUSE, Widget.NO_MOUSE, tip, top, bottom); // keep animations running
                    cy += h;
                }
            }
            ctx.disableScissor();

            if (scrollable) {
                int trackH = bottom - top - 6;
                int thumbH = Math.max(12, Math.round(trackH * (maxBody / (float) contentH)));
                int thumbY = top + 3 + Math.round((trackH - thumbH) * (scroll / Math.max(1, maxScroll)));
                Paint.round(ctx, x + WIDTH - 4, top + 3, 2, trackH, 1, 0x40000000);
                Paint.round(ctx, x + WIDTH - 4, thumbY, 2, thumbH, 1, Palette.accentA(190));
            }
        }
    }

    /** True when the point is over the panel. */
    public boolean isOver(double mx, double my) {
        return Paint.inside(mx, my, x - 1, y - 1, WIDTH + 2, HEADER_H + bodyH + 2);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (Paint.inside(mx, my, x, y, WIDTH, HEADER_H)) {
            if (button == 0) {
                dragging = true;
                dragDX = mx - x;
                dragDY = my - y;
            } else if (button == 1) {
                fold.set(!fold.on());
                Widget.click(fold.on() ? 1.1f : 0.8f);
            }
            return true;
        }
        if (!isOver(mx, my)) return false;
        if (bodyH > 2) {
            for (ModuleEntry e : shown) if (e.mouseClicked(mx, my, button)) return true;
        }
        return true;
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
    }

    public void mouseDragged(double mx, double my, int button) {
        if (dragging) {
            x = (int) Math.round(mx - dragDX);
            y = (int) Math.round(my - dragDY);
            moved = true;
        }
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) return false;
        if (contentH > maxBody) scrollTarget -= (float) (amount * 22);
        return true;
    }

    /** Small helper for right-aligned scaled text. */
    private static final class SmallText {
        static void right(DrawContext ctx, String s, int right, int y, int color) {
            float sc = 0.75f;
            Paint.textScaled(ctx, s, right - Paint.width(s) * sc, y, sc, color);
        }
    }
}
