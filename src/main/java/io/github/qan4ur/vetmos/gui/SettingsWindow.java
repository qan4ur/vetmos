package io.github.qan4ur.vetmos.gui;

import com.google.gson.*;
import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * Centered, draggable settings window of one module: header (title, on/off, key, reset, copy/paste, close),
 * vertical group tabs on the left, the group's settings with always-visible descriptions in the middle and a live
 * preview on the right. Lives in the ClickGUI's virtual (fit-scaled) coordinates.
 */
public class SettingsWindow {
    private static final int HEADER_H = 26;
    private static final int TAB_W = 84, TAB_H = 16;
    private static final int RADIUS = 6;
    private static final Gson GSON = new GsonBuilder().create();
    private static final String COPY_KEY = "vetmos-module";

    /** Offset of the window from the screen center, kept for the session. */
    private static int offX, offY;
    private static final Map<String, Integer> LAST_TAB = new HashMap<>();

    public final Module module;
    private final Anim.Toggle open = new Anim.Toggle(0.2f, false);
    private final List<String> groups = new ArrayList<>();
    private final Map<String, List<Widget>> byGroup = new LinkedHashMap<>();
    private final Map<String, GroupHeader> headers = new HashMap<>();
    private final List<HeaderButton> buttons = new ArrayList<>();
    private final KeybindWidget keybind;
    private final ButtonWidget resetPosition;
    final SearchField search = new SearchField();
    private int tab;
    private float scroll, scrollTarget;
    private float switchT, switchHover, tabAnimY = -1;
    private final float[] tabHover;

    // layout of the last frame (virtual coordinates)
    private int x, y, w, h;
    private int listX, listY, listW, listH, contentH;
    private int prevX, prevW;
    private List<Widget> visible = new ArrayList<>();
    private boolean dragging;
    private double dragDX, dragDY;
    private float time;

    public SettingsWindow(Module module, boolean instant) {
        this.module = module;
        if (instant) open.snap(true);
        else open.set(true);
        switchT = module.isEnabled() ? 1 : 0;
        search.placeholder = () -> Lang.ui("search-settings", "Поиск настроек…");

        for (Setting<?> s : module.getSettings()) {
            String g = s.group == null ? "Основное" : s.group;
            if (!byGroup.containsKey(g)) {
                groups.add(g);
                byGroup.put(g, new ArrayList<>());
                headers.put(g, new GroupHeader(s::displayGroup, () -> true));
            }
            SettingWidget<?> sw = create(s);
            if (sw != null) byGroup.get(g).add(new DescribedWidget(sw));
        }
        tabHover = new float[groups.size()];
        tab = Math.min(LAST_TAB.getOrDefault(module.name, 0), Math.max(0, groups.size() - 1));

        keybind = new KeybindWidget(module);
        keybind.compact = true;
        resetPosition = module instanceof HudModule
            ? new ButtonWidget(() -> Lang.ui("reset-position", "Сбросить позицию"), () -> Lang.ui("reset-position-desc", "Вернуть элемент HUD на место по умолчанию."), () -> HudManager.resetPosition(module))
            : null;

        buttons.add(new HeaderButton(() -> "↺ " + Lang.ui("reset-defaults-short", "Сбросить"),
            () -> Lang.ui("reset-defaults", "Сбросить по умолчанию") + ": " + Lang.ui("reset-defaults-desc", "все настройки модуля вернутся к значениям по умолчанию (клавиша и состояние сохраняются)."),
            () -> {
                module.resetSettings();
                Toasts.show(Lang.ui("toast-reset", "Настройки сброшены"));
            }));
        buttons.add(new HeaderButton(() -> Lang.ui("copy", "Копировать"), () -> Lang.ui("copy-desc", "Скопировать настройки модуля в буфер обмена (Ctrl+C)."), this::copySettings));
        buttons.add(new HeaderButton(() -> Lang.ui("paste", "Вставить"), () -> Lang.ui("paste-desc", "Вставить настройки модуля из буфера обмена (Ctrl+V)."), this::pasteSettings));
        buttons.add(new HeaderButton(() -> "✕", () -> Lang.ui("close-desc", "Закрыть окно (Esc)."), this::close));
    }

    private static SettingWidget<?> create(Setting<?> s) {
        if (s instanceof BoolSetting b) return new BoolWidget(b);
        if (s instanceof IntSetting i) return new SliderWidget(i);
        if (s instanceof DoubleSetting d) return new SliderWidget(d);
        if (s instanceof EnumSetting<?> en) return new EnumWidget(en);
        if (s instanceof ColorSetting c) return new ColorWidget(c);
        return null;
    }

    public void close() {
        open.set(false);
        dragging = false;
        if (KeybindWidget.listening == keybind) KeybindWidget.listening = null;
    }

    public boolean isClosing() {
        return !open.on();
    }

    /** Fully closed: the screen drops the window. */
    public boolean isDead() {
        return !open.on() && open.raw() <= 0;
    }

    public boolean isOver(double mx, double my) {
        return open.on() && Paint.inside(mx, my, x, y, w, h);
    }

    // ---------------------------------------------------------------- copy / paste

    public void copySettings() {
        JsonObject root = new JsonObject();
        root.addProperty(COPY_KEY, module.name);
        JsonObject settings = new JsonObject();
        for (Setting<?> s : module.getSettings()) settings.add(s.name, s.toJson());
        root.add("settings", settings);
        MinecraftClient.getInstance().keyboard.setClipboard(GSON.toJson(root));
        Toasts.show(Lang.ui("toast-copied-settings", "Настройки скопированы"));
    }

    public void pasteSettings() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        try {
            JsonObject root = JsonParser.parseString(clip == null ? "" : clip).getAsJsonObject();
            if (!root.has(COPY_KEY) || !root.has("settings")) throw new IllegalStateException();
            String from = root.get(COPY_KEY).getAsString();
            if (!from.equals(module.name)) {
                Toasts.show(Lang.ui("toast-other-module", "Это настройки другого модуля") + ": " + from, true);
                return;
            }
            JsonObject settings = root.getAsJsonObject("settings");
            for (Setting<?> s : module.getSettings()) if (settings.has(s.name)) s.fromJson(settings.get(s.name));
            Toasts.show(Lang.ui("toast-pasted-settings", "Настройки вставлены"));
        } catch (RuntimeException e) {
            Toasts.show(Lang.ui("toast-no-settings", "В буфере нет настроек Vetmos"), true);
        }
    }

    // ---------------------------------------------------------------- layout helpers

    private List<Widget> currentWidgets() {
        List<Widget> list = new ArrayList<>();
        String q = search.text().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            if (!groups.isEmpty()) for (Widget wd : byGroup.get(groups.get(tab))) if (wd.visible()) list.add(wd);
            return list;
        }
        for (String g : groups) {
            List<Widget> matches = new ArrayList<>();
            for (Widget wd : byGroup.get(g)) {
                if (!wd.visible() || !(wd instanceof DescribedWidget dw)) continue;
                Setting<?> s = dw.inner.setting();
                if (s.displayTitle().toLowerCase(Locale.ROOT).contains(q) || s.title.toLowerCase(Locale.ROOT).contains(q)
                    || s.displayDescription().toLowerCase(Locale.ROOT).contains(q) || s.name.contains(q)) matches.add(wd);
            }
            if (!matches.isEmpty()) {
                list.add(headers.get(g));
                list.addAll(matches);
            }
        }
        return list;
    }

    private String groupTitle(String g) {
        List<Widget> list = byGroup.get(g);
        for (Widget wd : list) if (wd instanceof DescribedWidget dw) return dw.inner.setting().displayGroup();
        return g;
    }

    // ---------------------------------------------------------------- render

    public void render(DrawContext ctx, int mx, int my, Tooltip tip, int vw, int vh) {
        open.update();
        time += Anim.dt;
        float e = open.on() ? Anim.easeOutCubic(open.raw()) : Anim.easeInOutCubic(open.raw());
        if (e <= 0) return;

        w = Math.min(vw - 20, 470);
        h = Math.min(vh - 44, 290);
        x = Math.max(4, Math.min(vw - w - 4, vw / 2 - w / 2 + offX));
        y = Math.max(32, Math.min(vh - h - 4, vh / 2 - h / 2 + 10 + offY));
        if (!open.on()) mx = my = Widget.NO_MOUSE;

        float old = Paint.alpha;
        Paint.alpha = old * e;
        var m = ctx.getMatrices();
        m.pushMatrix();
        float s = 0.92f + 0.08f * e;
        m.translate(x + w / 2f, y + h / 2f);
        m.scale(s, s);
        m.translate(-(x + w / 2f), -(y + h / 2f) + (1 - e) * 8);

        // frame
        Paint.glow(ctx, x, y, w, h, RADIUS, 0xD0000000, 10);
        Paint.glow(ctx, x, y, w, h, RADIUS, Palette.accentA(40), 3);
        Paint.roundGradient(ctx, x, y, w, h, RADIUS, 0xF8161616, 0xF80C0C0C);
        Paint.roundGradient(ctx, x, y, w, HEADER_H, RADIUS, Palette.HEADER_TOP, Palette.HEADER_BOTTOM, true, false);
        Paint.rect(ctx, x + RADIUS, y + 1, w - RADIUS * 2, 1, Palette.SHEEN);
        accentLine(ctx, x, y + HEADER_H - 1, w);
        Paint.outline(ctx, x, y, w, h, RADIUS, Palette.OUTLINE);

        renderHeader(ctx, mx, my, tip);

        int bodyY = y + HEADER_H + 6, bodyH = h - HEADER_H - 12;
        prevW = w >= 440 ? 124 : 100;
        prevX = x + w - prevW - 6;
        int tabsX = x + 6;
        boolean hasTabs = groups.size() > 1;
        int midX = hasTabs ? tabsX + TAB_W + 6 : x + 8;
        int midW = prevX - 8 - midX;

        if (hasTabs) renderTabs(ctx, mx, my, tabsX, bodyY, bodyH, tip);
        renderSettings(ctx, mx, my, tip, midX, bodyY, midW, bodyH);
        renderPreview(ctx, mx, my, tip, prevX, bodyY, prevW, bodyH);

        m.popMatrix();
        Paint.alpha = old;
    }

    /** 1px line in the accent (follows the gradient) fading out at both ends. */
    static void accentLine(DrawContext ctx, int x, int y, int w) {
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            int sx = x + w * i / seg, ex = x + w * (i + 1) / seg;
            float t = (i + 0.5f) / seg;
            float edge = Math.min(1, Math.min(t, 1 - t) * 4);
            Paint.rect(ctx, sx, y, ex - sx, 1, Anim.withAlpha(Theme.accentAt(t), Math.round(200 * edge)));
        }
    }

    private void renderHeader(DrawContext ctx, int mx, int my, Tooltip tip) {
        int cy = y + 6;
        // right-aligned buttons
        int bx = x + w - 6;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            HeaderButton b = buttons.get(i);
            String label = b.label.get();
            b.w = "✕".equals(label) ? 15 : Paint.width(label) + 12;
            bx -= b.w;
            b.x = bx;
            b.y = cy;
            b.h = 14;
            bx -= 3;
        }
        // key bind box
        keybind.w = Math.min(90, Math.max(50, keybind.preferredWidth()));
        bx -= keybind.w + 2;
        keybind.x = bx;
        keybind.y = cy - 1;
        // switch + title
        int swX = bx - 26, swY = cy + 3;
        Text title = Text.literal(module.displayTitle()).formatted(Formatting.BOLD);
        int titleMax = swX - (x + 10) - 6;
        String t = Paint.trim(module.displayTitle(), Math.max(20, titleMax - 6));
        Paint.text(ctx, Text.literal(t).formatted(Formatting.BOLD), x + 10, y + 9, Palette.TEXT);
        int titleEnd = x + 10 + Math.min(Paint.font().getWidth(title), titleMax) + 8;
        swX = Math.min(swX, Math.max(titleEnd, swX));

        boolean swHov = Paint.inside(mx, my, swX - 2, swY - 3, 22, 14);
        switchHover = Anim.approach(switchHover, swHov ? 1 : 0, 18);
        switchT = Anim.approach(switchT, module.isEnabled() ? 1 : 0, 16);
        float se = Anim.easeInOutCubic(switchT);
        int tw = 18, th = 9;
        if (se > 0.02f) Paint.glow(ctx, swX, swY, tw, th, 4, Anim.alpha(Palette.glow(), se), 3);
        Paint.round(ctx, swX, swY, tw, th, 4, Anim.mix(Palette.TRACK, Palette.accent(), se));
        Paint.outline(ctx, swX, swY, tw, th, 4, Anim.mix(Palette.CONTROL_LINE, Palette.accentBright(), Math.max(se * 0.6f, switchHover * 0.6f)));
        Paint.round(ctx, Math.round(swX + 1 + se * (tw - th)), swY + 1, th - 2, th - 2, 3, Anim.mix(0xFF9A9A9A, Palette.onAccent(), se));
        if (swHov) tip.offer("win-switch", module.isEnabled() ? Lang.ui("module-on", "Модуль включён — нажмите, чтобы выключить.") : Lang.ui("module-off", "Модуль выключен — нажмите, чтобы включить."));
        switchX = swX;
        switchY = swY;

        keybind.render(ctx, mx, my, tip);
        for (HeaderButton b : buttons) b.render(ctx, mx, my, tip);
    }

    private int switchX, switchY;

    private void renderTabs(DrawContext ctx, int mx, int my, int tx, int ty, int th, Tooltip tip) {
        boolean searching = !search.text().isBlank();
        Paint.round(ctx, tx, ty, TAB_W, th, 4, 0x60000000);
        int target = ty + 3 + tab * TAB_H;
        if (tabAnimY < 0) tabAnimY = target;
        tabAnimY = Anim.approach(tabAnimY, target, 20);
        if (!searching) {
            int ay = Math.round(tabAnimY);
            Paint.round(ctx, tx + 2, ay, TAB_W - 4, TAB_H - 1, 3, Palette.accentA(40));
            Paint.rect(ctx, tx + 2, ay + 3, 2, TAB_H - 7, Theme.accentAt((float) tab / Math.max(1, groups.size())) | 0xFF000000);
        }
        ctx.enableScissor(tx, ty, tx + TAB_W, ty + th);
        for (int i = 0; i < groups.size(); i++) {
            int ry = ty + 3 + i * TAB_H;
            boolean hov = Paint.inside(mx, my, tx + 2, ry, TAB_W - 4, TAB_H - 1) && my < ty + th;
            tabHover[i] = Anim.approach(tabHover[i], hov ? 1 : 0, 18);
            if (tabHover[i] > 0.01f) Paint.round(ctx, tx + 2, ry, TAB_W - 4, TAB_H - 1, 3, Anim.withAlpha(0xFFFFFFFF, Math.round(14 * tabHover[i])));
            boolean sel = i == tab && !searching;
            int c = searching ? Palette.TEXT_FAINT : sel ? Palette.TEXT : Anim.mix(Palette.TEXT_MUTED, Palette.TEXT_IDLE, tabHover[i]);
            Paint.text(ctx, Paint.trim(groupTitle(groups.get(i)), TAB_W - 14), tx + 8, ry + 4, c);
        }
        ctx.disableScissor();
    }

    private void renderSettings(DrawContext ctx, int mx, int my, Tooltip tip, int sx, int sy, int sw, int sh) {
        search.x = sx;
        search.y = sy;
        search.w = sw;
        search.h = 14;
        search.render(ctx, mx, my);

        listX = sx;
        listY = sy + 18;
        listW = sw;
        listH = sh - 18;
        visible = currentWidgets();
        contentH = 2;
        for (Widget wd : visible) {
            wd.w = listW - 6;
            contentH += wd.height();
        }
        contentH += 4;
        float maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Math.max(0, Math.min(maxScroll, Anim.approach(scroll, scrollTarget, 16)));

        ctx.enableScissor(listX - 2, listY, listX + listW + 2, listY + listH);
        boolean in = Paint.inside(mx, my, listX - 2, listY, listW + 4, listH);
        int wmx = in ? mx : Widget.NO_MOUSE, wmy = in ? my : Widget.NO_MOUSE;
        int cy = listY + 2 - Math.round(scroll);
        if (visible.isEmpty()) {
            String msg = groups.isEmpty() ? Lang.ui("no-settings", "У этого модуля нет настроек.") : Lang.ui("nothing-found", "Ничего не найдено");
            Paint.textCentered(ctx, msg, listX + listW / 2, listY + 12, Palette.TEXT_MUTED);
        }
        for (Widget wd : visible) {
            wd.x = listX + 1;
            wd.y = cy;
            wd.w = listW - 6;
            int hh = wd.height();
            if (cy + hh > listY && cy < listY + listH) wd.render(ctx, wmx, wmy, tip);
            cy += hh;
        }
        ctx.disableScissor();

        if (contentH > listH) {
            int trackH = listH - 4;
            int thumbH = Math.max(14, Math.round(trackH * (listH / (float) contentH)));
            int thumbY = listY + 2 + Math.round((trackH - thumbH) * (scroll / Math.max(1, maxScroll)));
            Paint.round(ctx, listX + listW - 3, listY + 2, 2, trackH, 1, 0x40000000);
            Paint.round(ctx, listX + listW - 3, thumbY, 2, thumbH, 1, Palette.accentA(180));
        }
    }

    private void renderPreview(DrawContext ctx, int mx, int my, Tooltip tip, int px, int py, int pw, int ph) {
        int boxH = Math.min(ph - (resetPosition != null ? 22 : 0), Math.max(70, ph * 3 / 5));
        if (resetPosition == null) boxH = Math.min(boxH, 92);
        Paint.round(ctx, px, py, pw, boxH, 4, 0xFF080808);
        // faint grid
        for (int gx = px + 8; gx < px + pw; gx += 8) Paint.rect(ctx, gx, py + 1, 1, boxH - 2, 0x08FFFFFF);
        for (int gy = py + 8; gy < py + boxH; gy += 8) Paint.rect(ctx, px + 1, gy, pw - 2, 1, 0x08FFFFFF);
        Paint.outline(ctx, px, py, pw, boxH, 4, Palette.OUTLINE);
        Paint.textScaled(ctx, Lang.ui("preview", "Предпросмотр").toUpperCase(Locale.ROOT), px + 4, py + 3, 0.6f, Palette.TEXT_FAINT);

        if (module instanceof HudModule hud) {
            ctx.enableScissor(px + 1, py + 1, px + pw - 1, py + boxH - 1);
            var pm = ctx.getMatrices();
            boolean failed = false;
            pm.pushMatrix();
            try {
                double hw = Math.max(1, hud.getHudWidth()), hh = Math.max(1, hud.getHudHeight());
                float s = (float) Math.min(2.0, Math.min((pw - 12) / hw, (boxH - 16) / hh));
                pm.translate((float) (px + pw / 2.0 - hw * s / 2), (float) (py + 4 + boxH / 2.0 - hh * s / 2));
                pm.scale(s, s);
                float delta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
                hud.renderHud(ctx, delta, true);
            } catch (Throwable t) {
                failed = true;
            } finally {
                pm.popMatrix();
            }
            if (failed) Paint.textCentered(ctx, Lang.ui("preview-error", "Предпросмотр недоступен"), px + pw / 2, py + boxH / 2 - 4, Palette.TEXT_MUTED);
            ctx.disableScissor();
            resetPosition.x = px;
            resetPosition.y = py + boxH + 3;
            resetPosition.w = pw;
            resetPosition.render(ctx, mx, my, tip);
        } else {
            // placeholder: a big category glyph, the state and the module's colours
            float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2);
            int cx = px + pw / 2, cy = py + boxH / 2 - 8;
            int r = 13;
            Paint.glow(ctx, cx - r, cy - r, r * 2, r * 2, r, Palette.accentA(Math.round((module.isEnabled() ? 60 : 20) + 30 * pulse)), 5);
            Paint.circle(ctx, cx, cy, r, module.isEnabled() ? Anim.withAlpha(Palette.accent(), 60) : 0x30FFFFFF);
            Paint.outline(ctx, cx - r, cy - r, r * 2, r * 2, r, module.isEnabled() ? Palette.accent() : 0x40FFFFFF);
            String icon = CategoryPanel.icon(module.category);
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(cx, cy);
            m.scale(2, 2);
            Paint.text(ctx, icon, -Paint.width(icon) / 2, -4, module.isEnabled() ? Palette.TEXT : Palette.TEXT_MUTED);
            m.popMatrix();
            String state = module.isEnabled() ? Lang.ui("state-on", "Включён") : Lang.ui("state-off", "Выключен");
            Paint.textCentered(ctx, state, cx, cy + r + 5, module.isEnabled() ? Palette.accent() : Palette.TEXT_MUTED);
            List<Integer> colors = new ArrayList<>();
            for (Setting<?> st : module.getSettings()) if (st instanceof ColorSetting c && st.isVisible()) colors.add(c.get());
            int n = Math.min(colors.size(), (pw - 8) / 12);
            int sx = cx - n * 12 / 2;
            for (int i = 0; i < n; i++) {
                Paint.checker(ctx, sx + i * 12 + 1, py + boxH - 12, 10, 8, 2);
                Paint.rect(ctx, sx + i * 12 + 1, py + boxH - 12, 10, 8, colors.get(i));
                Paint.outline(ctx, sx + i * 12, py + boxH - 13, 12, 10, 0, 0x50FFFFFF);
            }
        }

        // the module description, always visible
        int dy = py + boxH + (resetPosition != null ? 22 : 6);
        int maxY = py + ph;
        List<OrderedText> lines = Paint.font().wrapLines(StringVisitable.plain(module.displayDescription()), Math.round(pw / 0.75f));
        var m = ctx.getMatrices();
        for (OrderedText line : lines) {
            if (dy + 7 > maxY) break;
            m.pushMatrix();
            m.translate(px, dy);
            m.scale(0.75f, 0.75f);
            int c = Paint.a(Palette.TEXT_MUTED);
            if ((c >>> 24) >= 4) ctx.drawText(Paint.font(), line, 0, 0, c, false);
            m.popMatrix();
            dy += 7;
        }
    }

    // ---------------------------------------------------------------- input

    public boolean mouseClicked(double mx, double my, int button) {
        if (!open.on() || !Paint.inside(mx, my, x, y, w, h)) return false;
        for (HeaderButton b : buttons) if (b.click(mx, my, button)) return true;
        if (keybind.mouseClicked(mx, my, button)) return true;
        if (button == 0 && Paint.inside(mx, my, switchX - 2, switchY - 3, 22, 14)) {
            module.toggle();
            Widget.click(module.isEnabled() ? 1.25f : 0.9f);
            return true;
        }
        if (Paint.inside(mx, my, x, y, w, HEADER_H)) {
            if (button == 0) {
                dragging = true;
                dragDX = mx - x;
                dragDY = my - y;
            }
            return true;
        }
        search.mouseClicked(mx, my, button);
        if (groups.size() > 1) {
            int tx = x + 6, ty = y + HEADER_H + 6;
            for (int i = 0; i < groups.size(); i++) {
                if (button == 0 && Paint.inside(mx, my, tx + 2, ty + 3 + i * TAB_H, TAB_W - 4, TAB_H - 1)) {
                    if (tab != i) {
                        tab = i;
                        LAST_TAB.put(module.name, i);
                        scroll = scrollTarget = 0;
                        Widget.click(1.15f);
                    }
                    search.setText("");
                    return true;
                }
            }
        }
        if (Paint.inside(mx, my, listX - 2, listY, listW + 4, listH)) {
            for (Widget wd : visible) if (wd.mouseClicked(mx, my, button)) return true;
            return true;
        }
        if (resetPosition != null && resetPosition.mouseClicked(mx, my, button)) return true;
        return true;
    }

    public void mouseDragged(double mx, double my, int button, int vw, int vh) {
        if (dragging) {
            offX = (int) Math.round(mx - dragDX - (vw / 2.0 - w / 2.0));
            offY = (int) Math.round(my - dragDY - (vh / 2.0 - h / 2.0 + 10));
            offX = Math.max(-vw / 2, Math.min(vw / 2, offX));
            offY = Math.max(-vh / 2, Math.min(vh / 2, offY));
            return;
        }
        for (List<Widget> list : byGroup.values()) for (Widget wd : list) wd.mouseDragged(mx, my, button);
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (List<Widget> list : byGroup.values()) for (Widget wd : list) wd.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) return false;
        if (Paint.inside(mx, my, listX - 2, listY, listW + 4, listH)) {
            for (Widget wd : visible) if (wd.mouseScrolled(mx, my, amount)) return true;
        }
        scrollTarget -= (float) (amount * 24);
        return true;
    }

    /** A small text button in the header. */
    private static final class HeaderButton {
        final java.util.function.Supplier<String> label, tooltip;
        final Runnable action;
        int x, y, w, h;
        float hover, flash;

        HeaderButton(java.util.function.Supplier<String> label, java.util.function.Supplier<String> tooltip, Runnable action) {
            this.label = label;
            this.tooltip = tooltip;
            this.action = action;
        }

        void render(DrawContext ctx, int mx, int my, Tooltip tip) {
            boolean hov = Paint.inside(mx, my, x, y, w, h);
            hover = Anim.approach(hover, hov ? 1 : 0, 18);
            flash = Anim.approach(flash, 0, 6);
            ButtonWidget.draw(ctx, x, y, w, h, label.get(), hover, flash);
            if (hov) tip.offer(this, tooltip.get());
        }

        boolean click(double mx, double my, int button) {
            if (button != 0 || !Paint.inside(mx, my, x, y, w, h)) return false;
            flash = 1;
            Widget.click(1.1f);
            action.run();
            return true;
        }
    }
}
