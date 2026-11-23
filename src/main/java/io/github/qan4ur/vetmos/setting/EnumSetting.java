package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;

    public EnumSetting(String name, String title, String description, String group, E defaultValue) {
        super(name, title, description, group, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getValues() {
        return values;
    }

    public void cycle(boolean forward) {
        int i = value.ordinal() + (forward ? 1 : -1);
        set(values[(i + values.length) % values.length]);
    }

    @Override
    public boolean parse(String text) {
        for (E e : values) {
            if (e.name().equalsIgnoreCase(text.trim()) || e.toString().equalsIgnoreCase(text.trim())) {
                set(e);
                return true;
            }
        }
        return false;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return;
        for (E e : values) if (e.name().equals(json.getAsString())) set(e);
    }
}
