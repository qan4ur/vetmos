package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class IntSetting extends Setting<Integer> {
    public final int min, max;

    public IntSetting(String name, String title, String description, String group, int defaultValue, int min, int max) {
        super(name, title, description, group, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    protected Integer validate(Integer newValue) {
        return Math.max(min, Math.min(max, newValue));
    }

    @Override
    public boolean parse(String text) {
        try {
            set(Integer.parseInt(text.trim()));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
