package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * "Клавиша: Z" row. Click it, then press a key or a mouse button (except LMB, which cancels).
 * Esc / Backspace / Delete remove the bind.
 */
public class KeybindWidget extends Widget {
    /** The widget currently waiting for a key, if any. */
    static KeybindWidget listening;

    private final java.util.function.IntSupplier getter;
    private final java.util.function.IntConsumer setter;
    /** In compact mode: prefix the key with "Клавиша:". */
    public boolean showLabel = true;
    private float listen;
    private float flash;

    /** Compact mode: the whole widget is the box, labelled "Клавиша: Z" (used in the settings window header). */
    public boolean compact;

    public KeybindWidget(Module module) {
        this(module::getKey, module::setKey);
    }

    /** Generic bind (e.g. a macro key). */
    public KeybindWidget(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        this.getter = getter;
        this.setter = setter;
    }

    /** Width the compact box wants for the current key. */
    public int preferredWidth() {
        return Paint.width(boxText()) + 12;
    }

    private String keyName() {
        return isListening() ? "..." : getter.getAsInt() == Keys.NONE ? Lang.ui("key-none", "нет") : Keys.name(getter.getAsInt());
    }

    private String boxText() {
        return compact && showLabel ? Lang.ui("key", "Клавиша") + ": " + keyName() : keyName();
    }

    @Override
    public int height() {
        return ROW + 1;
    }

    public boolean isListening() {
        return listening == this;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        boolean hov = isHovered(mx, my);
        updateHover(hov);
        drawHoverBg(ctx, height());
        listen = Anim.approach(listen, isListening() ? 1 : 0, 14);
        flash = Anim.approach(flash, 0, 5);

        String keyName = boxText();
        int boxW = compact ? w : Math.max(28, Paint.width(keyName) + 12);
        int bx = x + w - boxW, by = y + 1, bh = height() - 2;

        if (!compact) Paint.text(ctx, Lang.ui("key", "Клавиша"), x, y + 3, textColor(hover));
        else drawHoverBg(ctx, 0);
        float pulse = isListening() ? 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 140.0) : 0;
        Paint.button(ctx, bx, by, boxW, bh, Paint.styled(""), Math.max(hover, listen * (0.5f + 0.5f * pulse)), flash, true);
        if (listen > 0.02f) Paint.rect(ctx, bx, by + bh - 2, boxW, 2, Anim.alpha(Palette.accent(), listen * (0.6f + 0.4f * pulse)));
        int textColor = getter.getAsInt() == Keys.NONE && !isListening() ? Palette.TEXT_MUTED : Palette.TEXT;
        Paint.textCentered(ctx, keyName, bx + boxW / 2, by + (bh - 10) / 2, textColor);

        if (hov) {
            tip.offer(this, Lang.ui("key-title", "Клавиша модуля"), isListening()
                ? Lang.ui("key-listening", "Нажмите клавишу или кнопку мыши. Esc, Backspace или Delete — убрать клавишу, ЛКМ — отмена.")
                : Lang.ui("key-hint", "Нажмите, чтобы назначить клавишу или кнопку мыши для включения и выключения модуля."));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            listening = isListening() ? null : this;
            click(1.2f);
            return true;
        }
        return false;
    }

    /** Called by the screen while listening. Returns true when consumed. */
    public boolean handleMouse(int button) {
        if (button == 0) {
            listening = null;
            return true;
        }
        bind(Keys.mouse(button));
        return true;
    }

    public boolean handleKey(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) bind(Keys.NONE);
        else bind(key);
        return true;
    }

    private void bind(int key) {
        setter.accept(key);
        listening = null;
        flash = 1;
        click(1.5f);
    }
}
