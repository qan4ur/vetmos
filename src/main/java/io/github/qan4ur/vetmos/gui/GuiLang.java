package io.github.qan4ur.vetmos.gui;

import static io.github.qan4ur.vetmos.util.Lang.en;

/** English translations of fixed GUI strings: {@code Lang.en("ui:key", "English")}. */
public final class GuiLang {
    private GuiLang() {}

    private static void ui(String key, String english) {
        en("ui:" + key, english);
    }

    public static void register() {
        // top bar / panels
        ui("search-modules", "Search modules…");
        ui("hud-editor", "HUD editor");
        ui("hud-editor-desc", "Drag HUD elements with the mouse, resize them with the wheel.");
        ui("help-desc", "Show the quick guide.");
        ui("lang-desc", "Interface language: Russian / English.");
        ui("palette-desc", "Accent colour, gradient and language.");
        ui("profiles-desc", "Settings profiles: switch, create, delete, share as a code.");
        ui("gui-hint", "LMB - on/off  •  RMB or ⚙ - settings  •  wheel - scroll  •  Esc - close");
        ui("panel-hint", "LMB - drag the panel, RMB - fold or unfold.");
        ui("panel-enabled", "Modules enabled");
        ui("nothing-found", "Nothing found");
        ui("open-settings", "Open the module settings (or RMB on the row).");

        // settings window
        ui("search-settings", "Search settings…");
        ui("reset-defaults-short", "Reset");
        ui("reset-defaults", "Reset to defaults");
        ui("reset-defaults-desc", "all module settings go back to their defaults (the key bind and on/off state are kept).");
        ui("copy", "Copy");
        ui("copy-desc", "Copy the module settings to the clipboard (Ctrl+C).");
        ui("paste", "Paste");
        ui("paste-desc", "Paste module settings from the clipboard (Ctrl+V).");
        ui("close-desc", "Close the window (Esc).");
        ui("module-on", "The module is on - click to turn it off.");
        ui("module-off", "The module is off - click to turn it on.");
        ui("no-settings", "This module has no settings.");
        ui("preview", "Preview");
        ui("preview-error", "Preview unavailable");
        ui("state-on", "On");
        ui("state-off", "Off");
        ui("reset-position", "Reset position");
        ui("reset-position-desc", "Move the HUD element back to its default place.");
        ui("reset-setting", "Reset to default");
        ui("color-click", "click to open the picker");
        ui("key", "Key");
        ui("key-none", "none");
        ui("key-title", "Module key");
        ui("key-hint", "Click to bind a key or mouse button that toggles the module.");
        ui("key-listening", "Press a key or mouse button. Esc, Backspace or Delete - remove the bind, LMB - cancel.");
        ui("toast-reset", "Settings reset");
        ui("toast-copied-settings", "Settings copied");
        ui("toast-pasted-settings", "Settings pasted");
        ui("toast-no-settings", "No Vetmos settings in the clipboard");
        ui("toast-other-module", "These are settings of another module");

        // palette
        ui("accent", "Accent");
        ui("language", "Language");
        ui("custom-color", "Custom colour");
        ui("gradient", "Gradient");

        // profiles
        ui("profiles", "Profiles");
        ui("profile-current", "Current profile");
        ui("profile-switch", "Switch to this profile");
        ui("delete-profile", "Delete profile");
        ui("delete-confirm", "Delete");
        ui("yes", "Yes");
        ui("no", "No");
        ui("new-profile", "New profile (copy of current)");
        ui("profile-name", "Profile name…");
        ui("create", "Create");
        ui("export-code", "Export code");
        ui("import-code", "Import code");
        ui("export-desc", "Copy all settings and the HUD layout as a code to send to a friend.");
        ui("import-desc", "Apply a code from the clipboard to the current profile.");
        ui("toast-bad-name", "Invalid profile name");
        ui("toast-exists", "This profile already exists");
        ui("toast-profile-created", "Profile created");
        ui("toast-deleted", "Profile deleted");
        ui("toast-switched", "Profile");
        ui("toast-code-copied", "Code copied");
        ui("toast-code-error", "Could not create the code");
        ui("toast-code-imported", "Settings from the code applied");
        ui("toast-code-invalid", "No Vetmos code in the clipboard");

        // welcome guide
        ui("welcome1-title", "Welcome to Vetmos");
        ui("welcome1-body", "The menu opens with Right Ctrl (you can change it in Minecraft's controls). Pick the interface language right here, top right.");
        ui("welcome2-title", "Modules");
        ui("welcome2-body", "LMB on a module turns it on and off. RMB or the ⚙ icon opens the settings window: tabs on the left, a description under every setting, ↺ resets a value, a preview on the right.");
        ui("welcome3-title", "HUD editor");
        ui("welcome3-body", "The \"HUD editor\" button at the top: drag elements with the mouse, resize them with the wheel, RMB opens an element's settings.");
        ui("welcome4-title", "Style and profiles");
        ui("welcome4-body", "The palette circle changes the accent colour and gradient. Profiles keep several sets of settings and can be shared as a code. \"?\" shows this guide again.");
        ui("back", "Back");
        ui("skip", "Skip");
        ui("next", "Next");
        ui("start", "Start");

        // HUD editor
        ui("hud-hint", "LMB - move, wheel - size, RMB - settings, Esc - exit");
        ui("hud-hint-shift", "Shift - no snapping");
        ui("hud-elements", "HUD elements");
        ui("hud-elements-desc", "Click an element to turn it on or off. LMB on the header - drag, RMB - fold.");
        ui("hud-side-row", "(LMB - on/off, RMB - settings)");
        ui("reset-positions", "Reset positions");
        ui("reset-positions-desc", "Move all HUD elements back to their default places.");

        // macros editor
        ui("macros-tab", "Macros");
        ui("macro-add", "Add macro");
        ui("macro-empty", "A macro sends text when you press a key. Three kinds of text:");
        ui("macro-empty-chat", "Hi all! - a chat message");
        ui("macro-empty-cmd", "/spawn - a server command");
        ui("macro-empty-vetmos", ".toggle zoom - a Vetmos command");
        ui("macro-text-ph", "Hi! or /spawn or .toggle zoom");
        ui("macro-name-ph", "Name (optional)");
        ui("macro-on", "The macro is on - click to turn it off.");
        ui("macro-off", "The macro is off - click to turn it on.");
        ui("macro-test", "test");
        ui("macro-test-desc", "Send the macro now (the cooldown applies).");
        ui("macro-test-noworld", "Join a world to test the macro");
        ui("macro-sent", "Macro sent");
        ui("macro-on-cooldown", "The macro is on cooldown");
        ui("macro-delete", "Delete macro");
        ui("macro-delete-confirm", "Delete this macro?");
        ui("macro-kind-chat", "chat");
        ui("macro-kind-cmd", "command");
        ui("macro-cooldown-label", "Cooldown");
        ui("macro-cooldown-desc", "Pause between uses (0-60 s). Click the number to type an exact value; Shift + wheel - 0.1 steps.");
        ui("seconds-short", "s");
        ui("macro-warn-empty", "Empty text - the macro won't send anything.");
        ui("macro-warn-nokey", "No key bound.");
        ui("macro-warn-dup", "Another macro uses this key - both will fire.");
        ui("macro-warn-module", "This key also toggles the module");
        ui("macro-howto-1", "1. Click \"+ Add macro\".");
        ui("macro-howto-2", "2. Bind a key or mouse button.");
        ui("macro-howto-3", "3. Type the text: a message, a /server command or a .Vetmos command.");
        ui("macro-howto-4", "4. Press the key in game (menu closed). ▶ test sends it right away.");

        // GUI-side hooks (welcome guide on the first world join). Registered here because this is the GUI's only
        // entry point called at startup.
    }
}
