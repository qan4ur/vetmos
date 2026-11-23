package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB color (alpha included). */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String title, String description, String group, int defaultArgb) {
        super(name, title, description, group, defaultArgb);
    }

    public static int argb(int r, int g, int b, int a) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static int rgb(int r, int g, int b) {
        return argb(r, g, b, 255);
    }

    public int alpha() { return value >>> 24; }
    public int red() { return value >> 16 & 255; }
    public int green() { return value >> 8 & 255; }
    public int blue() { return value & 255; }

    /** Color with the alpha multiplied by the given factor (0..1). */
    public int withAlpha(float factor) {
        int a = Math.round(alpha() * Math.max(0, Math.min(1, factor)));
        return (a << 24) | (value & 0xFFFFFF);
    }

    @Override
    public boolean parse(String text) {
        String t = text.trim();
        try {
            if (t.startsWith("#")) t = t.substring(1);
            if (t.contains(",") || t.contains(" ")) {
                String[] p = t.split("[,\\s]+");
                int r = Integer.parseInt(p[0]), g = Integer.parseInt(p[1]), b = Integer.parseInt(p[2]);
                int a = p.length > 3 ? Integer.parseInt(p[3]) : 255;
                set(argb(r, g, b, a));
                return true;
            }
            long v = Long.parseLong(t, 16);
            if (t.length() <= 6) v |= 0xFF000000L;
            set((int) v);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String asString() {
        return String.format("#%08X (%d, %d, %d, %d)", value, red(), green(), blue(), alpha());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", value));
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) parse(json.getAsString());
    }
}
