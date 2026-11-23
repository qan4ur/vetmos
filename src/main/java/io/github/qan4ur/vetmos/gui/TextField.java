package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single-line text editor: caret, arrows, Home/End, Backspace/Delete, Ctrl+A (select all), Ctrl+C/X/V,
 * horizontal scrolling for long text. Works with any keyboard layout through charTyped.
 */
public class TextField implements TextInput {
    public int x, y, w, h = 14;
    /** Space reserved on the right inside the box (for a tag). */
    public int rightPad;
    public int maxLen = 256;
    public Supplier<String> placeholder = () -> "";
    /** Called after every edit with the new text. */
    public Consumer<String> onChange;
    /** Enter pressed. */
    public Runnable onEnter;
    /** Focus lost. */
    public Runnable onBlur;

    private String text = "";
    private int cursor;
    private boolean selectAll;
    private boolean focused;
    private int scrollPx;
    private long lastEdit;
    private float focusAnim, hover;

    @Override
    public String text() {
        return text;
    }

    @Override
    public void setText(String t) {
        text = t == null ? "" : t;
        cursor = Math.min(cursor, text.length());
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean f) {
        if (focused && !f) {
            selectAll = false;
            if (onBlur != null) onBlur.run();
        }
        if (!focused && f) cursor = text.length();
        focused = f;
    }

    private int innerX() {
        return x + 4;
    }

    private int innerW() {
        return Math.max(4, w - 8 - rightPad);
    }

    public void render(DrawContext ctx, int mx, int my) {
        boolean hov = Paint.inside(mx, my, x, y, w, h);
        hover = Anim.approach(hover, hov ? 1 : 0, 16);
        focusAnim = Anim.approach(focusAnim, focused ? 1 : 0, 14);
        Paint.round(ctx, x, y, w, h, 3, Anim.mix(0xE0101010, 0xF01A1A1A, Math.max(hover * 0.5f, focusAnim)));
        Paint.outline(ctx, x, y, w, h, 3, Anim.mix(Anim.mix(Palette.CONTROL_LINE, Palette.CONTROL_LINE_HOVER, hover), Palette.accent(), focusAnim));

        int ix = innerX(), iw = innerW(), ty = y + (h - 8) / 2;
        int caretPx = Paint.width(text.substring(0, cursor));
        int textW = Paint.width(text);
        if (caretPx - scrollPx > iw - 2) scrollPx = caretPx - iw + 2;
        if (caretPx < scrollPx) scrollPx = caretPx;
        scrollPx = Math.max(0, Math.min(scrollPx, Math.max(0, textW - iw + 2)));

        ctx.enableScissor(ix - 1, y + 1, ix + iw + 1, y + h - 1);
        if (text.isEmpty()) {
            if (!focused || placeholder != null) Paint.text(ctx, Paint.trim(placeholder.get(), iw), ix, ty, Palette.TEXT_FAINT);
        } else {
            if (selectAll && focused) Paint.rect(ctx, ix - scrollPx, ty - 1, textW, 10, Palette.accentA(90));
            Paint.text(ctx, text, ix - scrollPx, ty, Palette.TEXT);
        }
        boolean blink = (System.currentTimeMillis() - lastEdit) % 1000 < 500;
        if (focused && blink && !selectAll) Paint.rect(ctx, ix - scrollPx + caretPx, ty - 1, 1, 10, Palette.accent());
        ctx.disableScissor();
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (Paint.inside(mx, my, x, y, w, h)) {
            if (!focused) setFocused(true);
            selectAll = false;
            // caret at the clicked character
            double rel = mx - innerX() + scrollPx;
            int best = text.length();
            for (int i = 0; i <= text.length(); i++) {
                if (Paint.width(text.substring(0, i)) >= rel) {
                    int prev = i > 0 ? Paint.width(text.substring(0, i - 1)) : 0;
                    best = i > 0 && rel - prev < Paint.width(text.substring(0, i)) - rel ? i - 1 : i;
                    break;
                }
            }
            cursor = Math.max(0, Math.min(text.length(), best));
            lastEdit = System.currentTimeMillis();
            return true;
        }
        if (focused) setFocused(false);
        return false;
    }

    private void edited() {
        lastEdit = System.currentTimeMillis();
        if (onChange != null) onChange.accept(text);
    }

    private void insert(String s) {
        StringBuilder clean = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\n' || c == '\r') break;
            if (StringHelper.isValidChar(c)) clean.append(c);
        }
        if (selectAll) {
            text = "";
            cursor = 0;
            selectAll = false;
        }
        int room = maxLen - text.length();
        String add = clean.length() > room ? clean.substring(0, Math.max(0, room)) : clean.toString();
        if (add.isEmpty()) return;
        text = text.substring(0, cursor) + add + text.substring(cursor);
        cursor += add.length();
        edited();
    }

    @Override
    public boolean charTyped(char chr) {
        if (!focused) return false;
        if (!StringHelper.isValidChar(chr)) return true;
        insert(String.valueOf(chr));
        return true;
    }

    /** Consumes every key while focused (except Esc, which the screen uses to unfocus). */
    @Override
    public boolean keyPressed(int key) {
        if (!focused) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        lastEdit = System.currentTimeMillis();
        if (Screen.isSelectAll(key)) {
            selectAll = !text.isEmpty();
            return true;
        }
        if (Screen.isCopy(key)) {
            mc.keyboard.setClipboard(text);
            return true;
        }
        if (Screen.isCut(key)) {
            mc.keyboard.setClipboard(text);
            text = "";
            cursor = 0;
            selectAll = false;
            edited();
            return true;
        }
        if (Screen.isPaste(key)) {
            String clip = mc.keyboard.getClipboard();
            if (clip != null) insert(clip);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (selectAll) {
                    text = "";
                    cursor = 0;
                    selectAll = false;
                    edited();
                } else if (cursor > 0) {
                    int from = Screen.hasControlDown() ? wordStart() : cursor - 1;
                    text = text.substring(0, from) + text.substring(cursor);
                    cursor = from;
                    edited();
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (selectAll) {
                    text = "";
                    cursor = 0;
                    selectAll = false;
                    edited();
                } else if (cursor < text.length()) {
                    text = text.substring(0, cursor) + text.substring(cursor + 1);
                    edited();
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                cursor = selectAll ? 0 : Screen.hasControlDown() ? wordStart() : Math.max(0, cursor - 1);
                selectAll = false;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                cursor = selectAll ? text.length() : Screen.hasControlDown() ? wordEnd() : Math.min(text.length(), cursor + 1);
                selectAll = false;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursor = 0;
                selectAll = false;
            }
            case GLFW.GLFW_KEY_END -> {
                cursor = text.length();
                selectAll = false;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (onEnter != null) onEnter.run();
            }
            default -> {}
        }
        return true;
    }

    private int wordStart() {
        int i = cursor;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int wordEnd() {
        int i = cursor;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        while (i < text.length() && text.charAt(i) != ' ') i++;
        return i;
    }
}
