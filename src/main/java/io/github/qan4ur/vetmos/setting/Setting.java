package io.github.qan4ur.vetmos.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** A single module option shown in the ClickGUI and editable with .settings. */
public abstract class Setting<T> {
    /** Internal English kebab-case name (config key, used by .settings). */
    public final String name;
    /** Display title (Russian) shown in the ClickGUI. */
    public final String title;
    /** Tooltip (Russian). */
    public final String description;
    public final String group;
    protected final T defaultValue;
    protected T value;
    private BooleanSupplier visible = () -> true;
    private Runnable onChange;

    protected Setting(String name, String title, String description, String group, T defaultValue) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.group = group;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        T clamped = validate(newValue);
        if (clamped == null) return;
        boolean changed = !clamped.equals(value);
        value = clamped;
        if (changed) {
            io.github.qan4ur.vetmos.config.ConfigManager.markDirty();
            if (onChange != null) onChange.run();
        }
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    /** Returns the value to store (clamped), or null to reject. */
    protected T validate(T newValue) {
        return newValue;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visible = supplier;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Runnable runnable) {
        this.onChange = runnable;
        return (S) this;
    }

    /** Parses a value typed by the user in chat (.settings). Returns false if invalid. */
    public abstract boolean parse(String text);

    /** Text form of the current value (chat, tooltips). */
    public String asString() {
        return String.valueOf(value);
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);
}
