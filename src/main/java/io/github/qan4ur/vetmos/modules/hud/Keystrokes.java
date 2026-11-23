package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;

/** Shows your movement keys, mouse buttons and CPS, with a press-in animation and a glow on pressed keys. */
public class Keystrokes extends Module implements HudModule {
    private static final String G_MAIN = "Основное";
    private static final String G_COLORS = "Цвета";

    private final BoolSetting showMovement = bool(G_MAIN, "movement-keys", "Клавиши движения", "Показывает W A S D.", true);

    private final BoolSetting showMouse = bool(G_MAIN, "mouse-buttons", "Кнопки мыши", "Показывает левую и правую кнопки мыши.", true);

    private final BoolSetting showCps = bool(G_MAIN, "cps", "КПС", "Показывает клики в секунду под кнопками мыши.", true)
        .visibleWhen(showMouse::get);

    private final BoolSetting showSpace = bool(G_MAIN, "space-bar", "Пробел", "Показывает клавишу прыжка.", true);

    private final BoolSetting showSneak = bool(G_MAIN, "sneak-key", "Присед", "Показывает клавишу приседания рядом с пробелом.", false);

    private final BoolSetting boundNames = bool(G_MAIN, "bound-key-names", "Реальные клавиши",
        "Подписывать клавиши тем, на что они назначены, вместо W A S D.", false);

    private final IntSetting keySize = integer(G_MAIN, "key-size", "Размер клавиши", "Размер одной клавиши.", 22, 14, 40);

    private final IntSetting gap = integer(G_MAIN, "gap", "Зазор", "Расстояние между клавишами.", 2, 0, 8);

    private final DoubleSetting fadeSpeed = decimal(G_MAIN, "fade-speed", "Скорость затухания", "Как быстро клавиша гаснет после отпускания.", 8.0, 1.0, 40.0, 1);

    private final BoolSetting showInScreens = bool(G_MAIN, "show-in-screens", "Показывать в меню", "Рисовать, даже когда открыто меню (кроме чата).", false);

    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting keyColor = color(G_COLORS, "key", "Клавиша", "Фон отпущенной клавиши.", Theme.PANEL);

    private final ColorSetting pressedColor = color(G_COLORS, "pressed", "Нажатая клавиша", "Фон нажатой клавиши.", ColorSetting.argb(240, 240, 240, 230));

    private final ColorSetting outlineColor = color(G_COLORS, "outline", "Обводка", "Обводка клавиши.", ColorSetting.argb(90, 90, 90, 160));

    private final ColorSetting glowColor = color(G_COLORS, "glow", "Свечение", "Свечение вокруг нажатых клавиш.", Theme.GLOW);

    private final ColorSetting textColor = color(G_COLORS, "text", "Текст", "Цвет подписи отпущенной клавиши.", Theme.TEXT_SECONDARY);

    private final ColorSetting pressedTextColor = color(G_COLORS, "pressed-text", "Текст нажатой", "Цвет подписи нажатой клавиши.", 0xFF101010);

    private final ColorSetting shadowColor = color(G_COLORS, "shadow", "Тень", "Мягкая тень под каждой клавишей.", ColorSetting.argb(0, 0, 0, 70));

    // ---------------------------------------------------------------- state

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    // forward, left, back, right, attack, use, jump, sneak
    private final double[] anim = new double[8];

    private final PanelRender.FrameClock clock = new PanelRender.FrameClock();

    public Keystrokes() {
        super("keystrokes", "Клавиши", "Показывает клавиши движения, кнопки мыши и КПС.", Category.HUD);
    }

    @Override
    protected void onEnable() {
        leftClicks.clear();
        rightClicks.clear();
        Arrays.fill(anim, 0);
    }

    /** Called by {@code KeystrokesMouseMixin} for every mouse button press. */
    public static void onMouseClick(int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;
        Keystrokes module = ModuleManager.get(Keystrokes.class);
        if (module == null || !module.isEnabled()) return;
        long now = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) module.leftClicks.addLast(now);
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) module.rightClicks.addLast(now);
    }

    private static int cps(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        return clicks.size();
    }

    // ---------------------------------------------------------------- animation (driven every frame by the HUD)

    private KeyBinding[] keys() {
        return new KeyBinding[] {
            mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey,
            mc.options.attackKey, mc.options.useKey, mc.options.jumpKey, mc.options.sneakKey
        };
    }

    private void update() {
        double dt = clock.tick();
        KeyBinding[] keys = keys();
        for (int i = 0; i < keys.length; i++) {
            boolean pressed = keys[i].isPressed();
            // fast press, slower configurable release fade
            anim[i] = PanelRender.approach(anim[i], pressed ? 1 : 0, pressed ? 30 : fadeSpeed.get(), dt);
        }
    }

    // ---------------------------------------------------------------- HUD

    private int contentHeight() {
        int k = keySize.get();
        int g = gap.get();
        int h = 0;
        if (showMovement.get()) h += (k + g) * 2;
        if (showMouse.get()) h += k + g;
        if (showSpace.get() || showSneak.get()) h += k / 2 + 2 + g;
        if (h == 0) return 0;
        return h - g;
    }

    @Override
    public double getHudWidth() {
        return keySize.get() * 3 + gap.get() * 2;
    }

    @Override
    public double getHudHeight() {
        return Math.max(1, contentHeight());
    }

    @Override
    public boolean shouldRenderHud() {
        return contentHeight() > 0 && PanelRender.shouldRender(showInScreens.get());
    }

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.LEFT, HudPosition.AnchorY.BOTTOM, 4, -4);
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        update();
        if (contentHeight() == 0) return;

        long now = System.currentTimeMillis();
        int lCps = cps(leftClicks, now);
        int rCps = cps(rightClicks, now);
        KeyBinding[] keys = keys();

        int k = keySize.get();
        int g = gap.get();
        int totalW = k * 3 + g * 2;

        int cy = 0;
        if (showMovement.get()) {
            drawKey(ctx, k + g, cy, k, k, label(keys[0], "W"), null, anim[0]);
            cy += k + g;
            drawKey(ctx, 0, cy, k, k, label(keys[1], "A"), null, anim[1]);
            drawKey(ctx, k + g, cy, k, k, label(keys[2], "S"), null, anim[2]);
            drawKey(ctx, (k + g) * 2, cy, k, k, label(keys[3], "D"), null, anim[3]);
            cy += k + g;
        }

        if (showMouse.get()) {
            int half = (totalW - g) / 2;
            boolean cpsOn = showCps.get();
            drawKey(ctx, 0, cy, half, k, "ЛКМ", cpsOn ? lCps + " КПС" : null, anim[4]);
            drawKey(ctx, half + g, cy, totalW - half - g, k, "ПКМ", cpsOn ? rCps + " КПС" : null, anim[5]);
            cy += k + g;
        }

        if (showSpace.get() || showSneak.get()) {
            int sh2 = k / 2 + 2;
            if (showSpace.get() && showSneak.get()) {
                int spaceW = totalW - k - g;
                drawSpace(ctx, 0, cy, spaceW, sh2, anim[6]);
                drawKey(ctx, spaceW + g, cy, k, sh2, label(keys[7], "SHF"), null, anim[7]);
            } else if (showSpace.get()) {
                drawSpace(ctx, 0, cy, totalW, sh2, anim[6]);
            } else {
                drawKey(ctx, 0, cy, totalW, sh2, label(keys[7], "SHIFT"), null, anim[7]);
            }
        }
    }

    private String label(KeyBinding key, String fallback) {
        if (!boundNames.get()) return fallback;
        String s = key.getBoundKeyLocalizedText().getString();
        if (s.isEmpty()) return fallback;
        if (s.length() > 3) {
            String[] words = s.trim().split("\\s+");
            if (words.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (String word : words) if (!word.isEmpty() && sb.length() < 3) sb.append(word.charAt(0));
                s = sb.toString();
            } else s = s.substring(0, 3);
        }
        return s.toUpperCase(Locale.ROOT);
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, double p) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        // subtle "press in" scale
        float sc = (float) (1 - 0.06 * p);
        m.translate(x + w / 2f, y + h / 2f);
        m.scale(sc, sc);
        m.translate(-w / 2f, -h / 2f);

        PanelRender.shadow(ctx, 0, 0, w, h, 3, 2, shadowColor.get());
        if (p > 0.01) PanelRender.shadow(ctx, -1, -1, w + 1, h + 1, 4, 3, PanelRender.withAlpha(glowColor.get(), p));

        int bg = PanelRender.lerpColor(p, keyColor.get(), pressedColor.get());
        // same alpha, rgb darkened by 35% towards the bottom
        int bgBottom = (bg & 0xFF000000) | (PanelRender.lerpColor(0.35, bg | 0xFF000000, 0xFF000000) & 0x00FFFFFF);
        PanelRender.roundedGradient(ctx, 0, 0, w, h, 3, bg, bgBottom);
        PanelRender.ring(ctx, 0, 0, w, h, 3, 1, PanelRender.lerpColor(p, outlineColor.get(), glowColor.get() | 0xFF000000));
        m.popMatrix();
    }

    private void drawKey(DrawContext ctx, int x, int y, int w, int h, String text, String sub, double p) {
        drawBox(ctx, x, y, w, h, p);
        TextRenderer tr = mc.textRenderer;
        int color = PanelRender.lerpColor(p, textColor.get(), pressedTextColor.get());

        if (sub == null) {
            PanelRender.text(ctx, text, x + (w - tr.getWidth(text)) / 2, y + (h - 8) / 2, color, true);
        } else {
            int ty = y + h / 2 - 8;
            PanelRender.text(ctx, text, x + (w - tr.getWidth(text)) / 2, ty, color, true);
            Matrix3x2fStack m = ctx.getMatrices();
            m.pushMatrix();
            float sx = x + w / 2f;
            m.translate(sx, ty + 10);
            m.scale(0.7f, 0.7f);
            PanelRender.text(ctx, sub, -tr.getWidth(sub) / 2, 0, PanelRender.withAlpha(color, 0.85), true);
            m.popMatrix();
        }
    }

    private void drawSpace(DrawContext ctx, int x, int y, int w, int h, double p) {
        drawBox(ctx, x, y, w, h, p);
        int color = PanelRender.lerpColor(p, textColor.get(), pressedTextColor.get());
        int lw = Math.max(8, w / 3);
        int lx = x + (w - lw) / 2;
        int ly = y + h / 2;
        ctx.fill(lx, ly, lx + lw, ly + 1, color);
    }
}
