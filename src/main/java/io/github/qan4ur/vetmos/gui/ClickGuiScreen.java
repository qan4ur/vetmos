package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Vetmos ClickGUI in the "blood moon" style: one draggable panel per category, modules toggle with LMB and unfold
 * their settings with RMB, a search field and a button to the HUD editor in the top bar.
 */
public class ClickGuiScreen extends Screen {
    /** Panel positions / folded state kept for the session: {x, y, folded, moved by the user}. */
    private static final Map<Category, int[]> PANEL_STATE = new EnumMap<>(Category.class);
    /** Modules whose settings were open when the GUI was closed. */
    private static final Set<String> EXPANDED = new HashSet<>();
    private static String lastQuery = "";

    private static final int BAR_H = 22;
    private static final int PANEL_GAP = 10;
    /** Below this fit scale the panels wrap into two rows instead of shrinking further (text stays readable). */
    private static final float MIN_ONE_ROW_FIT = 0.7f;

    private final Screen parent;
    private final List<CategoryPanel> panels = new ArrayList<>();
    private final SearchField search = new SearchField();
    private final Tooltip tooltip = new Tooltip();
    private final Anim.Toggle open = new Anim.Toggle(0.22f, false);
    private boolean closing;
    private boolean swallowChar;
    private float hudBtnHover, hudBtnFlash, time;
    private int barX, barY, barW, hudBtnX, hudBtnW;
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
     * @param focus  module to expand, scroll to and highlight (may be null)
     */
    public ClickGuiScreen(Screen parent, Module focus) {
        super(Text.literal("Vetmos"));
        this.parent = parent;
        search.setText(focus == null ? lastQuery : "");

        for (Category c : Category.values()) {
            int[] st = PANEL_STATE.get(c);
            CategoryPanel p = new CategoryPanel(c, st != null ? st[0] : 0, st != null ? st[1] : 38, st != null && st[2] == 1, EXPANDED);
            p.moved = st != null && st.length > 3 && st[3] == 1;
            panels.add(p);
        }

        if (focus != null) {
            CategoryPanel target = null;
            ModuleEntry entry = null;
            for (CategoryPanel p : panels) {
                for (ModuleEntry e : p.entries()) {
                    if (e.module == focus) {
                        target = p;
                        entry = e;
                    }
                }
            }
            if (target != null) {
                target.focus(entry);
                panels.remove(target);
                panels.add(target);
            }
        }
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

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Anim.frame();
        open.update();
        if (closing && open.raw() <= 0) {
            finishClose();
            return;
        }
        float e = closing ? Anim.easeInOutCubic(open.raw()) : Anim.easeOutCubic(open.raw());
        openScale = 0.94f + 0.06f * e;
        float oldAlpha = Paint.alpha;
        Paint.alpha = e;

        int mx = (int) Math.round(tx(mouseX)), my = (int) Math.round(ty(mouseY));
        if (closing) mx = my = Widget.NO_MOUSE;

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
            if (p.isDragging() || p.isOver(mx, my)) {
                top = p;
                break;
            }
        }
        for (CategoryPanel p : panels) {
            boolean active = p == top;
            p.render(ctx, active ? mx : Widget.NO_MOUSE, active ? my : Widget.NO_MOUSE, tooltip, query, vw, vh, bottomLimit(p));
        }

        renderTopBar(ctx, mx, my);
        renderHint(ctx);

        m.popMatrix();

        ctx.createNewRootLayer();
        if (!closing) tooltip.render(ctx, mouseX, mouseY, width, height);
        Paint.alpha = oldAlpha;
    }

    private void renderTopBar(DrawContext ctx, int mx, int my) {
        barW = Math.min(vw - 16, 400);
        barX = (vw - barW) / 2;
        barY = 6;

        Paint.glow(ctx, barX, barY, barW, BAR_H, 6, Palette.SHADOW, 5);
        Paint.roundGradient(ctx, barX, barY, barW, BAR_H, 6, 0xF0181818, 0xF00C0C0C);
        Paint.outline(ctx, barX, barY, barW, BAR_H, 6, Palette.OUTLINE);
        Paint.gradientH(ctx, barX + 6, barY + BAR_H - 1, barW / 2 - 6, 1, Palette.accentA(0), Palette.accentA(170));
        Paint.gradientH(ctx, barX + barW / 2, barY + BAR_H - 1, barW / 2 - 6, 1, Palette.accentA(170), Palette.accentA(0));

        // blood moon (a red disc with a dark bite) + title
        int moonX = barX + 13, moonY = barY + BAR_H / 2;
        time += Anim.dt;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        Paint.glow(ctx, moonX - 5, moonY - 5, 10, 10, 5, Palette.accentA(Math.round(70 + 60 * pulse)), 4);
        Paint.circle(ctx, moonX, moonY, 5, Palette.accent());
        Paint.circle(ctx, moonX + 2, moonY - 1, 4, 0xFF131313);
        Text title = Text.literal("Vetmos").formatted(Formatting.BOLD);
        int titleX = barX + 23;
        Paint.text(ctx, title, titleX, barY + 7, Anim.mix(Palette.accent(), Palette.accentBright(), pulse * 0.6f));

        // HUD editor button
        String hudLabel = "Редактор HUD";
        hudBtnW = Paint.width(hudLabel) + 14;
        hudBtnX = barX + barW - hudBtnW - 4;
        boolean bh = Paint.inside(mx, my, hudBtnX, barY + 3, hudBtnW, BAR_H - 6);
        hudBtnHover = Anim.approach(hudBtnHover, bh ? 1 : 0, 16);
        hudBtnFlash = Anim.approach(hudBtnFlash, 0, 6);
        ButtonWidget.draw(ctx, hudBtnX, barY + 3, hudBtnW, BAR_H - 6, hudLabel, hudBtnHover, hudBtnFlash);
        if (bh) tooltip.offer("hud-editor", "Редактор HUD", "Перетаскивайте элементы HUD мышью, меняйте их размер колёсиком.");

        // search field between the title and the button
        int titleEnd = titleX + Paint.font().getWidth(title) + 10;
        search.w = Math.max(50, Math.min(150, hudBtnX - titleEnd - 8));
        search.h = BAR_H - 6;
        search.x = Math.max(titleEnd, barX + (barW - search.w) / 2);
        if (search.x + search.w > hudBtnX - 6) search.x = hudBtnX - 6 - search.w;
        search.y = barY + 3;
        search.render(ctx, mx, my);
    }

    private void renderHint(DrawContext ctx) {
        String hint = "ЛКМ — вкл/выкл  •  ПКМ — настройки  •  колёсико — прокрутка  •  Esc — закрыть";
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

        if (button == 0 && Paint.inside(mx, my, hudBtnX, barY + 3, hudBtnW, BAR_H - 6)) {
            hudBtnFlash = 1;
            Widget.click(1.1f);
            openHudEditor();
            return true;
        }
        if (search.mouseClicked(mx, my, button)) return true;

        for (int i = panels.size() - 1; i >= 0; i--) {
            CategoryPanel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                panels.remove(i);
                panels.add(p);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = tx(mouseX), my = ty(mouseY);
        for (CategoryPanel p : panels) p.mouseReleased(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (closing) return true;
        double mx = tx(mouseX), my = ty(mouseY);
        for (CategoryPanel p : panels) p.mouseDragged(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return true;
        double mx = tx(mouseX), my = ty(mouseY);
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mx, my, verticalAmount)) return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        swallowChar = false;
        if (closing) return true;
        if (KeybindWidget.listening != null) {
            swallowChar = true;
            return KeybindWidget.listening.handleKey(keyCode);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (search.isFocused() && !search.text().isEmpty()) {
                search.setText("");
                search.setFocused(false);
                return true;
            }
            close();
            return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            search.setFocused(true);
            swallowChar = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.text().isEmpty()) search.setFocused(true);
        if (search.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        if (closing || KeybindWidget.listening != null) return true;
        if (chr == ' ' && search.text().isEmpty()) return true;
        return search.charTyped(chr);
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
        EXPANDED.clear();
        for (CategoryPanel p : panels) for (ModuleEntry e : p.entries()) if (e.isExpanded()) EXPANDED.add(e.module.name);
        lastQuery = search.text();
    }
}
