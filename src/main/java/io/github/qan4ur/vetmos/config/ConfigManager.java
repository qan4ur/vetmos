package io.github.qan4ur.vetmos.config;

import com.google.gson.*;
import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Saves everything to {@code <game dir>/vetmos/config.json}. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("vetmos");
    private static final Path FILE = DIR.resolve("config.json");

    private static boolean dirty;
    private static boolean loading;
    private static int ticksSinceDirty;

    private ConfigManager() {}

    public static Path dir() {
        return DIR;
    }

    public static void markDirty() {
        if (loading) return;
        dirty = true;
        ticksSinceDirty = 0;
    }

    /** Saves a second after the last change. */
    public static void tick() {
        if (!dirty) return;
        if (++ticksSinceDirty >= 20) save();
    }

    public static void load() {
        loading = true;
        try {
            JsonObject root = null;
            if (Files.exists(FILE)) {
                try {
                    root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (Exception e) {
                    Vetmos.LOG.error("Could not read {}, using defaults", FILE, e);
                }
            }

            JsonObject modules = root != null && root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module module : ModuleManager.all()) {
                JsonObject m = modules.has(module.name) ? modules.getAsJsonObject(module.name) : null;
                if (m == null) {
                    module.setEnabled(module.isEnabledByDefault());
                    continue;
                }
                if (m.has("settings")) {
                    JsonObject s = m.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (s.has(setting.name)) {
                            try {
                                setting.fromJson(s.get(setting.name));
                            } catch (RuntimeException e) {
                                Vetmos.LOG.warn("Bad value for {}.{}", module.name, setting.name);
                            }
                        }
                    }
                }
                if (m.has("key")) module.setKey(m.get("key").getAsInt());
                module.setEnabled(m.has("enabled") ? m.get("enabled").getAsBoolean() : module.isEnabledByDefault());
            }

            JsonObject hud = root != null && root.has("hud") ? root.getAsJsonObject("hud") : new JsonObject();
            for (Module module : HudManager.hudModules()) {
                HudPosition def = ((HudModule) module).defaultHudPosition();
                HudPosition pos = hud.has(module.name) ? HudPosition.fromJson(hud.getAsJsonObject(module.name), def) : def.copy();
                HudManager.positions().put(module.name, pos);
            }
        } finally {
            loading = false;
            dirty = false;
        }
    }

    public static void save() {
        dirty = false;
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);

        JsonObject modules = new JsonObject();
        for (Module module : ModuleManager.all()) {
            JsonObject m = new JsonObject();
            m.addProperty("enabled", module.isEnabled());
            m.addProperty("key", module.getKey());
            JsonObject s = new JsonObject();
            for (Setting<?> setting : module.getSettings()) s.add(setting.name, setting.toJson());
            m.add("settings", s);
            modules.add(module.name, m);
        }
        root.add("modules", modules);

        JsonObject hud = new JsonObject();
        for (Module module : HudManager.hudModules()) hud.add(module.name, HudManager.getPosition(module).toJson());
        root.add("hud", hud);

        try {
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve("config.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vetmos.LOG.error("Could not save {}", FILE, e);
        }
    }
}
