package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;

/**
 * Vetmos ClickGUI: one draggable panel per category (LMB toggles a module, RMB / ⚙ opens its settings window),
 * a top bar with search, profiles, accent palette, language, help and the HUD editor. Everything is laid out in
 * virtual coordinates and drawn scaled by a fit factor so it fits small scaled screens.
 */
public class ClickGuiScreen extends Screen {
    /** Panel positions / folded state kept for the session: {x, y, folded, moved by the user}. */
    private static final Map<Category, int[]> PANEL_STATE = new EnumMap<>(Category.class);
    private static String lastQuery = "";

    private static final int BAR_H = 22;
    private static final int PANEL_GAP = 10;
    /** Below this fit scale the panels wrap into two rows instead of shrinking further (text stays readable). */
    private static final float MIN_ONE_ROW_FIT = 0.7f;

    private final Screen parent;
    private List<CategoryPanel> panels = new ArrayList<>();
    private final SearchField search = new SearchField();
    private final Tooltip tooltip = new Tooltip();
    private final Anim.Toggle open = new Anim.Toggle(0.22f, false);
    private final PalettePopover palette = new PalettePopover();
    private final ProfilePopover profiles = new ProfilePopover(this::rebuild);
    private final WelcomeOverlay welcome = new WelcomeOverlay();
    private SettingsWindow window;
    private final BarButton hudBtn, helpBtn, langBtn, paletteBtn, profileBtn;
    private final List<BarButton> barButtons = new ArrayList<>();
    private boolean closing;
    private boolean swallowChar;
    private float time;
    private int barX, barY, barW;
    private float openScale = 1;
    /** UI fit scale: the whole GUI is laid out in virtual coordinates (vw x vh) and drawn scaled by this factor. */
    private float fit = 1;
    private int vw, vh;
    private boolean twoRows;

    public ClickGuiScreen() {
        this(null, null);
    }

    /**
     * @param parent screen to return to on close (may be null)
     * @param focus  module whose settings window opens right away (may be null)
     */
    public ClickGuiScreen(Screen parent, Module focus) {
        super(Text.literal("Vetmos"));
        this.parent = parent;
        search.setText(focus == null ? lastQuery : "");
        buildPanels();

        hudBtn = new BarButton(() -> vw < 520 ? "HUD" : Lang.ui("hud-editor", "Редактор HUD"),
            () -> Lang.ui("hud-editor-desc", "Перетаскивайте элементы HUD мышью, меняйте их размер колёсиком."), this::openHudEditor);
        helpBtn = new BarButton(() -> "?", () -> Lang.ui("help-desc", "Показать краткую справку."), () -> welcome.show());
        langBtn = new BarButton(() -> Lang.isEnglish() ? "EN" : "RU", () -> Lang.ui("lang-desc", "Язык интерфейса: русский / English."), this::toggleLanguage);
        paletteBtn = new BarButton(() -> "", () -> Lang.ui("palette-desc", "Акцентный цвет, градиент и язык."), () -> togglePopover(palette));
        profileBtn = new BarButton(() -> "☰ " + ConfigManager.currentProfile() + " ▾", () -> Lang.ui("profiles-desc", "Профили настроек: переключение, создание, удаление, обмен кодом."), () -> togglePopover(profiles));
        paletteBtn.active = palette::isOpen;
        profileBtn.active = profiles::isOpen;
        barButtons.addAll(List.of(profileBtn, paletteBtn, langBtn, helpBtn, hudBtn));

        if (focus != null) {
            CategoryPanel targetPanel = null;
            ModuleEntry targetEntry = null;
            for (CategoryPanel p : panels) {
                for (ModuleEntry e : p.entries()) {
                    if (e.module == focus) {
                        targetPanel = p;
                        targetEntry = e;
                    }
                }
            }
            if (targetPanel != null) {
                targetPanel.focus(targetEntry);
                panels.remove(targetPanel);
                panels.add(targetPanel);
            }
            window = new SettingsWindow(focus, false);
        }

        if (!ConfigManager.isWelcomeShown()) {
            welcome.show();
            GuiHooks.markShown();
        }
    }

    private void buildPanels() {
        panels = new ArrayList<>();
        for (Category c : Category.values()) {
            int[] st = PANEL_STATE.get(c);
            CategoryPanel p = new CategoryPanel(c, st != null ? st[0] : 0, st != null ? st[1] : 38, st != null && st[2] == 1, this::openSettings);
            p.moved = st != null && st.length > 3 && st[3] == 1;
            panels.add(p);
        }
    }

    /** After a profile switch / code import: rebuild module rows and the open settings window. */
    private void rebuild() {
        saveState();
        List<Category> order = new ArrayList<>();
        for (CategoryPanel p : panels) order.add(p.category);
        buildPanels();
        panels.sort(Comparator.comparingInt(p -> order.indexOf(p.category)));
        if (width > 0) layoutPanels();
        if (window != null && !window.isClosing()) {
            Module m = ModuleManager.get(window.module.name);
            window = m != null ? new SettingsWindow(m, true) : null;
        }
    }

    private void openSettings(Module m) {
        if (window != null && window.module == m && !window.isClosing()) {
            window.close();
            return;
        }
        window = new SettingsWindow(m, window != null && !window.isClosing());
        search.setFocused(false);
        palette.hide();
        profiles.hide();
    }

    private void toggleLanguage() {
        InterfaceModule ui = ModuleManager.get(InterfaceModule.class);
        if (ui != null) ui.language.set(Lang.isEnglish() ? InterfaceModule.Language.RU : InterfaceModule.Language.EN);
    }

    private void togglePopover(Popover p) {
        BarButton anchor = p == palette ? paletteBtn : profileBtn;
        Popover other = p == palette ? profiles : palette;
        other.hide();
        if (p.isOpen()) p.hide();
        else p.show(anchor.x + anchor.w - p.w, barY + BAR_H + 4);
    }

    @Override
    protected void init() {
        computeFit();
        layoutPanels();
        if (!closing) open.set(true);
    }

    private static int rowWidth(int panelsInRow) {
        return panelsInRow * CategoryPanel.WIDTH + (panelsInRow - 1) * PANEL_GAP;
    }

    /**
     * Picks the UI scale: one row of panels shrunk to fit the screen, or, if that would get too small
     * (below MIN_ONE_ROW_FIT), two rows of panels with a milder shrink.
     */
    private void computeFit() {
        int n = Category.values().length;
        float oneRow = Math.min(1f, Math.min((width - 8f) / rowWidth(n), height / 220f));
        if (oneRow >= MIN_ONE_ROW_FIT || n <= 2) {
            twoRows = false;
            fit = oneRow;
        } else {
            twoRows = true;
            int perRow = (n + 1) / 2;
            fit = Math.min(1f, Math.min((width - 8f) / rowWidth(perRow), height / 300f));
        }
        fit = Math.max(0.3f, fit);
        vw = Math.round(width / fit);
        vh = Math.round(height / fit);
    }

    /** Default positions for every panel the user hasn't dragged. */
    private void layoutPanels() {
        List<CategoryPanel> ordered = new ArrayList<>(panels);
        ordered.sort(Comparator.comparingInt(p -> p.category.ordinal()));
        int n = ordered.size();
        int perRow = twoRows ? (n + 1) / 2 : n;
        int rowY0 = 38;
        int rowY1 = rowY0 + Math.max(60, (vh - rowY0 - 22) / 2) + 4;
        for (int i = 0; i < n; i++) {
            CategoryPanel p = ordered.get(i);
            if (p.moved) continue;
            int row = i / perRow, col = i % perRow;
            int inRow = Math.min(perRow, n - row * perRow);
            int startX = Math.max(4, (vw - rowWidth(inRow)) / 2);
            p.x = startX + col * (CategoryPanel.WIDTH + PANEL_GAP);
            p.y = row == 0 ? rowY0 : rowY1;
        }
    }

    /** Lowest y the panel body may reach: just above the nearest panel placed below it that overlaps horizontally. */
    private int bottomLimit(CategoryPanel p) {
        int limit = Integer.MAX_VALUE;
        for (CategoryPanel o : panels) {
            if (o == p || o.y <= p.y + CategoryPanel.HEADER_H) continue;
            boolean overlapX = o.x < p.x + CategoryPanel.WIDTH && p.x < o.x + CategoryPanel.WIDTH;
            if (overlapX) limit = Math.min(limit, o.y - 6);
        }
        return limit;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = closing ? Anim.easeInOutCubic(open.raw()) : Anim.easeOutCubic(open.raw());
        if (client != null && client.world == null) {
            ctx.fillGradient(0, 0, width, height, 0xFF0C0C0C, 0xFF161616);
        } else {
            ctx.fillGradient(0, 0, width, height, Anim.alpha(0x50000000, a), Anim.alpha(0x80000000, a));
        }
        ctx.fillGradient(0, height - height / 3, width, height, 0x00000000, Anim.alpha(0x30000000, a));
    }

    /** Screen -> virtual GUI coordinates (undo the fit scale and the open-animation scale). */
    private double tx(double mx) {
        return vw / 2.0 + (mx - width / 2.0) / (openScale * fit);
    }

    private double ty(double my) {
        return vh / 2.0 + (my - height / 2.0) / (openScale * fit);
    }

    private Popover openPopover() {
        if (palette.isOpen()) return palette;
        if (profiles.isOpen()) return profiles;
        return null;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Anim.frame();
        open.update();
        if (closing && open.raw() <= 0) {
            finishClose();
            return;
        }
        if (window != null && window.isDead()) window = null;
        float e = closing ? Anim.easeInOutCubic(open.raw()) : Anim.easeOutCubic(open.raw());
        openScale = 0.94f + 0.06f * e;
        float oldAlpha = Paint.alpha;
        Paint.alpha = e;

        int mx = (int) Math.round(tx(mouseX)), my = (int) Math.round(ty(mouseY));
        if (closing || welcome.isOpen()) mx = my = Widget.NO_MOUSE;

        // who gets the mouse: popover > settings window > top bar / panels
        Popover pop = openPopover();
        boolean overPop = pop != null && pop.isOver(mx, my);
        boolean overWindow = !overPop && window != null && window.isOver(mx, my);
        int baseX = overPop || overWindow ? Widget.NO_MOUSE : mx, baseY = overPop || overWindow ? Widget.NO_MOUSE : my;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        // scissor rectangles are transformed by the matrix too (DrawContext.enableScissor), so clipping follows the scale
        m.translate(width / 2f, height / 2f);
        m.scale(openScale * fit, openScale * fit);
        m.translate(-vw / 2f, -vh / 2f + (1 - e) * 6);

        String query = search.text();
        CategoryPanel top = null;
        for (int i = panels.size() - 1; i >= 0; i--) {
            CategoryPanel p = panels.get(i);
            if (p.isDragging() || p.isOver(baseX, baseY)) {
                top = p;
                break;
            }
        }
        for (CategoryPanel p : panels) {
            boolean active = p == top;
            p.render(ctx, active ? baseX : Widget.NO_MOUSE, active ? baseY : Widget.NO_MOUSE, tooltip, query, vw, vh, bottomLimit(p));
        }

        renderTopBar(ctx, baseX, baseY);
        renderHint(ctx);

        // modal dim layer over the ClickGUI behind the settings window
        if (window != null) {
            float d = window.openAmount();
            if (d > 0.01f) Paint.rect(ctx, -2000, -2000, vw + 4000, vh + 4000, Anim.withAlpha(0xFF000000, Math.round(0x99 * d)));
        }
        if (window != null) window.render(ctx, overWindow ? mx : (overPop ? Widget.NO_MOUSE : mx), overPop ? Widget.NO_MOUSE : my, tooltip, vw, vh);
        palette.render(ctx, overPop && pop == palette ? mx : Widget.NO_MOUSE, overPop && pop == palette ? my : Widget.NO_MOUSE, tooltip, vw, vh);
        profiles.render(ctx, overPop && pop == profiles ? mx : Widget.NO_MOUSE, overPop && pop == profiles ? my : Widget.NO_MOUSE, tooltip, vw, vh);
        Toasts.render(ctx, vw / 2, vh - 16);
        welcome.render(ctx, (int) Math.round(tx(mouseX)), (int) Math.round(ty(mouseY)), vw, vh);

        m.popMatrix();

        ctx.createNewRootLayer();
        if (!closing && !welcome.isVisible()) tooltip.render(ctx, mouseX, mouseY, width, height);
        else tooltip.render(ctx, Widget.NO_MOUSE, Widget.NO_MOUSE, width, height); // reset hover state
        Paint.alpha = oldAlpha;
    }

    private void renderTopBar(DrawContext ctx, int mx, int my) {
        barW = Math.min(vw - 16, 620);
        barX = (vw - barW) / 2;
        barY = 6;
        time += Anim.dt;

        Paint.panel(ctx, barX, barY, barW, BAR_H);
        SettingsWindow.accentLine(ctx, barX + 6, barY + BAR_H - 3, barW - 12);

        // moon + gradient title
        int moonX = barX + 13, moonY = barY + BAR_H / 2;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        Paint.circle(ctx, moonX, moonY, 5, Theme.accentAt(0) | 0xFF000000);
        Paint.circle(ctx, moonX + 2, moonY - 1, 4, 0xFF131313);
        String name = "Vetmos";
        int tx = barX + 23;
        for (int i = 0; i < name.length(); i++) {
            Text ch = Text.literal(String.valueOf(name.charAt(i))).formatted(Formatting.BOLD);
            int c = Theme.accentAt(i / (float) name.length()) | 0xFF000000;
            Paint.text(ctx, ch, tx, barY + 7, Anim.mix(c, 0xFFFFFFFF, pulse * 0.15f));
            tx += Paint.width(ch);
        }
        int titleEnd = tx + 10;

        // right-side buttons, laid out right to left
        int bx = barX + barW - 4;
        for (int i = barButtons.size() - 1; i >= 0; i--) {
            BarButton b = barButtons.get(i);
            String label = b.label.get();
            if (b == paletteBtn) b.w = 18;
            else if (b == profileBtn) b.w = Math.min(100, Paint.width(label) + 12);
            else b.w = Math.max(18, Paint.width(label) + 12);
            bx -= b.w;
            b.x = bx;
            b.y = barY + 3;
            b.h = BAR_H - 6;
            bx -= 3;
        }
        for (BarButton b : barButtons) b.render(ctx, mx, my);
        // palette swatch inside its button
        int pc = paletteBtn.x + paletteBtn.w / 2, pcy = paletteBtn.y + paletteBtn.h / 2;
        Paint.circle(ctx, pc, pcy, 4, Theme.accentAt(0.2f) | 0xFF000000);
        Paint.circle(ctx, pc + 1, pcy + 1, 2, Theme.accentAt(0.7f) | 0xFF000000);
        Paint.ring(ctx, pc, pcy, 4, 0x60FFFFFF);

        // search between the title and the buttons
        int right = profileBtn.x - 6;
        search.w = Math.max(50, Math.min(160, right - titleEnd));
        search.h = BAR_H - 6;
        search.x = Math.max(titleEnd, Math.min(right - search.w, barX + (barW - search.w) / 2));
        search.y = barY + 3;
        search.render(ctx, mx, my);

        for (BarButton b : barButtons) if (b.hovered) tooltip.offer(b, b.tooltip.get());
    }

    private void renderHint(DrawContext ctx) {
        String hint = Lang.ui("gui-hint", "ЛКМ — вкл/выкл  •  ПКМ или ⚙ — настройки  •  колёсико — прокрутка  •  Esc — закрыть");
        float sc = 0.75f;
        int w = Math.round(Paint.width(hint) * sc);
        Paint.textScaled(ctx, hint, (vw - w) / 2f, vh - 10, sc, 0xB0909090);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        if (KeybindWidget.listening != null) return KeybindWidget.listening.handleMouse(button);
        double mx = tx(mouseX), my = ty(mouseY);
        if (welcome.mouseClicked(mx, my, button)) return true;

        Popover pop = openPopover();
        if (pop != null) {
            if (pop.mouseClicked(mx, my, button)) return true;
            pop.hide();
            BarButton anchor = pop == palette ? paletteBtn : profileBtn;
            if (Paint.inside(mx, my, anchor.x, anchor.y, anchor.w, anchor.h)) return true; // the click closed it
        }
        boolean inWindow = window != null && window.isOver(mx, my);
        if (window != null && !inWindow) window.blurInputs();
        for (BarButton b : barButtons) if (b.click(mx, my, button)) return true;
        if (search.mouseClicked(mx, my, button)) {
            if (window != null) window.search.setFocused(false);
            return true;
        }

        if (window != null && window.mouseClicked(mx, my, button)) return true;
        if (window != null) window.search.setFocused(false);

        for (int i = panels.size() - 1; i >= 0; i--) {
            CategoryPanel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                if (panels.contains(p)) {
                    panels.remove(p);
                    panels.add(p);
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = tx(mouseX), my = ty(mouseY);
        for (CategoryPanel p : panels) p.mouseReleased(mx, my, button);
        if (window != null) window.mouseReleased(mx, my, button);
        palette.mouseReleased(mx, my, button);
        profiles.mouseReleased(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (closing || welcome.isOpen()) return true;
        double mx = tx(mouseX), my = ty(mouseY);
        for (CategoryPanel p : panels) p.mouseDragged(mx, my, button);
        if (window != null) window.mouseDragged(mx, my, button, vw, vh);
        palette.mouseDragged(mx, my, button);
        profiles.mouseDragged(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing || welcome.isOpen()) return true;
        double mx = tx(mouseX), my = ty(mouseY);
        Popover pop = openPopover();
        if (pop != null && pop.mouseScrolled(mx, my, verticalAmount)) return true;
        if (window != null && window.mouseScrolled(mx, my, verticalAmount)) return true;
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mx, my, verticalAmount)) return true;
        }
        return true;
    }

    /** The text field that receives typing right now, or null. */
    private TextInput typingTarget() {
        if (window != null && !window.isClosing() && window.focusedInput() != null) return window.focusedInput();
        Popover pop = openPopover();
        if (pop != null && pop.focusedField() != null) return pop.focusedField();
        if (window != null && !window.isClosing()) {
            if (window.search.isFocused() || (!search.isFocused() && !window.onMacroTab())) return window.search;
        }
        return search;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        swallowChar = false;
        if (closing) return true;
        if (KeybindWidget.listening != null) {
            swallowChar = true;
            return KeybindWidget.listening.handleKey(keyCode);
        }
        if (welcome.isOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) welcome.finish();
            swallowChar = true;
            return true;
        }
        Popover pop = openPopover();
        TextInput target = typingTarget();

        // a macro field is focused: it takes every key; Esc only unfocuses it (no module binds, no closing)
        if (target instanceof TextField tf && tf.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) tf.setFocused(false);
            else tf.keyPressed(keyCode);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (pop != null) {
                pop.hide();
                return true;
            }
            if (target.isFocused() && !target.text().isEmpty()) {
                target.setText("");
                target.setFocused(false);
                return true;
            }
            if (window != null && !window.isClosing()) {
                window.close();
                return true;
            }
            close();
            return true;
        }
        // copy / paste the settings of the open window
        boolean windowOpen = window != null && !window.isClosing();
        if (windowOpen && pop == null && !window.search.isFocused() && !search.isFocused()) {
            if (Screen.isCopy(keyCode)) {
                window.copySettings();
                swallowChar = true;
                return true;
            }
            if (Screen.isPaste(keyCode)) {
                window.pasteSettings();
                swallowChar = true;
                return true;
            }
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            target.setFocused(true);
            swallowChar = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !target.text().isEmpty()) target.setFocused(true);
        if (target.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        if (closing || KeybindWidget.listening != null || welcome.isOpen()) return true;
        TextInput target = typingTarget();
        if (target instanceof TextField) return target.charTyped(chr);
        if (chr == ' ' && target.text().isEmpty()) return true;
        Popover pop = openPopover();
        if (pop != null && pop.focusedField() == null) return true; // don't type into hidden fields under a popover
        return target.charTyped(chr);
    }

    private void openHudEditor() {
        saveState();
        if (client == null) return;
        if (parent instanceof HudEditorScreen) client.setScreen(parent);
        else client.setScreen(new HudEditorScreen(this));
    }

    @Override
    public void close() {
        if (closing) return;
        KeybindWidget.listening = null;
        saveState();
        closing = true;
        open.set(false);
    }

    private void finishClose() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void removed() {
        KeybindWidget.listening = null;
        closing = false;
        open.snap(true);
        saveState();
    }

    private void saveState() {
        for (CategoryPanel p : panels) PANEL_STATE.put(p.category, new int[] { p.x, p.y, p.isFolded() ? 1 : 0, p.moved ? 1 : 0 });
        lastQuery = search.text();
    }

    /** A top-bar button. */
    private static final class BarButton {
        final Supplier<String> label, tooltip;
        final Runnable action;
        java.util.function.BooleanSupplier active = () -> false;
        int x, y, w, h;
        float hover, flash;
        boolean hovered;

        BarButton(Supplier<String> label, Supplier<String> tooltip, Runnable action) {
            this.label = label;
            this.tooltip = tooltip;
            this.action = action;
        }

        void render(DrawContext ctx, int mx, int my) {
            hovered = Paint.inside(mx, my, x, y, w, h);
            hover = Anim.approach(hover, hovered || active.getAsBoolean() ? 1 : 0, 16);
            flash = Anim.approach(flash, 0, 6);
            ButtonWidget.draw(ctx, x, y, w, h, label.get(), hover, flash);
            if (active.getAsBoolean()) Paint.rect(ctx, x, y + h - 2, w, 2, Palette.accent());
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
