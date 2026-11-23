package io.github.qan4ur.vetmos.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Key codes used for module binds. Keyboard keys use their GLFW key code, mouse buttons are stored as
 * {@code MOUSE_OFFSET + button} so both fit in one int.
 */
public final class Keys {
    public static final int NONE = -1;
    public static final int MOUSE_OFFSET = 1000;

    private Keys() {}

    public static int mouse(int button) {
        return MOUSE_OFFSET + button;
    }

    public static boolean isMouse(int key) {
        return key >= MOUSE_OFFSET;
    }

    public static String name(int key) {
        if (key == NONE) return "none";
        if (isMouse(key)) return "mouse" + (key - MOUSE_OFFSET + 1);
        // Layout-independent names (G stays G on a Russian keyboard)
        InputUtil.Key k = InputUtil.Type.KEYSYM.createFromCode(key);
        String translation = k.getTranslationKey();
        if (translation.startsWith("key.keyboard.")) return translation.substring("key.keyboard.".length()).toUpperCase(Locale.ROOT);
        String glfw = GLFW.glfwGetKeyName(key, 0);
        if (glfw != null && !glfw.isBlank()) return glfw.toUpperCase(Locale.ROOT);
        return String.valueOf(key);
    }

    /** Parses names like "z", "rshift", "right.shift", "f5", "mouse3", "none". Returns null if unknown. */
    public static Integer parse(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.equals("none") || t.equals("unbind") || t.isEmpty()) return NONE;
        if (t.startsWith("mouse")) {
            try {
                int n = Integer.parseInt(t.substring(5));
                if (n >= 1 && n <= 8) return mouse(n - 1);
            } catch (NumberFormatException ignored) {}
            return null;
        }
        String alias = switch (t) {
            case "rshift" -> "right.shift";
            case "lshift", "shift" -> "left.shift";
            case "rctrl" -> "right.control";
            case "lctrl", "ctrl" -> "left.control";
            case "ralt" -> "right.alt";
            case "lalt", "alt" -> "left.alt";
            case "esc" -> "escape";
            case "del" -> "delete";
            case "ins" -> "insert";
            default -> t.replace('_', '.');
        };
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + alias);
            if (key != InputUtil.UNKNOWN_KEY) return key.getCode();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }
}
