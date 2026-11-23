package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Positioning mode for a single HUD element, opened from its settings window ("Переместить"). Everything else is
 * dimmed; LMB drags (with the HUD editor's snapping, Shift disables it), the wheel scales, RMB / Enter / "Готово"
 * confirms and Esc restores the previous position and scale. Returns to the parent screen.
 */
public class MoveElementScreen extends Screen {
    private final Screen parent;
    private final Module module;
    private final HudModule hud;
    private final HudPosition originalPos;
    private final double originalScale;
    private final Anim.Toggle open = new Anim.Toggle(0.2f, false);
    private final Tooltip tooltip = new Tooltip();

    private boolean dragging;
    private double dragDX, dragDY;
    private Double guideX, guideY;
    private float hover, drag, doneHover, cancelHover, doneFlash, scaleFade;
    private int doneX, doneY, doneW, cancelX, cancelW, btnH = 16;

    public MoveElementScreen(Screen parent, Module module) {
        super(Text.literal(Lang.ui("move", "Переместить")));
        this.parent = parent;
        this.module = module;
        this.hud = (HudModule) module;
        this.originalPos = HudManager.getPosition(module).copy();
        this.originalScale = hud.hudScale().get();
    }

    @Override
    protected void init() {
        open.set(true);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** The real HUD layer draws this element only when it's enabled and visible. */
    private boolean drawnByHud() {
        return module.isEnabled() && hud.shouldRenderHud() && client != null && client.world != null && !client.options.hudHidden;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = Anim.easeOutCubic(open.raw());
        if (client != null && client.world == null) ctx.fillGradient(0, 0, width, height, 0xFF0C0C0C, 0xFF161616);
        else ctx.fill(0, 0, width, height, Anim.alpha(0x78000000, a)); // dims the other HUD elements
    }

    private boolean overElement(double mx, double my) {
        double[] b = HudManager.getBounds(module);
        return Paint.inside(mx, my, b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Anim.frame();
        open.update();
        float e = Anim.easeOutCubic(open.raw());
        float old = Paint.alpha;
        Paint.alpha = e;

        // the element itself, on top of the dim layer (real content if visible, otherwise its preview)
        HudLayout.renderPreview(ctx, module, !drawnByHud());

        hover = Anim.approach(hover, overElement(mx, my) ? 1 : 0, 18);
        drag = Anim.approach(drag, dragging ? 1 : 0, 18);
        HudLayout.drawFrame(ctx, module, Math.max(0.35f, hover), drag, width);

        if (dragging) {
            if (guideX != null) Paint.rect(ctx, (int) Math.round(guideX), 0, 1, height, Palette.accentA(180));
            if (guideY != null) Paint.rect(ctx, 0, (int) Math.round(guideY), width, 1, Palette.accentA(180));
        }

        // scale popup
        scaleFade = Anim.approach(scaleFade, 0, 2.5f);
        if (scaleFade > 0.01f) {
            double[] b = HudManager.getBounds(module);
            String s = String.format(java.util.Locale.ROOT, "×%.2f", hud.hudScale().get());
            int w = Paint.width(s) + 8;
            int x = (int) Math.round(b[0] + b[2] / 2 - w / 2.0), y = (int) Math.round(b[1] + b[3] / 2 - 6);
            float o2 = Paint.alpha;
            Paint.alpha = o2 * Anim.clamp01(scaleFade * 2);
            Paint.tooltipBox(ctx, x, y, w, 12);
            Paint.textCentered(ctx, s, x + w / 2, y + 2, Palette.TEXT);
            Paint.alpha = o2;
        }

        // hint bar
        String hint = Lang.ui("move-hint", "ЛКМ — двигать · колёсико — размер · Enter — готово · Esc — отмена");
        int hw = Paint.width(hint) + 16;
        int hx = (width - hw) / 2;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0, (1 - e) * -6);
        Paint.panel(ctx, hx, 5, hw, 26, Paint.HUD_EDITOR_BG);
        Paint.textCentered(ctx, hint, width / 2, 9, Palette.TEXT);
        float sc = 0.75f;
        String sub = module.displayTitle() + "  •  " + Lang.ui("hud-hint-shift", "Shift — без привязки к краям");
        Paint.textScaled(ctx, sub, width / 2f - Paint.width(sub) * sc / 2, 20, sc, Palette.TEXT_MUTED);
        m.popMatrix();

        // floating buttons
        String done = "✓ " + Lang.ui("move-done", "Готово"), cancel = Lang.ui("move-cancel", "Отмена");
        doneW = Paint.width(done) + 20;
        cancelW = Paint.width(cancel) + 16;
        int total = doneW + 4 + cancelW;
        doneX = (width - total) / 2;
        cancelX = doneX + doneW + 4;
        doneY = height - btnH - 10;
        boolean dh = Paint.inside(mx, my, doneX, doneY, doneW, btnH), ch = Paint.inside(mx, my, cancelX, doneY, cancelW, btnH);
        doneHover = Anim.approach(doneHover, dh ? 1 : 0, 18);
        cancelHover = Anim.approach(cancelHover, ch ? 1 : 0, 18);
        doneFlash = Anim.approach(doneFlash, 0, 6);
        m.pushMatrix();
        m.translate(0, (1 - e) * 6);
        Paint.button(ctx, doneX, doneY, doneW, btnH, Paint.styled(done), Math.max(doneHover, 0.3f), doneFlash, true);
        Paint.rect(ctx, doneX, doneY + btnH - 2, doneW, 2, Palette.accent());
        Paint.button(ctx, cancelX, doneY, cancelW, btnH, Paint.styled(cancel), cancelHover, 0, true);
        m.popMatrix();
        if (dh) tooltip.offer("done", Lang.ui("move-done-desc", "Сохранить положение и вернуться (Enter или ПКМ)."));
        if (ch) tooltip.offer("cancel", Lang.ui("move-cancel-desc", "Вернуть прежнее положение и размер (Esc)."));

        ctx.createNewRootLayer();
        tooltip.render(ctx, mx, my, width, height);
        Paint.alpha = old;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && Paint.inside(mx, my, doneX, doneY, doneW, btnH)) {
            doneFlash = 1;
            confirm();
            return true;
        }
        if (button == 0 && Paint.inside(mx, my, cancelX, doneY, cancelW, btnH)) {
            cancel();
            return true;
        }
        if (button == 1) {
            confirm();
            return true;
        }
        if (button == 0) {
            // grab the element; clicking elsewhere also picks it up so it jumps under the cursor's offset
            double[] b = HudManager.getBounds(module);
            if (overElement(mx, my)) {
                dragDX = mx - b[0];
                dragDY = my - b[1];
            } else {
                dragDX = b[2] / 2;
                dragDY = b[3] / 2;
                mouseDragged(mx, my, 0, 0, 0);
            }
            dragging = true;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return true;
        double[] b = HudManager.getBounds(module);
        double w = b[2], h = b[3];
        double left = mx - dragDX, top = my - dragDY;
        guideX = null;
        guideY = null;
        if (!Screen.hasShiftDown()) {
            double[] sx = HudLayout.snap(left, w, true, width, height, module);
            left = sx[0];
            if (!Double.isNaN(sx[1])) guideX = sx[1];
            double[] sy = HudLayout.snap(top, h, false, width, height, module);
            top = sy[0];
            if (!Double.isNaN(sy[1])) guideY = sy[1];
        }
        HudLayout.move(module, left, top, w, h, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        guideX = null;
        guideY = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return true;
        double[] sz = HudLayout.scaleBy(module, verticalAmount, width, height);
        if (dragging) {
            dragDX *= sz[2] / Math.max(1e-6, sz[0]);
            dragDY *= sz[3] / Math.max(1e-6, sz[1]);
        }
        scaleFade = 1;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return true;
    }

    private void confirm() {
        Widget.click(1.2f);
        Toasts.show(Lang.ui("move-saved", "Положение сохранено"));
        back();
    }

    private void cancel() {
        hud.hudScale().set(originalScale);
        HudManager.setPosition(module, originalPos.copy());
        Widget.click(0.9f);
        back();
    }

    private void back() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void close() {
        cancel();
    }
}
