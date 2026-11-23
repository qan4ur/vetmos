package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DoubleSetting extends Setting<Double> {
    public final double min, max;
    /** Number of decimals shown in the GUI and used when rounding slider values. */
    public final int decimals;

    public DoubleSetting(String name, String title, String description, String group, double defaultValue, double min, double max, int decimals) {
        super(name, title, description, group, defaultValue);
        this.min = min;
        this.max = max;
        this.decimals = decimals;
    }

    public DoubleSetting(String name, String title, String description, String group, double defaultValue, double min, double max) {
        this(name, title, description, group, defaultValue, min, max, 2);
    }

    public float getFloat() {
        return value.floatValue();
    }

    @Override
    protected Double validate(Double newValue) {
        double pow = Math.pow(10, decimals);
        double v = Math.round(newValue * pow) / pow;
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public boolean parse(String text) {
        try {
            set(Double.parseDouble(text.trim().replace(',', '.')));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String asString() {
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) set(json.getAsDouble());
    }
}
