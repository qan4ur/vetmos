package io.github.qan4ur.vetmos.command;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.Setting;
import io.github.qan4ur.vetmos.util.ChatUtil;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

/** Chat commands starting with "." — handled locally, never sent to the server. */
public final class CommandManager {
    public static final String PREFIX = ".";

    private CommandManager() {}

    /** Returns true if the message was a Vetmos command (and must not be sent). */
    public static boolean handle(String message) {
        if (!message.startsWith(PREFIX) || message.length() == PREFIX.length()) return false;
        String[] args = message.substring(PREFIX.length()).trim().split("\\s+");
        String cmd = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (cmd) {
                case "help", "commands" -> help();
                case "toggle", "t" -> toggle(rest);
                case "bind" -> bind(rest);
                case "binds" -> binds();
                case "unbind" -> bind(rest.length > 0 ? new String[] { rest[0], "none" } : rest);
                case "settings", "set", "s" -> settings(rest);
                case "reset" -> reset(rest);
                case "modules", "list" -> modules();
                case "gui" -> Vetmos.openScreenNextTick(Vetmos.createClickGui());
                case "hud" -> Vetmos.openScreenNextTick(Vetmos.createHudEditor());
                default -> ChatUtil.error("Unknown command '" + cmd + "'. Type " + PREFIX + "help");
            }
        } catch (RuntimeException e) {
            ChatUtil.error("Command failed: " + e.getMessage());
            Vetmos.LOG.error("Command failed: {}", message, e);
        }
        return true;
    }

    private static Module module(String name) {
        Module m = ModuleManager.get(name);
        if (m == null) ChatUtil.error("No module named '" + name + "'. Type " + PREFIX + "modules");
        return m;
    }

    private static void help() {
        ChatUtil.info("Commands:");
        line(".toggle <module>", "turn a module on/off");
        line(".bind <module> <key|mouse3|none>", "bind a key");
        line(".binds", "list binds");
        line(".settings <module>", "list settings");
        line(".settings <module> <setting> <value>", "change a setting");
        line(".reset <module>", "reset a module's settings to defaults");
        line(".reset bind <module> | .reset hud [module] | .reset all", "");
        line(".modules", "list modules");
        line(".gui / .hud", "open ClickGUI / HUD editor");
    }

    private static void line(String usage, String description) {
        MutableText t = Text.literal(usage).withColor(Theme.accent() & 0xFFFFFF);
        if (!description.isEmpty()) t.append(Text.literal(" - " + description).formatted(Formatting.GRAY));
        ChatUtil.send(t);
    }

    private static void toggle(String[] args) {
        if (args.length < 1) {
            ChatUtil.error("Usage: .toggle <module>");
            return;
        }
        Module m = module(args[0]);
        if (m != null) m.toggleWithFeedback();
    }

    private static void bind(String[] args) {
        if (args.length < 2) {
            ChatUtil.error("Usage: .bind <module> <key|mouse3|none>");
            return;
        }
        Module m = module(args[0]);
        if (m == null) return;
        Integer key = Keys.parse(args[1]);
        if (key == null) {
            ChatUtil.error("Unknown key '" + args[1] + "'");
            return;
        }
        m.setKey(key);
        ChatUtil.info(m.title + " bound to " + Keys.name(key));
    }

    private static void binds() {
        int n = 0;
        for (Module m : ModuleManager.all()) {
            if (m.getKey() == Keys.NONE) continue;
            ChatUtil.send(Text.literal(m.title).formatted(Formatting.WHITE).append(Text.literal(" - " + Keys.name(m.getKey())).formatted(Formatting.GRAY)));
            n++;
        }
        if (n == 0) ChatUtil.info("No binds.");
    }

    private static void settings(String[] args) {
        if (args.length < 1) {
            ChatUtil.error("Usage: .settings <module> [setting] [value]");
            return;
        }
        Module m = module(args[0]);
        if (m == null) return;

        if (args.length == 1) {
            ChatUtil.info(m.title + " settings:");
            for (Setting<?> s : m.getSettings()) {
                ChatUtil.send(Text.literal(s.name).withColor(Theme.accent() & 0xFFFFFF)
                    .append(Text.literal(" = " + s.asString()).formatted(Formatting.WHITE))
                    .append(Text.literal("  (" + s.description + ")").formatted(Formatting.DARK_GRAY)));
            }
            return;
        }

        Setting<?> s = m.getSetting(args[1]);
        if (s == null) {
            ChatUtil.error("No setting '" + args[1] + "' in " + m.title);
            return;
        }
        if (args.length == 2) {
            ChatUtil.info(m.title + " " + s.name + " = " + s.asString());
            return;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("default")) s.reset();
        else if (!s.parse(value)) {
            ChatUtil.error("Invalid value '" + value + "' for " + s.name);
            return;
        }
        ChatUtil.info(m.title + " " + s.name + " = " + s.asString());
    }

    private static void reset(String[] args) {
        if (args.length < 1) {
            ChatUtil.error("Usage: .reset <module> | .reset bind <module> | .reset hud [module] | .reset all");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "bind" -> {
                if (args.length < 2) {
                    ChatUtil.error("Usage: .reset bind <module>");
                    return;
                }
                Module m = module(args[1]);
                if (m == null) return;
                m.setKey(m.getDefaultKey());
                ChatUtil.info("Reset bind of " + m.title + " (" + Keys.name(m.getKey()) + ")");
            }
            case "hud" -> {
                if (args.length >= 2) {
                    Module m = module(args[1]);
                    if (m == null) return;
                    if (!HudManager.hudModules().contains(m)) {
                        ChatUtil.error(m.title + " has no HUD element");
                        return;
                    }
                    HudManager.resetPosition(m);
                    ChatUtil.info("Reset HUD position of " + m.title);
                } else {
                    HudManager.resetAllPositions();
                    ChatUtil.info("Reset all HUD positions");
                }
            }
            case "all" -> {
                for (Module m : ModuleManager.all()) {
                    m.resetSettings();
                    m.setKey(m.getDefaultKey());
                }
                HudManager.resetAllPositions();
                ChatUtil.info("Reset all settings, binds and HUD positions");
            }
            default -> {
                Module m = module(args[0]);
                if (m == null) return;
                m.resetSettings();
                ChatUtil.info("Reset settings of " + m.title);
            }
        }
    }

    private static void modules() {
        for (Category c : Category.values()) {
            MutableText t = Text.literal(c.title + ": ").withColor(Theme.accent() & 0xFFFFFF);
            boolean first = true;
            for (Module m : ModuleManager.inCategory(c)) {
                if (!first) t.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                t.append(Text.literal(m.name).formatted(m.isEnabled() ? Formatting.WHITE : Formatting.GRAY));
                first = false;
            }
            ChatUtil.send(t);
        }
    }
}
