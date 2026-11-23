package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.qan4ur.vetmos.util.Keys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * A key or mouse button chosen in the module settings (see {@link Keys}). Unlike vanilla key bindings it never
 * conflicts with other binds - Vetmos reads the key state directly.
 */
public class KeySetting extends Setting<Integer> {
    public KeySetting(String name, String title, String description, String group, int defaultKey) {
        super(name, title, description, group, defaultKey);
    }

    /** Whether the key / mouse button is held right now (in game window). */
    public boolean isPressed() {
        int key = value;
        if (key == Keys.NONE) return false;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (Keys.isMouse(key)) return GLFW.glfwGetMouseButton(window, key - Keys.MOUSE_OFFSET) == GLFW.GLFW_PRESS;
        return InputUtil.isKeyPressed(window, key);
    }

    @Override
    public boolean parse(String text) {
        Integer key = Keys.parse(text);
        if (key == null) return false;
        set(key);
        return true;
    }

    @Override
    public String asString() {
        return Keys.name(value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) set(json.getAsInt());
    }
}
