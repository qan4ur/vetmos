package io.github.qan4ur.vetmos.module;

import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of every Vetmos feature. A module has a kebab-case name (used by commands), a title (shown in the GUI),
 * a category, an on/off state, an optional key bind and a list of settings.
 */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    /** Display title (Russian). */
    public final String title;
    /** Short Russian description shown on hover in the ClickGUI. */
    public final String description;
    public final Category category;

    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private final boolean defaultEnabled;
    /** Key code (see {@link io.github.qan4ur.vetmos.util.Keys}), {@code Keys.NONE} when unbound. */
    private int key;
    private final int defaultKey;

    protected Module(String name, String title, String description, Category category) {
        this(name, title, description, category, false, io.github.qan4ur.vetmos.util.Keys.NONE);
    }

    protected Module(String name, String title, String description, Category category, boolean defaultEnabled, int defaultKey) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = false;
        this.defaultKey = defaultKey;
        this.key = defaultKey;
    }

    // ---------------------------------------------------------------- settings

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    // group = Russian group title shown in the GUI, name = English kebab-case key, title/description = Russian

    protected BoolSetting bool(String group, String name, String title, String description, boolean def) {
        return add(new BoolSetting(name, title, description, group, def));
    }

    protected IntSetting integer(String group, String name, String title, String description, int def, int min, int max) {
        return add(new IntSetting(name, title, description, group, def, min, max));
    }

    protected DoubleSetting decimal(String group, String name, String title, String description, double def, double min, double max) {
        return add(new DoubleSetting(name, title, description, group, def, min, max));
    }

    protected DoubleSetting decimal(String group, String name, String title, String description, double def, double min, double max, int decimals) {
        return add(new DoubleSetting(name, title, description, group, def, min, max, decimals));
    }

    protected <E extends Enum<E>> EnumSetting<E> enumeration(String group, String name, String title, String description, E def) {
        return add(new EnumSetting<>(name, title, description, group, def));
    }

    protected ColorSetting color(String group, String name, String title, String description, int defArgb) {
        return add(new ColorSetting(name, title, description, group, defArgb));
    }

    /** The standard "scale" setting of HUD modules. */
    protected DoubleSetting hudScaleSetting() {
        return decimal("HUD", "scale", "Размер", "Размер элемента на экране (также колёсико мыши в редакторе HUD).", 1.0, 0.3, 4.0);
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public Setting<?> getSetting(String name) {
        for (Setting<?> s : settings) if (s.name.equalsIgnoreCase(name) || s.title.equalsIgnoreCase(name)) return s;
        return null;
    }

    /** Resets every setting to its default value (the key bind and on/off state are kept). */
    public void resetSettings() {
        for (Setting<?> s : settings) s.reset();
        ConfigManager.markDirty();
    }

    // ---------------------------------------------------------------- state

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnabledByDefault() {
        return defaultEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
        ConfigManager.markDirty();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Toggle triggered by the user (key bind / command) — also prints a chat message. */
    public void toggleWithFeedback() {
        toggle();
        if (mc.player != null) ChatUtil.moduleToggled(this);
    }

    public int getKey() {
        return key;
    }

    public int getDefaultKey() {
        return defaultKey;
    }

    public void setKey(int key) {
        this.key = key;
        ConfigManager.markDirty();
    }

    // ---------------------------------------------------------------- hooks (called only while enabled)

    protected void onEnable() {}

    protected void onDisable() {}

    /** End of every client tick. */
    public void onTick() {}

    /** The local player attacked an entity (client side, before the packet is sent). */
    public void onAttack(Entity target) {}

    /** EntityStatusS2CPacket handled on the main thread (e.g. 35 = totem pop, 3 = death). */
    public void onEntityStatus(Entity entity, byte status) {}

    /** Called once at startup after all modules are registered and the config is loaded (register Fabric hooks here). */
    public void onInit() {}

    /** Joined a world / server. */
    public void onJoin() {}

    /** Left the world / server. */
    public void onLeave() {}
}
