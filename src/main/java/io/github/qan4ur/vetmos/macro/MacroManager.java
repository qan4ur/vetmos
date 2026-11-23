package io.github.qan4ur.vetmos.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.qan4ur.vetmos.command.CommandManager;
import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.MacrosModule;
import io.github.qan4ur.vetmos.util.ChatUtil;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** All macros of the current profile. Saved inside the profile JSON (key "macros"). */
public final class MacroManager {
    public static final int MAX_TEXT = 256;
    private static final List<Macro> MACROS = new ArrayList<>();

    private MacroManager() {}

    public static List<Macro> all() {
        return Collections.unmodifiableList(MACROS);
    }

    public static Macro add(int key, String text, double cooldown) {
        Macro m = new Macro(key, trim(text), Math.max(0, cooldown));
        MACROS.add(m);
        ConfigManager.markDirty();
        return m;
    }

    public static Macro add() {
        return add(Keys.NONE, "", 1.0);
    }

    public static boolean remove(Macro macro) {
        boolean removed = MACROS.remove(macro);
        if (removed) ConfigManager.markDirty();
        return removed;
    }

    public static void clear() {
        MACROS.clear();
        ConfigManager.markDirty();
    }

    /** Call after editing a macro's fields from the GUI. */
    public static void changed() {
        ConfigManager.markDirty();
    }

    public static String trim(String text) {
        if (text == null) return "";
        String t = text.replace('\n', ' ').trim();
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }

    /** Key pressed in game (no screen open). */
    public static void onKey(int key) {
        MacrosModule module = ModuleManager.get(MacrosModule.class);
        if (module == null || !module.isEnabled() || key == Keys.NONE) return;
        for (Macro m : MACROS) {
            if (m.enabled && m.key == key && !m.text.isBlank()) run(m, module);
        }
    }

    /** Runs a macro, respecting its cooldown. Returns true if it was sent. */
    public static boolean run(Macro m, MacrosModule module) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return false;

        double left = m.cooldownLeft();
        if (left > 0) {
            if (module != null && module.cooldownMessage.get())
                ChatUtil.info(String.format(Locale.ROOT, Lang.ui("macro-cooldown", "Макрос «%s» перезаряжается: %.1fс"), m.label(), left));
            return false;
        }
        m.lastUsed = System.currentTimeMillis();

        String text = m.text;
        if (text.startsWith(CommandManager.PREFIX) && !text.startsWith(CommandManager.PREFIX + CommandManager.PREFIX)) {
            CommandManager.handle(text);
        } else if (text.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(text.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(text);
        }
        return true;
    }

    // ---------------------------------------------------------------- storage

    public static JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (Macro m : MACROS) arr.add(m.toJson());
        return arr;
    }

    public static void fromJson(JsonArray arr) {
        MACROS.clear();
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                MACROS.add(Macro.fromJson(e.getAsJsonObject()));
            } catch (RuntimeException ignored) {}
        }
    }
}
