package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

/** Minimal one-line text field for the module search (works with any keyboard layout via charTyped). */
public class SearchField {
    private static final int MAX_LEN = 32;
    public int x, y, w, h;
    private String text = "";
    private boolean focused;
    private float focusAnim, hover;
    private long lastEdit;

    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void render(DrawContext ctx, int mx, int my) {
        boolean hov = Paint.inside(mx, my, x, y, w, h);
        hover = Anim.approach(hover, hov ? 1 : 0, 16);
        focusAnim = Anim.approach(focusAnim, focused ? 1 : 0, 14);

        if (focusAnim > 0.02f) Paint.glow(ctx, x, y, w, h, 4, Anim.alpha(Palette.glow(), focusAnim * 0.7f), 3);
        Paint.round(ctx, x, y, w, h, 4, Anim.mix(0xE0121212, 0xF01C1C1C, Math.max(hover * 0.5f, focusAnim)));
        Paint.outline(ctx, x, y, w, h, 4, Anim.mix(Anim.mix(Palette.CONTROL_LINE, Palette.CONTROL_LINE_HOVER, hover), Palette.accent(), focusAnim));

        int ty = y + (h - 8) / 2;
        Paint.text(ctx, "⌕", x + 5, ty, Anim.mix(Palette.TEXT_FAINT, Palette.accent(), focusAnim));
        int tx = x + 15;
        int maxW = w - 20;
        if (text.isEmpty()) {
            Paint.text(ctx, "Поиск модулей…", tx, ty, Palette.TEXT_FAINT);
        } else {
            String shown = text;
            while (Paint.width(shown) > maxW && shown.length() > 1) shown = shown.substring(1);
            Paint.text(ctx, shown, tx, ty, Palette.TEXT);
            tx += Paint.width(shown);
        }
        boolean blink = (System.currentTimeMillis() - lastEdit) % 1000 < 500;
        if (focused && blink) Paint.rect(ctx, tx + (text.isEmpty() ? 0 : 1), ty - 1, 1, 10, Palette.accent());

        if (!text.isEmpty()) {
            boolean xh = Paint.inside(mx, my, x + w - 12, y, 12, h);
            Paint.text(ctx, "×", x + w - 9, ty, xh ? Palette.accent() : Palette.TEXT_MUTED);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (Paint.inside(mx, my, x, y, w, h)) {
            if (!text.isEmpty() && (button == 1 || Paint.inside(mx, my, x + w - 12, y, 12, h))) {
                text = "";
                lastEdit = System.currentTimeMillis();
            }
            focused = true;
            return true;
        }
        focused = false;
        return false;
    }

    public boolean charTyped(char chr) {
        if (!StringHelper.isValidChar(chr)) return false;
        if (text.length() >= MAX_LEN) return true;
        text += chr;
        focused = true;
        lastEdit = System.currentTimeMillis();
        return true;
    }

    /** Editing keys. Returns true when consumed. */
    public boolean keyPressed(int key) {
        if (!focused) return false;
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty()) {
                text = Screen.hasControlDown() ? "" : text.substring(0, text.length() - 1);
                lastEdit = System.currentTimeMillis();
            }
            return true;
        }
        if (Screen.isPaste(key)) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) {
                for (char c : clip.toCharArray()) {
                    if (c == '\n' || c == '\r') break;
                    if (StringHelper.isValidChar(c) && text.length() < MAX_LEN) text += c;
                }
                lastEdit = System.currentTimeMillis();
            }
            return true;
        }
        if (Screen.isSelectAll(key)) {
            text = "";
            return true;
        }
        return false;
    }
}
