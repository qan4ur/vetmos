package io.github.qan4ur.vetmos.util;

/** English translations for macros (module, chat messages, commands). */
final class LangEnMacros {
    private LangEnMacros() {}

    static void register() {
        Lang.en("macros", "Macros");
        Lang.en("macros#desc", "Key binds that send a chat message, a /command or a Vetmos command. Each macro has its own cooldown.");
        Lang.en("macros.cooldown-message", "Cooldown message");
        Lang.en("macros.cooldown-message#desc", "Tell in chat how long is left when a macro is pressed too early.");
        Lang.en("ui:macro-cooldown", "Macro \"%s\" is on cooldown: %.1fs");
        Lang.en("ui:cmd-help-macro-add", "macro: key -> message or /command, cooldown in seconds");
        Lang.en("ui:cmd-help-macro", "list / remove / clear macros");
        Lang.en("ui:cmd-macro-empty", "No macros. Add one: .macro add <key> <cooldown> <text>");
        Lang.en("ui:cmd-macro-add-usage", "Usage: .macro add <key> <cooldown seconds> <text or /command>");
        Lang.en("ui:cmd-macro-bad-cd", "Cooldown must be a number of seconds, e.g. 1.5");
        Lang.en("ui:cmd-macro-added", "Macro added: %s -> %s");
        Lang.en("ui:cmd-macro-removed", "Macro removed");
        Lang.en("ui:cmd-macro-remove-usage", "Usage: .macro remove <number from .macro list>");
        Lang.en("ui:cmd-macro-cleared", "All macros removed");
        Lang.en("ui:cmd-macro-usage", "Usage: .macro list | add <key> <cooldown> <text> | remove <number> | clear");
    }
}
