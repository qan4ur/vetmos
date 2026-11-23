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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Settings storage with profiles.
 * <pre>
 *   &lt;game dir&gt;/vetmos/state.json            current profile, first-launch flag
 *   &lt;game dir&gt;/vetmos/profiles/&lt;name&gt;.json    one file per profile (modules + HUD positions)
 * </pre>
 * A profile can also be exported as a short text code ({@link #exportCode()}) and imported by a friend.
 */
public final class ConfigManager {
    public static final String DEFAULT_PROFILE = "default";
    private static final String CODE_PREFIX = "VETMOS1:";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("vetmos");
    private static final Path PROFILES = DIR.resolve("profiles");
    private static final Path STATE = DIR.resolve("state.json");
    private static final Path LEGACY = DIR.resolve("config.json");

    private static String currentProfile = DEFAULT_PROFILE;
    private static boolean welcomeShown;

    private static boolean dirty;
    private static boolean loading;
    private static int ticksSinceDirty;

    private ConfigManager() {}

    public static Path dir() {
        return DIR;
    }

    // ---------------------------------------------------------------- dirty / autosave

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

    // ---------------------------------------------------------------- startup

    public static void load() {
        readState();
        migrateLegacy();
        JsonObject root = read(profileFile(currentProfile));
        apply(root);
    }

    public static void save() {
        dirty = false;
        write(profileFile(currentProfile), toJson());
        writeState();
    }

    // ---------------------------------------------------------------- first launch

    public static boolean isWelcomeShown() {
        return welcomeShown;
    }

    public static void setWelcomeShown(boolean shown) {
        welcomeShown = shown;
        writeState();
    }

    // ---------------------------------------------------------------- profiles

    public static String currentProfile() {
        return currentProfile;
    }

    /** Profile names sorted alphabetically (the current one is always included). */
    public static List<String> listProfiles() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.add(currentProfile);
        if (Files.isDirectory(PROFILES)) {
            try (Stream<Path> files = Files.list(PROFILES)) {
                files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String n = p.getFileName().toString();
                        names.add(n.substring(0, n.length() - 5));
                    });
            } catch (IOException e) {
                Vetmos.LOG.error("Could not list profiles", e);
            }
        }
        return new ArrayList<>(names);
    }

    public static boolean isValidProfileName(String name) {
        return name != null && name.matches("[A-Za-z0-9А-Яа-яЁё_\\- ]{1,32}") && !name.isBlank();
    }

    public static boolean profileExists(String name) {
        return Files.exists(profileFile(name));
    }

    /**
     * Saves the current profile and switches to another one. A profile that doesn't exist yet is created as a copy of
     * the current settings.
     */
    public static boolean switchProfile(String name) {
        if (!isValidProfileName(name)) return false;
        save();
        boolean existed = profileExists(name);
        currentProfile = name.trim();
        if (existed) apply(read(profileFile(currentProfile)));
        save();
        return true;
    }

    /** Creates a new profile: a copy of the current settings, or all defaults. Does not switch to it. */
    public static boolean createProfile(String name, boolean copyCurrent) {
        if (!isValidProfileName(name) || profileExists(name)) return false;
        save();
        JsonObject json = copyCurrent ? toJson() : defaultsJson();
        write(profileFile(name.trim()), json);
        return true;
    }

    /** Deletes a profile (not the current one). */
    public static boolean deleteProfile(String name) {
        if (name == null || name.equalsIgnoreCase(currentProfile)) return false;
        try {
            return Files.deleteIfExists(profileFile(name));
        } catch (IOException e) {
            Vetmos.LOG.error("Could not delete profile {}", name, e);
            return false;
        }
    }

    // ---------------------------------------------------------------- share codes

    /** Current settings and HUD layout as a compact text code that can be sent to a friend. */
    public static String exportCode() {
        try {
            byte[] json = GSON.toJson(toJson()).getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
                gz.write(json);
            }
            return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Applies a code from {@link #exportCode()} to the current profile. Returns false if the code is invalid. */
    public static boolean importCode(String code) {
        if (code == null) return false;
        String c = code.trim();
        if (!c.startsWith(CODE_PREFIX)) return false;
        try {
            byte[] data = Base64.getUrlDecoder().decode(c.substring(CODE_PREFIX.length()));
            String json;
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
                json = new String(gz.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("modules")) return false;
            apply(root);
            save();
            return true;
        } catch (RuntimeException | IOException e) {
            Vetmos.LOG.warn("Invalid Vetmos config code", e);
            return false;
        }
    }

    // ---------------------------------------------------------------- internals

    private static Path profileFile(String name) {
        return PROFILES.resolve(name.trim() + ".json");
    }

    private static JsonObject read(Path file) {
        if (!Files.exists(file)) return null;
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            Vetmos.LOG.error("Could not read {}, using defaults", file, e);
            return null;
        }
    }

    private static void write(Path file, JsonElement json) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vetmos.LOG.error("Could not save {}", file, e);
        }
    }

    private static void readState() {
        JsonObject state = read(STATE);
        if (state == null) return;
        if (state.has("profile") && isValidProfileName(state.get("profile").getAsString())) currentProfile = state.get("profile").getAsString();
        if (state.has("welcomeShown")) welcomeShown = state.get("welcomeShown").getAsBoolean();
    }

    private static void writeState() {
        JsonObject state = new JsonObject();
        state.addProperty("profile", currentProfile);
        state.addProperty("welcomeShown", welcomeShown);
        write(STATE, state);
    }

    /** 1.0.0 stored everything in vetmos/config.json — becomes the "default" profile. */
    private static void migrateLegacy() {
        if (!Files.exists(LEGACY) || Files.exists(profileFile(DEFAULT_PROFILE))) return;
        try {
            Files.createDirectories(PROFILES);
            Files.move(LEGACY, profileFile(DEFAULT_PROFILE));
            welcomeShown = true; // existing user
            Vetmos.LOG.info("Moved vetmos/config.json to profile '{}'", DEFAULT_PROFILE);
        } catch (IOException e) {
            Vetmos.LOG.error("Could not migrate {}", LEGACY, e);
        }
    }

    /** Applies a profile JSON (null = all defaults) to the modules and the HUD. */
    private static void apply(JsonObject root) {
        loading = true;
        try {
            JsonObject modules = root != null && root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module module : ModuleManager.all()) {
                JsonObject m = modules.has(module.name) ? modules.getAsJsonObject(module.name) : null;
                for (Setting<?> setting : module.getSettings()) {
                    if (m != null && m.has("settings") && m.getAsJsonObject("settings").has(setting.name)) {
                        try {
                            setting.fromJson(m.getAsJsonObject("settings").get(setting.name));
                        } catch (RuntimeException e) {
                            Vetmos.LOG.warn("Bad value for {}.{}", module.name, setting.name);
                            setting.reset();
                        }
                    } else {
                        setting.reset();
                    }
                }
                module.setKey(m != null && m.has("key") ? m.get("key").getAsInt() : module.getDefaultKey());
                module.setEnabled(m != null && m.has("enabled") ? m.get("enabled").getAsBoolean() : module.isEnabledByDefault());
            }

            JsonObject hud = root != null && root.has("hud") ? root.getAsJsonObject("hud") : new JsonObject();
            for (Module module : HudManager.hudModules()) {
                HudPosition def = ((HudModule) module).defaultHudPosition();
                HudPosition pos = hud.has(module.name) ? HudPosition.fromJson(hud.getAsJsonObject(module.name), def) : def.copy();
                HudManager.positions().put(module.name, pos);
            }

            io.github.qan4ur.vetmos.macro.MacroManager.fromJson(root != null && root.has("macros") ? root.getAsJsonArray("macros") : null);
        } finally {
            loading = false;
            dirty = false;
        }
    }

    private static JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);

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
        root.add("macros", io.github.qan4ur.vetmos.macro.MacroManager.toJson());
        return root;
    }

    /** JSON of a profile with every module at defaults (modules enabled by default stay enabled). */
    private static JsonObject defaultsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);
        JsonObject modules = new JsonObject();
        for (Module module : ModuleManager.all()) {
            JsonObject m = new JsonObject();
            m.addProperty("enabled", module.isEnabledByDefault());
            m.addProperty("key", module.getDefaultKey());
            JsonObject s = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                Object current = setting.get();
                resetQuiet(setting);
                s.add(setting.name, setting.toJson());
                restoreQuiet(setting, current);
            }
            m.add("settings", s);
            modules.add(module.name, m);
        }
        root.add("modules", modules);
        JsonObject hud = new JsonObject();
        for (Module module : HudManager.hudModules()) hud.add(module.name, ((HudModule) module).defaultHudPosition().toJson());
        root.add("hud", hud);
        return root;
    }

    private static void resetQuiet(Setting<?> setting) {
        boolean l = loading;
        loading = true;
        setting.reset();
        loading = l;
    }

    @SuppressWarnings("unchecked")
    private static <T> void restoreQuiet(Setting<T> setting, Object value) {
        boolean l = loading;
        loading = true;
        setting.set((T) value);
        loading = l;
    }
}
