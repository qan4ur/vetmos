package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

/** Minimal one-line text field for the module search (works with any keyboard layout via charTyped). */
public class SearchField implements TextInput {
    /** Placeholder shown while empty (a supplier so it follows the language). */
    public java.util.function.Supplier<String> placeholder = () -> io.github.qan4ur.vetmos.util.Lang.ui("search-modules", "Поиск модулей…");
    public int maxLen = 32;
    /** Called on Enter (may be null). */
    public Runnable onEnter;
    /** Draw the ⌕ icon. */
    public boolean icon = true;
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

        Paint.field(ctx, x, y, w, h, hover, focusAnim);

        int ty = y + (h - 8) / 2;
        if (icon) Paint.text(ctx, "⌕", x + 5, ty, Anim.mix(Palette.TEXT_FAINT, Palette.accent(), focusAnim));
        int tx = x + (icon ? 15 : 5);
        int maxW = w - (icon ? 20 : 10) - 8;
        if (text.isEmpty()) {
            Paint.text(ctx, Paint.trim(placeholder.get(), maxW), tx, ty, Palette.TEXT_FAINT);
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
        if (text.length() >= maxLen) return true;
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
                    if (StringHelper.isValidChar(c) && text.length() < maxLen) text += c;
                }
                lastEdit = System.currentTimeMillis();
            }
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && onEnter != null) {
            onEnter.run();
            return true;
        }
        if (Screen.isSelectAll(key)) {
            text = "";
            return true;
        }
        return false;
    }
}
