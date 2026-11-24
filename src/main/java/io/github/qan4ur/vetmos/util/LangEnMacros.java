package io.github.qan4ur.vetmos.util;

/** English translations for macros (module, chat messages, commands). */
final class LangEnMacros {
    private LangEnMacros() {}

    static void register() {
        Lang.en("interface.custom-font", "Clean font");
        Lang.en("interface.custom-font#desc", "Modern font (Fira Sans) in the Vetmos menus instead of the pixel Minecraft font.");
        Lang.en("ui:coords-copy", "Click to copy the coordinates");
        Lang.en("category:INVENTORY", "Inventory");
        Lang.en("inventory-sort", "Sorting");
        Lang.en("inventory-sort#desc", "Sort your inventory and chests with a key (R by default), like Inventory Profiles Next.");
        Lang.en("inventory-sort.key", "Sort key");
        Lang.en("inventory-sort.key#desc", "Press it with an inventory or chest open - the part under the mouse gets sorted. Does nothing in the world.");
        Lang.en("inventory-sort.order", "Order");
        Lang.en("inventory-sort.order#desc", "How to order items: like the creative menu (like IPN), by name or by item ID.");
        Lang.en("inventory-sort.hotbar", "Sort hotbar");
        Lang.en("inventory-sort.hotbar#desc", "Also sort the hotbar together with the inventory (usually left alone).");
        Lang.en("inventory-sort.clicks-per-tick", "Clicks per tick");
        Lang.en("inventory-sort.clicks-per-tick#desc", "How many clicks per tick (0 = all at once, like IPN). Lower is slower but gentler for strict servers.");
        Lang.en("inventory-sort.sound", "Sound");
        Lang.en("inventory-sort.sound#desc", "A quiet click after sorting.");
        Lang.en("enum:InventorySort.Order.CREATIVE", "Creative menu");
        Lang.en("enum:InventorySort.Order.NAME", "By name");
        Lang.en("enum:InventorySort.Order.ID", "By item ID");
        Lang.en("group:Скорость", "Speed");
        Lang.en("shift-drag", "Fast moving");
        Lang.en("shift-drag#desc", "Hold Shift and LMB and sweep over slots - every item the cursor passes is moved to the other inventory.");
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
