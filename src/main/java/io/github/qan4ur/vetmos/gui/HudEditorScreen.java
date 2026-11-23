package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * HUD editor: shows every enabled HUD element (in preview mode) with an outline. LMB drags (with snapping to the
 * screen center/edges and to other elements, Shift disables it), the wheel changes the size, RMB opens the
 * module's settings in the ClickGUI. A side panel lists all HUD modules so they can be switched on and off.
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private static final int SIDE_W = 118, SIDE_HEADER = 18, SIDE_ROW = 14;
    private static int sideX = Integer.MIN_VALUE, sideY;
    private static boolean sideFolded;

    private final Screen parent;
    private final Tooltip tooltip = new Tooltip();
    private final Map<Module, float[]> anims = new HashMap<>(); // {hover, drag}
    private final Anim.Toggle open = new Anim.Toggle(0.2f, false);
    private final Anim.Toggle sideOpen = new Anim.Toggle(0.22f, !sideFolded);

    private Module dragging;
    private double dragDX, dragDY;
    private Double guideX, guideY;
    private Module scaleShown;
    private float scaleFade;

    private boolean sideDragging;
    private double sideDX, sideDY;
    private final Map<Module, float[]> sideAnims = new HashMap<>(); // {hover, on}
    private float resetHover, resetFlash, sideHeaderHover, sideFade = 1;

    public HudEditorScreen(Screen parent) {
        super(Text.literal(Lang.ui("hud-editor", "Редактор HUD")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HudManager.setEditorOpen(true);
        open.set(true);
        if (sideX == Integer.MIN_VALUE) {
            // first open: on small scaled screens (e.g. 480x270) start folded so the list doesn't hide HUD elements
            sideFolded = height < 360 || width < 560;
            sideOpen.snap(!sideFolded);
            sideX = 6;
            sideY = Math.max(32, height / 2 - 60);
        }
    }

    @Override
    public void removed() {
        HudManager.setEditorOpen(false);
        dragging = null;
        sideDragging = false;
    }

    @Override
    public void close() {
        HudManager.setEditorOpen(false);
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = Anim.easeOutCubic(open.raw());
        if (client != null && client.world == null) ctx.fillGradient(0, 0, width, height, 0xFF0C0C0C, 0xFF161616);
        else ctx.fill(0, 0, width, height, Anim.alpha(0x30000000, a));

        // faint alignment grid and center lines
        int grid = Anim.alpha(0x10FFFFFF, a);
        for (int x = 0; x < width; x += 20) ctx.fill(x, 0, x + 1, height, grid);
        for (int y = 0; y < height; y += 20) ctx.fill(0, y, width, y + 1, grid);
        int center = Anim.alpha(Palette.accentA(48), a);
        ctx.fill(width / 2, 0, width / 2 + 1, height, center);
        ctx.fill(0, height / 2, width, height / 2 + 1, center);
    }

    private List<Module> enabledHud() {
        List<Module> list = new ArrayList<>();
        for (Module m : HudManager.hudModules()) if (m.isEnabled()) list.add(m);
        return list;
    }

    private Module elementAt(double mx, double my) {
        if (isOverSidebar(mx, my)) return null;
        List<Module> list = enabledHud();
        for (int i = list.size() - 1; i >= 0; i--) {
            double[] b = HudManager.getBounds(list.get(i));
            if (Paint.inside(mx, my, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2)) return list.get(i);
        }
        return null;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Anim.frame();
        open.update();
        sideOpen.update();
        if (client == null) return;

        // The in-game HUD (and with it the Vetmos HUD layer) is drawn behind screens, except with F1 or without a world.
        if (client.world == null || client.options.hudHidden) HudManager.render(ctx, client.getRenderTickCounter());

        float oldAlpha = Paint.alpha;
        Paint.alpha = Anim.easeOutCubic(open.raw());

        Module hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
        for (Module m : enabledHud()) {
            float[] an = anims.computeIfAbsent(m, k -> new float[2]);
            an[0] = Anim.approach(an[0], m == hovered ? 1 : 0, 18);
            an[1] = Anim.approach(an[1], m == dragging ? 1 : 0, 18);
            drawElementFrame(ctx, m, an[0], an[1]);
        }

        // snapping guides
        if (dragging != null) {
            if (guideX != null) Paint.rect(ctx, (int) Math.round(guideX), 0, 1, height, Palette.accentA(180));
            if (guideY != null) Paint.rect(ctx, 0, (int) Math.round(guideY), width, 1, Palette.accentA(180));
        }

        // scale popup
        scaleFade = Anim.approach(scaleFade, 0, 2.5f);
        if (scaleShown != null && scaleShown.isEnabled() && scaleFade > 0.01f) {
            double[] b = HudManager.getBounds(scaleShown);
            String s = String.format(Locale.ROOT, "×%.2f", ((HudModule) scaleShown).hudScale().get());
            int w = Paint.width(s) + 8;
            int x = (int) Math.round(b[0] + b[2] / 2 - w / 2.0), y = (int) Math.round(b[1] + b[3] / 2 - 6);
            float old = Paint.alpha;
            Paint.alpha = old * Anim.clamp01(scaleFade * 2);
            Paint.tooltipBox(ctx, x, y, w, 12);
            Paint.textCentered(ctx, s, x + w / 2, y + 2, Palette.TEXT);
            Paint.alpha = old;
        }

        renderHint(ctx);
        sideFade = Anim.approach(sideFade, dragging != null ? 0.25f : 1f, 12);
        float beforeSide = Paint.alpha;
        Paint.alpha = beforeSide * sideFade;
        renderSidebar(ctx, dragging != null ? Widget.NO_MOUSE : mouseX, dragging != null ? Widget.NO_MOUSE : mouseY);
        Paint.alpha = beforeSide;

        if (hovered != null && dragging == null && !isOverSidebar(mouseX, mouseY)) {
            tooltip.offer(hovered, hovered.displayTitle(), hovered.displayDescription());
        }
        ctx.createNewRootLayer();
        tooltip.render(ctx, mouseX, mouseY, width, height);
        Paint.alpha = oldAlpha;
    }

    private void drawElementFrame(DrawContext ctx, Module m, float hover, float drag) {
        double[] b = HudManager.getBounds(m);
        int x = (int) Math.floor(b[0]) - 1, y = (int) Math.floor(b[1]) - 1;
        int w = (int) Math.ceil(b[0] + b[2]) + 1 - x, h = (int) Math.ceil(b[1] + b[3]) + 1 - y;
        float f = Math.max(hover, drag);

        Paint.rect(ctx, x, y, w, h, Palette.accentA(Math.round(14 + 26 * f)));
        Paint.outline(ctx, x, y, w, h, 0, Anim.mix(Palette.accentA(150), Palette.accentBright(), f));
        // corner ticks
        int c = Anim.mix(Palette.accent(), 0xFFFFFFFF, f * 0.7f);
        int t = Math.min(4, Math.min(w, h) / 2);
        Paint.rect(ctx, x - 1, y - 1, t + 1, 1, c);
        Paint.rect(ctx, x - 1, y - 1, 1, t + 1, c);
        Paint.rect(ctx, x + w - t, y - 1, t + 1, 1, c);
        Paint.rect(ctx, x + w, y - 1, 1, t + 1, c);
        Paint.rect(ctx, x - 1, y + h, t + 1, 1, c);
        Paint.rect(ctx, x - 1, y + h - t, 1, t + 1, c);
        Paint.rect(ctx, x + w - t, y + h, t + 1, 1, c);
        Paint.rect(ctx, x + w, y + h - t, 1, t + 1, c);

        // title label
        float sc = 0.75f;
        String label = m.displayTitle();
        int lw = Math.round(Paint.width(label) * sc) + 6, lh = 9;
        int lx = Math.max(1, Math.min(width - lw - 1, x));
        int ly = y - lh - 1;
        if (ly < 1) ly = y + h + 1;
        Paint.rect(ctx, lx, ly, lw, lh, Anim.mix(0xB0101012, 0xD0303034, f));
        Paint.rect(ctx, lx, ly + lh - 1, lw, 1, Anim.mix(0xFF9A9A9A, Palette.accent(), f));
        Paint.textScaled(ctx, label, lx + 3, ly + 2, sc, Palette.TEXT);
    }

    private void renderHint(DrawContext ctx) {
        String hint = Lang.ui("hud-hint", "ЛКМ — двигать, колёсико — размер, ПКМ — настройки, Esc — выход");
        String sub = Lang.ui("hud-hint-shift", "Shift — без привязки к краям");
        int w = Paint.width(hint) + 16;
        int x = (width - w) / 2, y = 5;
        Paint.panel(ctx, x, y, w, 24, Paint.HUD_EDITOR_BG);
        Paint.textCentered(ctx, hint, width / 2, y + 4, Palette.TEXT);
        float sc = 0.75f;
        Paint.textScaled(ctx, sub, width / 2f - Paint.width(sub) * sc / 2, y + 14, sc, Palette.TEXT_MUTED);
    }

    // ---------------------------------------------------------------- sidebar

    private int sideBodyHeight() {
        int full = HudManager.hudModules().size() * SIDE_ROW + 6 + 18;
        return Math.round(full * sideOpen.eased());
    }

    private boolean isOverSidebar(double mx, double my) {
        if (dragging != null) return false; // while moving an element the sidebar is see-through and ignores the mouse
        return Paint.inside(mx, my, sideX, sideY, SIDE_W, SIDE_HEADER + sideBodyHeight());
    }

    private void renderSidebar(DrawContext ctx, int mx, int my) {
        sideX = Math.max(2, Math.min(width - SIDE_W - 2, sideX));
        sideY = Math.max(32, Math.min(height - SIDE_HEADER - sideBodyHeight() - 2, sideY));
        int x = sideX, y = sideY;
        int bodyH = sideBodyHeight();
        int total = SIDE_HEADER + bodyH;
        boolean hh = Paint.inside(mx, my, x, y, SIDE_W, SIDE_HEADER);
        sideHeaderHover = Anim.approach(sideHeaderHover, hh || sideDragging ? 1 : 0, 16);

        Paint.panel(ctx, x, y, SIDE_W, total, Paint.HUD_EDITOR_BG);
        if (sideHeaderHover > 0.01f) Paint.rect(ctx, x + 2, y + 2, SIDE_W - 4, SIDE_HEADER - 3, Anim.withAlpha(0xFFFFFFFF, Math.round(10 * sideHeaderHover)));
        if (bodyH > 0) Paint.rect(ctx, x + 4, y + SIDE_HEADER - 1, SIDE_W - 8, 1, 0x30FFFFFF);
        Paint.text(ctx, Lang.ui("hud-elements", "Элементы HUD"), x + 6, y + 5, Anim.mix(0xFF8A8A8A, 0xFFBDBDBD, sideHeaderHover));
        if (hh) tooltip.offer("side-header", Lang.ui("hud-elements", "Элементы HUD"), Lang.ui("hud-elements-desc", "Нажмите на элемент, чтобы включить или выключить его. ЛКМ по заголовку — перетащить, ПКМ — свернуть."));

        if (bodyH <= 0) return;
        ctx.enableScissor(x, y + SIDE_HEADER, x + SIDE_W, y + total);
        int ry = y + SIDE_HEADER + 3;
        for (Module m : HudManager.hudModules()) {
            float[] an = sideAnims.computeIfAbsent(m, k -> new float[] { 0, k.isEnabled() ? 1 : 0 });
            boolean hov = Paint.inside(mx, my, x + 2, ry, SIDE_W - 4, SIDE_ROW) && my < y + total;
            an[0] = Anim.approach(an[0], hov ? 1 : 0, 18);
            an[1] = Anim.approach(an[1], m.isEnabled() ? 1 : 0, 14);
            Paint.slot(ctx, x + 3, ry, SIDE_W - 6, SIDE_ROW - 1, an[0], an[1] * 0.55f);
            int barH = Math.round((SIDE_ROW - 3) * an[1]);
            if (barH > 0) Paint.rect(ctx, x + 4, ry + (SIDE_ROW - 1 - barH) / 2, 2, barH, io.github.qan4ur.vetmos.util.Theme.accentAt(ry % 300 / 300f) | 0xFF000000);
            Paint.text(ctx, Paint.trim(m.displayTitle(), SIDE_W - 22), x + 15, ry + 3, Anim.mix(Palette.TEXT_MUTED, Palette.TEXT, Math.max(an[1], an[0] * 0.6f)));
            if (hov) tooltip.offer("side:" + m.name, m.displayTitle(), m.displayDescription() + " " + Lang.ui("hud-side-row", "(ЛКМ — вкл/выкл, ПКМ — настройки)"));
            ry += SIDE_ROW;
        }
        // reset-all button
        int bx = x + 4, by = ry + 3, bw = SIDE_W - 8, bh = 14;
        boolean rh = Paint.inside(mx, my, bx, by, bw, bh) && my < y + total;
        resetHover = Anim.approach(resetHover, rh ? 1 : 0, 16);
        resetFlash = Anim.approach(resetFlash, 0, 6);
        ButtonWidget.draw(ctx, bx, by, bw, bh, Lang.ui("reset-positions", "Сбросить позиции"), resetHover, resetFlash);
        if (rh) tooltip.offer("reset-all", Lang.ui("reset-positions-desc", "Вернуть все элементы HUD на места по умолчанию."));
        ctx.disableScissor();
    }

    private boolean sidebarClicked(double mx, double my, int button) {
        if (!isOverSidebar(mx, my)) return false;
        int x = sideX, y = sideY;
        if (Paint.inside(mx, my, x, y, SIDE_W, SIDE_HEADER)) {
            if (button == 0) {
                sideDragging = true;
                sideDX = mx - x;
                sideDY = my - y;
            } else if (button == 1) {
                sideFolded = !sideFolded;
                sideOpen.set(!sideFolded);
                Widget.click(sideFolded ? 0.8f : 1.1f);
            }
            return true;
        }
        if (!sideOpen.on()) return true;
        int ry = y + SIDE_HEADER + 3;
        for (Module m : HudManager.hudModules()) {
            if (Paint.inside(mx, my, x + 2, ry, SIDE_W - 4, SIDE_ROW)) {
                if (button == 0) {
                    m.toggle();
                    Widget.click(m.isEnabled() ? 1.25f : 0.9f);
                } else if (button == 1) openSettings(m);
                return true;
            }
            ry += SIDE_ROW;
        }
        if (button == 0 && Paint.inside(mx, my, x + 4, ry + 3, SIDE_W - 8, 14)) {
            HudManager.resetAllPositions();
            resetFlash = 1;
            Widget.click(1.1f);
        }
        return true;
    }

    // ---------------------------------------------------------------- input

    private void openSettings(Module m) {
        if (client == null) return;
        Widget.click(1.1f);
        client.setScreen(new ClickGuiScreen(this, m));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (sidebarClicked(mx, my, button)) return true;
        Module m = elementAt(mx, my);
        if (m == null) return super.mouseClicked(mx, my, button);
        if (button == 0) {
            double[] b = HudManager.getBounds(m);
            dragging = m;
            dragDX = mx - b[0];
            dragDY = my - b[1];
            return true;
        }
        if (button == 1) {
            openSettings(m);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (sideDragging) {
            sideX = (int) Math.round(mx - sideDX);
            sideY = (int) Math.round(my - sideDY);
            return true;
        }
        if (dragging == null || !dragging.isEnabled()) return true;
        double[] b = HudManager.getBounds(dragging);
        double w = b[2], h = b[3];
        double left = mx - dragDX, top = my - dragDY;
        guideX = null;
        guideY = null;
        if (!Screen.hasShiftDown()) {
            double[] sx = snap(left, w, true);
            left = sx[0];
            if (!Double.isNaN(sx[1])) guideX = sx[1];
            double[] sy = snap(top, h, false);
            top = sy[0];
            if (!Double.isNaN(sy[1])) guideY = sy[1];
        }
        move(dragging, left, top, w, h);
        return true;
    }

    /** Snaps the box edge/center to screen edges, the screen center and other elements. Returns {pos, guide or NaN}. */
    private double[] snap(double pos, double size, boolean horizontal) {
        double screen = horizontal ? width : height;
        List<Double> targets = new ArrayList<>(List.of(0.0, screen / 2, screen));
        for (Module o : enabledHud()) {
            if (o == dragging) continue;
            double[] ob = HudManager.getBounds(o);
            double s = horizontal ? ob[0] : ob[1], l = horizontal ? ob[2] : ob[3];
            targets.add(s);
            targets.add(s + l);
            targets.add(s + l / 2);
        }
        double best = SNAP + 1, result = pos, guide = Double.NaN;
        double[] offsets = { 0, size / 2, size }; // left/top edge, center, right/bottom edge
        for (double t : targets) {
            for (double off : offsets) {
                double d = Math.abs(pos + off - t);
                if (d < best) {
                    best = d;
                    result = t - off;
                    guide = t;
                }
            }
        }
        return new double[] { result, guide };
    }

    private void move(Module m, double left, double top, double w, double h) {
        left = Math.max(0, Math.min(width - w, left));
        top = Math.max(0, Math.min(height - h, top));
        HudPosition pos = HudManager.getPosition(m).copy();
        pos.setFromTopLeft(left, top, w, h, width, height);
        HudManager.setPosition(m, pos);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        sideDragging = false;
        if (dragging != null && dragging.isEnabled()) {
            double[] b = HudManager.getBounds(dragging);
            move(dragging, b[0], b[1], b[2], b[3]);
        }
        dragging = null;
        guideX = null;
        guideY = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        Module m = dragging != null ? dragging : elementAt(mx, my);
        if (m == null || verticalAmount == 0) return true;
        DoubleSetting scale = ((HudModule) m).hudScale();
        double[] before = HudManager.getBounds(m);
        double cx = before[0] + before[2] / 2, cy = before[1] + before[3] / 2;
        scale.set(scale.get() + (verticalAmount > 0 ? 0.05 : -0.05));
        double[] after = HudManager.getBounds(m);
        move(m, cx - after[2] / 2, cy - after[3] / 2, after[2], after[3]);
        if (dragging == m) {
            // keep the grab point proportional so the element doesn't jump away from the cursor
            dragDX *= after[2] / Math.max(1e-6, before[2]);
            dragDY *= after[3] / Math.max(1e-6, before[3]);
        }
        scaleShown = m;
        scaleFade = 1;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
