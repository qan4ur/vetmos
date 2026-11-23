package io.github.qan4ur.vetmos.command;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.Setting;
import io.github.qan4ur.vetmos.util.ChatUtil;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
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
                case "profile", "profiles", "p" -> profile(rest);
                case "config", "cfg" -> config(rest);
                case "macro", "macros", "m" -> macro(rest);
                case "gui" -> Vetmos.openScreenNextTick(Vetmos.createClickGui());
                case "hud" -> Vetmos.openScreenNextTick(Vetmos.createHudEditor());
                default -> ChatUtil.error(f("cmd-unknown", "Неизвестная команда '%s'. Напиши %shelp", cmd, PREFIX));
            }
        } catch (RuntimeException e) {
            ChatUtil.error(f("cmd-failed", "Ошибка команды: %s", e.getMessage()));
            Vetmos.LOG.error("Command failed: {}", message, e);
        }
        return true;
    }

    private static Module module(String name) {
        Module m = ModuleManager.get(name);
        if (m == null) ChatUtil.error(f("cmd-no-module", "Нет модуля '%s'. Напиши %smodules", name, PREFIX));
        return m;
    }

    private static void help() {
        String mod = arg("module", "<модуль>"), set = arg("setting", "<настройка>"), val = arg("value", "<значение>");
        String nm = arg("name", "<имя>");
        ChatUtil.info(Lang.ui("cmd-commands", "Команды:"));
        line(".toggle " + mod, Lang.ui("cmd-help-toggle", "включить/выключить модуль"));
        line(".bind " + mod + " " + arg("key", "<клавиша|mouse3|none>"), Lang.ui("cmd-help-bind", "назначить клавишу"));
        line(".binds", Lang.ui("cmd-help-binds", "список биндов"));
        line(".macro add <клавиша> <кд> <текст>", Lang.ui("cmd-help-macro-add", "макрос: клавиша → сообщение или /команда, кд в секундах"));
        line(".macro list | remove <номер> | clear", Lang.ui("cmd-help-macro", "список / удалить / очистить макросы"));
        line(".settings " + mod, Lang.ui("cmd-help-settings", "список настроек"));
        line(".settings " + mod + " " + set + " " + val, Lang.ui("cmd-help-set", "изменить настройку"));
        line(".reset " + mod, Lang.ui("cmd-help-reset", "сбросить настройки модуля"));
        line(".reset bind " + mod + " | .reset hud [" + strip(mod) + "] | .reset all", Lang.ui("cmd-help-reset-other", "сбросить бинд, позиции HUD или всё"));
        line(".modules", Lang.ui("cmd-help-modules", "список модулей"));
        line(".profile [list|" + nm + "|create " + nm + "|delete " + nm + "]", Lang.ui("cmd-help-profile", "профили настроек"));
        line(".config export | .config import " + arg("code", "[код]"), Lang.ui("cmd-help-config", "поделиться настройками через код (буфер обмена)"));
        line(".gui / .hud", Lang.ui("cmd-help-gui", "открыть ClickGUI / редактор HUD"));
    }

    /** Localized, formatted chat string: {@code f("cmd-x", "Текст %s", arg)}. */
    private static String f(String key, String russian, Object... args) {
        return String.format(Lang.ui(key, russian), args);
    }

    private static String arg(String key, String russian) {
        return Lang.ui("cmd-arg-" + key, russian);
    }

    /** "<модуль>" -> "модуль". */
    private static String strip(String bracketed) {
        return bracketed.length() > 2 ? bracketed.substring(1, bracketed.length() - 1) : bracketed;
    }

    private static void usage(String usage) {
        ChatUtil.error(f("cmd-usage", "Использование: %s", usage));
    }

    private static void line(String usage, String description) {
        MutableText t = Text.literal(usage).withColor(Theme.accent() & 0xFFFFFF);
        if (!description.isEmpty()) t.append(Text.literal(" - " + description).formatted(Formatting.GRAY));
        ChatUtil.send(t);
    }

    private static void toggle(String[] args) {
        if (args.length < 1) {
            usage(".toggle " + arg("module", "<модуль>"));
            return;
        }
        Module m = module(args[0]);
        if (m != null) m.toggleWithFeedback();
    }

    private static void bind(String[] args) {
        if (args.length < 2) {
            usage(".bind " + arg("module", "<модуль>") + " " + arg("key", "<клавиша|mouse3|none>"));
            return;
        }
        Module m = module(args[0]);
        if (m == null) return;
        Integer key = Keys.parse(args[1]);
        if (key == null) {
            ChatUtil.error(f("cmd-unknown-key", "Неизвестная клавиша '%s'", args[1]));
            return;
        }
        m.setKey(key);
        ChatUtil.info(f("cmd-bound", "%s: клавиша %s", m.displayTitle(), Keys.name(key)));
    }

    private static void binds() {
        int n = 0;
        for (Module m : ModuleManager.all()) {
            if (m.getKey() == Keys.NONE) continue;
            ChatUtil.send(Text.literal(m.displayTitle()).formatted(Formatting.WHITE).append(Text.literal(" - " + Keys.name(m.getKey())).formatted(Formatting.GRAY)));
            n++;
        }
        if (n == 0) ChatUtil.info(Lang.ui("cmd-no-binds", "Биндов нет."));
    }

    private static void settings(String[] args) {
        if (args.length < 1) {
            usage(".settings " + arg("module", "<модуль>") + " [" + strip(arg("setting", "<настройка>")) + "] [" + strip(arg("value", "<значение>")) + "]");
            return;
        }
        Module m = module(args[0]);
        if (m == null) return;

        if (args.length == 1) {
            ChatUtil.info(f("cmd-settings-of", "Настройки %s:", m.displayTitle()));
            for (Setting<?> s : m.getSettings()) {
                ChatUtil.send(Text.literal(s.name).withColor(Theme.accent() & 0xFFFFFF)
                    .append(Text.literal(" = " + s.asString()).formatted(Formatting.WHITE))
                    .append(Text.literal("  (" + s.displayDescription() + ")").formatted(Formatting.DARK_GRAY)));
            }
            return;
        }

        Setting<?> s = m.getSetting(args[1]);
        if (s == null) {
            ChatUtil.error(f("cmd-no-setting", "Нет настройки '%s' в %s", args[1], m.displayTitle()));
            return;
        }
        if (args.length == 2) {
            ChatUtil.info(m.displayTitle() + " " + s.name + " = " + s.asString());
            return;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("default")) s.reset();
        else if (!s.parse(value)) {
            ChatUtil.error(f("cmd-invalid-value", "Неверное значение '%s' для %s", value, s.name));
            return;
        }
        ChatUtil.info(m.displayTitle() + " " + s.name + " = " + s.asString());
    }

    private static void reset(String[] args) {
        if (args.length < 1) {
            { String mod = arg("module", "<модуль>"); usage(".reset " + mod + " | .reset bind " + mod + " | .reset hud [" + strip(mod) + "] | .reset all"); }
            return;
        }
        switch (args[0].toLowerCase()) {
            case "bind" -> {
                if (args.length < 2) {
                    usage(".reset bind " + arg("module", "<модуль>"));
                    return;
                }
                Module m = module(args[1]);
                if (m == null) return;
                m.setKey(m.getDefaultKey());
                ChatUtil.info(f("cmd-reset-bind", "Бинд %s сброшен (%s)", m.displayTitle(), Keys.name(m.getKey())));
            }
            case "hud" -> {
                if (args.length >= 2) {
                    Module m = module(args[1]);
                    if (m == null) return;
                    if (!HudManager.hudModules().contains(m)) {
                        ChatUtil.error(f("cmd-no-hud", "У модуля %s нет элемента HUD", m.displayTitle()));
                        return;
                    }
                    HudManager.resetPosition(m);
                    ChatUtil.info(f("cmd-reset-hud", "Позиция HUD %s сброшена", m.displayTitle()));
                } else {
                    HudManager.resetAllPositions();
                    ChatUtil.info(Lang.ui("cmd-reset-hud-all", "Все позиции HUD сброшены"));
                }
            }
            case "all" -> {
                for (Module m : ModuleManager.all()) {
                    m.resetSettings();
                    m.setKey(m.getDefaultKey());
                }
                HudManager.resetAllPositions();
                ChatUtil.info(Lang.ui("cmd-reset-all", "Все настройки, бинды и позиции HUD сброшены"));
            }
            default -> {
                Module m = module(args[0]);
                if (m == null) return;
                m.resetSettings();
                ChatUtil.info(f("cmd-reset-settings", "Настройки %s сброшены", m.displayTitle()));
            }
        }
    }

    private static void profile(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            MutableText t = Text.literal(Lang.ui("cmd-profiles", "Профили: ")).formatted(Formatting.GRAY);
            boolean first = true;
            for (String p : io.github.qan4ur.vetmos.config.ConfigManager.listProfiles()) {
                if (!first) t.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                boolean cur = p.equalsIgnoreCase(io.github.qan4ur.vetmos.config.ConfigManager.currentProfile());
                t.append(cur ? Text.literal(p).withColor(Theme.accent() & 0xFFFFFF) : Text.literal(p).formatted(Formatting.WHITE));
                first = false;
            }
            ChatUtil.send(t);
            return;
        }
        String sub = args[0].toLowerCase();
        String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        switch (sub) {
            case "create", "new" -> {
                if (name == null || !io.github.qan4ur.vetmos.config.ConfigManager.createProfile(name, true)) ChatUtil.error(Lang.ui("cmd-profile-create-failed", "Не удалось создать профиль (имя занято или некорректно)"));
                else ChatUtil.info(f("cmd-profile-created", "Профиль '%s' создан (копия текущего)", name));
            }
            case "delete", "remove" -> {
                if (name == null || !io.github.qan4ur.vetmos.config.ConfigManager.deleteProfile(name)) ChatUtil.error(Lang.ui("cmd-profile-delete-failed", "Не удалось удалить профиль (текущий удалить нельзя)"));
                else ChatUtil.info(f("cmd-profile-deleted", "Профиль '%s' удалён", name));
            }
            default -> {
                String target = String.join(" ", args);
                if (io.github.qan4ur.vetmos.config.ConfigManager.switchProfile(target)) ChatUtil.info(f("cmd-profile-switched", "Профиль: %s", target));
                else ChatUtil.error(Lang.ui("cmd-profile-invalid", "Некорректное имя профиля"));
            }
        }
    }

    private static void config(String[] args) {
        if (args.length == 0) {
            usage(".config export | .config import " + arg("code", "[код]"));
            return;
        }
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        switch (args[0].toLowerCase()) {
            case "export" -> {
                String code = io.github.qan4ur.vetmos.config.ConfigManager.exportCode();
                mc.keyboard.setClipboard(code);
                ChatUtil.info(f("cmd-config-exported", "Код настроек скопирован в буфер обмена (%s символов). Отправь его другу.", code.length()));
            }
            case "import" -> {
                String code = args.length > 1 ? args[1] : mc.keyboard.getClipboard();
                if (io.github.qan4ur.vetmos.config.ConfigManager.importCode(code)) ChatUtil.info(f("cmd-config-imported", "Настройки импортированы в профиль '%s'", io.github.qan4ur.vetmos.config.ConfigManager.currentProfile()));
                else ChatUtil.error(Lang.ui("cmd-config-invalid", "Неверный код настроек"));
            }
            default -> usage(".config export | .config import " + arg("code", "[код]"));
        }
    }

    private static void macro(String[] args) {
        java.util.List<io.github.qan4ur.vetmos.macro.Macro> list = io.github.qan4ur.vetmos.macro.MacroManager.all();
        String sub = args.length == 0 ? "list" : args[0].toLowerCase();
        switch (sub) {
            case "list" -> {
                if (list.isEmpty()) {
                    ChatUtil.info(Lang.ui("cmd-macro-empty", "Макросов нет. Добавь: .macro add <клавиша> <кд> <текст>"));
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    io.github.qan4ur.vetmos.macro.Macro m = list.get(i);
                    ChatUtil.send(Text.literal((i + 1) + ". ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(Keys.name(m.key)).withColor(Theme.accent() & 0xFFFFFF))
                        .append(Text.literal(" → " + m.text).formatted(m.enabled ? Formatting.WHITE : Formatting.GRAY))
                        .append(Text.literal(String.format(java.util.Locale.ROOT, "  (%.1fс)", m.cooldown)).formatted(Formatting.DARK_GRAY)));
                }
            }
            case "add" -> {
                if (args.length < 4) {
                    ChatUtil.error(Lang.ui("cmd-macro-add-usage", "Использование: .macro add <клавиша> <кд в секундах> <текст или /команда>"));
                    return;
                }
                Integer key = Keys.parse(args[1]);
                if (key == null || key == Keys.NONE) {
                    ChatUtil.error(f("cmd-unknown-key", "Неизвестная клавиша '%s'", args[1]));
                    return;
                }
                double cd;
                try {
                    cd = Double.parseDouble(args[2].replace(',', '.'));
                } catch (NumberFormatException e) {
                    ChatUtil.error(Lang.ui("cmd-macro-bad-cd", "Кд должно быть числом секунд, например 1.5"));
                    return;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                io.github.qan4ur.vetmos.macro.MacroManager.add(key, text, cd);
                ChatUtil.info(f("cmd-macro-added", "Макрос добавлен: %s → %s", Keys.name(key), text));
            }
            case "remove", "delete" -> {
                try {
                    int i = Integer.parseInt(args[1]) - 1;
                    io.github.qan4ur.vetmos.macro.MacroManager.remove(list.get(i));
                    ChatUtil.info(Lang.ui("cmd-macro-removed", "Макрос удалён"));
                } catch (RuntimeException e) {
                    ChatUtil.error(Lang.ui("cmd-macro-remove-usage", "Использование: .macro remove <номер из .macro list>"));
                }
            }
            case "clear" -> {
                io.github.qan4ur.vetmos.macro.MacroManager.clear();
                ChatUtil.info(Lang.ui("cmd-macro-cleared", "Все макросы удалены"));
            }
            default -> ChatUtil.error(Lang.ui("cmd-macro-usage", "Использование: .macro list | add <клавиша> <кд> <текст> | remove <номер> | clear"));
        }
    }

    private static void modules() {
        for (Category c : Category.values()) {
            MutableText t = Text.literal(c.displayTitle() + ": ").withColor(Theme.accent() & 0xFFFFFF);
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
