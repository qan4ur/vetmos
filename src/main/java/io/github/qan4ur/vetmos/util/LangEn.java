package io.github.qan4ur.vetmos.util;

/** English translation table (see {@link Lang} for the key format). */
final class LangEn {
    private LangEn() {}

    /** Module title + description. */
    private static void m(String module, String title, String desc) {
        Lang.en(module, title);
        Lang.en(module + "#desc", desc);
    }

    /** Setting title + description. */
    private static void s(String module, String setting, String title, String desc) {
        Lang.en(module + "." + setting, title);
        Lang.en(module + "." + setting + "#desc", desc);
    }

    /** The standard HUD "scale" setting. */
    private static void scale(String module) {
        s(module, "scale", "Scale", "Size of the element on screen (also the mouse wheel in the HUD editor).");
    }

    static void register() {
        categories();
        groups();
        enums();
        chat();
        combat();
        hud();
        render();
        world();
        client();
    }

    // ---------------------------------------------------------------- categories / groups

    private static void categories() {
        Lang.en("category:COMBAT", "Combat");
        Lang.en("category:HUD", "HUD");
        Lang.en("category:RENDER", "Render");
        Lang.en("category:WORLD", "World");
        Lang.en("category:CLIENT", "Client");
    }

    private static void groups() {
        Lang.en("group:Основное", "General");
        Lang.en("group:Цвета", "Colors");
        Lang.en("group:Криты", "Crits");
        Lang.en("group:Молния", "Lightning");
        Lang.en("group:Частицы", "Particles");
        Lang.en("group:Звук", "Sound");
        Lang.en("group:Рендер", "Render");
        Lang.en("group:HUD", "HUD");
        Lang.en("group:Предметы", "Items");
        Lang.en("group:Стиль", "Style");
        Lang.en("group:Элементы", "Elements");
        Lang.en("group:Своя форма", "Custom Shape");
        Lang.en("group:Динамика", "Dynamics");
        Lang.en("group:Индикатор атаки", "Attack Indicator");
        Lang.en("group:Основная рука", "Main Hand");
        Lang.en("group:Вторая рука", "Off Hand");
        Lang.en("group:Небо", "Sky");
        Lang.en("group:Туман", "Fog");
        Lang.en("group:Время", "Time");
        Lang.en("group:Акцент", "Accent");
        Lang.en("group:Анимация", "Animation");
    }

    // ---------------------------------------------------------------- enum values

    private static void enums() {
        // HitParticles.Kind
        Lang.en("enum:Kind.DUST", "Dust");
        Lang.en("enum:Kind.CRIT", "Crit");
        Lang.en("enum:Kind.ENCHANTED_HIT", "Enchanted Hit");
        Lang.en("enum:Kind.HEART", "Heart");
        Lang.en("enum:Kind.FLAME", "Flame");
        Lang.en("enum:Kind.SOUL_FIRE", "Soul Fire");
        // KillEffect.KillSound
        Lang.en("enum:KillSound.THUNDER", "Thunder");
        Lang.en("enum:KillSound.LEVEL_UP", "Level Up");
        Lang.en("enum:KillSound.ORB", "XP Orb");
        Lang.en("enum:KillSound.ANVIL", "Anvil");
        Lang.en("enum:KillSound.BELL", "Bell");
        Lang.en("enum:KillSound.TOTEM", "Totem");
        Lang.en("enum:KillSound.WITHER", "Wither Spawn");
        Lang.en("enum:KillSound.DRAGON", "Dragon Growl");
        Lang.en("enum:KillSound.BLAZE", "Blaze Death");
        // ArmorHud.Layout / CooldownsHud.Layout
        Lang.en("enum:Layout.HORIZONTAL", "Row");
        Lang.en("enum:Layout.VERTICAL", "Column");
        // ArmorHud.Display
        Lang.en("enum:Display.PERCENT", "Percent");
        Lang.en("enum:Display.BAR", "Bar");
        Lang.en("enum:Display.BOTH", "Both");
        // ComboCounter.Label (the HUD text itself stays as is)
        Lang.en("enum:Label.COMBO_RU", "КОМБО (Russian)");
        Lang.en("enum:Label.COMBO_EN", "COMBO");
        Lang.en("enum:Label.HITS", "УДАРЫ (Hits)");
        Lang.en("enum:Label.NONE", "No Label");
        // CooldownsHud.Style
        Lang.en("enum:Style.RADIAL", "Radial");
        Lang.en("enum:Style.SWEEP", "Sweep");
        Lang.en("enum:Style.NONE", "None");
        // CrosshairHpIndicator.DisplayMode
        Lang.en("enum:DisplayMode.NUMBER", "Number");
        Lang.en("enum:DisplayMode.HEARTS", "Hearts");
        // LowHpWarning.PulseMode
        Lang.en("enum:PulseMode.SINE", "Smooth");
        Lang.en("enum:PulseMode.HEARTBEAT", "Heartbeat");
        Lang.en("enum:PulseMode.STATIC", "No Pulse");
        // PotionHud.Sort
        Lang.en("enum:Sort.DURATION", "By Duration");
        Lang.en("enum:Sort.NAME", "By Name");
        Lang.en("enum:Sort.HARMFUL", "Harmful First");
        // CustomCrosshair.Style
        Lang.en("enum:Style.CROSS", "Cross");
        Lang.en("enum:Style.DOT", "Dot");
        Lang.en("enum:Style.CIRCLE", "Circle");
        Lang.en("enum:Style.CROSS_DOT", "Cross + Dot");
        Lang.en("enum:Style.CUSTOM", "Custom Shape");
        // CustomCrosshair.IndicatorMode
        Lang.en("enum:IndicatorMode.FOLLOW_VANILLA", "Follow Game Settings");
        Lang.en("enum:IndicatorMode.ALWAYS", "Always");
        Lang.en("enum:IndicatorMode.OFF", "Off");
        // SwingAnimations.Preset
        Lang.en("enum:Preset.CUSTOM", "Custom");
        Lang.en("enum:Preset.CLASSIC_17", "Classic 1.7");
        // CustomSkyFog.FogMode
        Lang.en("enum:FogMode.VANILLA", "Vanilla");
        Lang.en("enum:FogMode.MULTIPLIER", "Multiplier");
        Lang.en("enum:FogMode.DISABLED", "No Fog");
        // WorldBorder.Mode
        Lang.en("enum:Mode.HIDE", "Hide");
        Lang.en("enum:Mode.STATIC", "Static");
        // InterfaceModule.Language keeps its native names ("Русский" / "English").
    }

    // ---------------------------------------------------------------- chat / commands

    private static void chat() {
        Lang.en("ui:cmd-on", "ON");
        Lang.en("ui:cmd-off", "OFF");
        Lang.en("ui:cmd-unknown", "Unknown command '%s'. Type %shelp");
        Lang.en("ui:cmd-failed", "Command failed: %s");
        Lang.en("ui:cmd-no-module", "No module named '%s'. Type %smodules");
        Lang.en("ui:cmd-commands", "Commands:");
        Lang.en("ui:cmd-help-toggle", "turn a module on/off");
        Lang.en("ui:cmd-help-bind", "bind a key");
        Lang.en("ui:cmd-help-binds", "list binds");
        Lang.en("ui:cmd-help-settings", "list settings");
        Lang.en("ui:cmd-help-set", "change a setting");
        Lang.en("ui:cmd-help-reset", "reset a module's settings to defaults");
        Lang.en("ui:cmd-help-reset-other", "reset a bind, HUD positions or everything");
        Lang.en("ui:cmd-help-modules", "list modules");
        Lang.en("ui:cmd-help-profile", "settings profiles");
        Lang.en("ui:cmd-help-config", "share settings with a code (clipboard)");
        Lang.en("ui:cmd-help-gui", "open ClickGUI / HUD editor");
        Lang.en("ui:cmd-arg-module", "<module>");
        Lang.en("ui:cmd-arg-key", "<key|mouse3|none>");
        Lang.en("ui:cmd-arg-setting", "<setting>");
        Lang.en("ui:cmd-arg-value", "<value>");
        Lang.en("ui:cmd-arg-name", "<name>");
        Lang.en("ui:cmd-arg-code", "[code]");
        Lang.en("ui:cmd-usage", "Usage: %s");
        Lang.en("ui:cmd-unknown-key", "Unknown key '%s'");
        Lang.en("ui:cmd-bound", "%s bound to %s");
        Lang.en("ui:cmd-no-binds", "No binds.");
        Lang.en("ui:cmd-settings-of", "%s settings:");
        Lang.en("ui:cmd-no-setting", "No setting '%s' in %s");
        Lang.en("ui:cmd-invalid-value", "Invalid value '%s' for %s");
        Lang.en("ui:cmd-reset-bind", "Reset bind of %s (%s)");
        Lang.en("ui:cmd-no-hud", "%s has no HUD element");
        Lang.en("ui:cmd-reset-hud", "Reset HUD position of %s");
        Lang.en("ui:cmd-reset-hud-all", "Reset all HUD positions");
        Lang.en("ui:cmd-reset-all", "Reset all settings, binds and HUD positions");
        Lang.en("ui:cmd-reset-settings", "Reset settings of %s");
        Lang.en("ui:cmd-profiles", "Profiles: ");
        Lang.en("ui:cmd-profile-create-failed", "Couldn't create the profile (name taken or invalid)");
        Lang.en("ui:cmd-profile-created", "Profile '%s' created (copy of the current one)");
        Lang.en("ui:cmd-profile-delete-failed", "Couldn't delete the profile (the current one can't be deleted)");
        Lang.en("ui:cmd-profile-deleted", "Profile '%s' deleted");
        Lang.en("ui:cmd-profile-switched", "Profile: %s");
        Lang.en("ui:cmd-profile-invalid", "Invalid profile name");
        Lang.en("ui:cmd-config-exported", "Settings code copied to the clipboard (%s characters). Send it to a friend.");
        Lang.en("ui:cmd-config-imported", "Settings imported into profile '%s'");
        Lang.en("ui:cmd-config-invalid", "Invalid settings code");
    }

    // ---------------------------------------------------------------- combat

    private static void combat() {
        m("hit-color", "Hit Color", "Replaces the red damage tint on entities with your own color.");
        s("hit-color", "hit-color", "Hit Color", "Tint of entities taking damage. Alpha sets the strength.");

        m("hit-particles", "Hit Particles", "Custom particles on the target when you hit it (only on your screen).");
        s("hit-particles", "particle", "Particle", "Particle type.");
        s("hit-particles", "normal-hits", "Normal Hits", "Particles on normal (non-critical) hits.");
        s("hit-particles", "amount", "Amount", "Particles per normal hit.");
        s("hit-particles", "spread", "Spread", "How far from the target's hitbox the particles appear.");
        s("hit-particles", "speed", "Speed", "Initial particle speed.");
        s("hit-particles", "size", "Size", "Size of dust particles.");
        s("hit-particles", "crits", "Crits", "Particles on critical hits.");
        s("hit-particles", "crit-amount", "Crit Amount", "Particles per critical hit.");
        s("hit-particles", "crit-particle", "Crit Particle", "Particle type on critical hits.");
        s("hit-particles", "color", "Color", "Dust color on normal hits.");
        s("hit-particles", "crit-color", "Crit Color", "Dust color on critical hits.");

        m("kill-effect", "Kill Effect", "Lightning, particles and a sound when someone you hit dies.");
        s("kill-effect", "players-only", "Players Only", "Trigger only on players (otherwise on any living entity).");
        s("kill-effect", "attack-window", "Attack Window", "How many seconds after your hit a death counts as your kill.");
        s("kill-effect", "lightning", "Lightning", "Strikes the victim with cosmetic lightning (only you see it).");
        s("kill-effect", "particles", "Particles", "Rings of dust particles around the victim.");
        s("kill-effect", "particle-color", "Particle Color", "Dust color.");
        s("kill-effect", "particles-per-ring", "Particles per Ring", "How many particles in each ring.");
        s("kill-effect", "rings", "Rings", "Number of rings along the victim's height.");
        s("kill-effect", "radius", "Radius", "Starting radius of the rings.");
        s("kill-effect", "burst-speed", "Burst Speed", "How fast the particles fly outward.");
        s("kill-effect", "particle-size", "Particle Size", "Size of dust particles.");
        s("kill-effect", "sound", "Sound", "A sound only you can hear.");
        s("kill-effect", "sound-type", "Kill Sound", "Which sound to play.");
        s("kill-effect", "volume", "Volume", "Sound volume.");
        s("kill-effect", "pitch", "Pitch", "Sound pitch.");

        m("totem-pop-counter", "Totem Pop Counter", "Counts players' totem pops and shows them above their heads.");
        s("totem-pop-counter", "ignore-self", "Ignore Self", "Don't count your own pops.");
        s("totem-pop-counter", "chat-message", "Chat Message", "Post in chat when someone pops a totem.");
        s("totem-pop-counter", "death-message", "Death Message", "Post in chat when a player dies after popping.");
        s("totem-pop-counter", "above-head", "Above Head", "Show the pop count above players' heads.");
        s("totem-pop-counter", "totem-icon", "Totem Icon", "A small totem icon next to the number.");
        s("totem-pop-counter", "float-up", "Floating -1", "On every pop a \"-1\" floats up and fades.");
        s("totem-pop-counter", "scale", "Scale", "Badge size.");
        s("totem-pop-counter", "height", "Height", "Height above the head (blocks). Raise it if it covers the name tag.");
        s("totem-pop-counter", "range", "Range", "Maximum distance at which badges are drawn.");
        s("totem-pop-counter", "background", "Background", "Badge background.");
        s("totem-pop-counter", "accent", "Accent", "Color of the number and the bottom line.");
        s("totem-pop-counter", "pop-flash", "Flash", "Flash color right after a pop.");
    }

    // ---------------------------------------------------------------- hud

    private static void hud() {
        m("armor-hud", "Armor HUD", "Shows your armor and held items with durability.");
        scale("armor-hud");
        s("armor-hud", "layout", "Layout", "Items in a row or a column.");
        s("armor-hud", "durability", "Durability", "How to show durability.");
        s("armor-hud", "main-hand", "Main Hand", "Show the item in your main hand.");
        s("armor-hud", "off-hand", "Off Hand", "Show the item in your off hand.");
        s("armor-hud", "hide-empty", "Hide Empty", "Don't draw empty slots.");
        s("armor-hud", "warn-threshold", "Warning Threshold", "Items below this durability % blink red.");
        s("armor-hud", "blink", "Blink", "Pulse items with low durability.");
        s("armor-hud", "blink-speed", "Blink Speed", "Flashes per second.");
        s("armor-hud", "background", "Background", "Draw a panel under the HUD.");
        s("armor-hud", "text-shadow", "Text Shadow", "Draw text with a shadow.");
        s("armor-hud", "background-color", "Background", "Panel background color.");
        s("armor-hud", "accent", "Accent", "Top line and slot frames.");
        s("armor-hud", "full-durability", "Full Durability", "Text and bar color at full durability.");
        s("armor-hud", "low-durability", "Zero Durability", "Text and bar color at zero durability.");
        s("armor-hud", "warning", "Warning", "Blink color of items below the threshold.");
        s("armor-hud", "text", "Text", "Item count color.");

        m("combo-counter", "Combo Counter", "Counts your hits in a row without missing or taking damage.");
        scale("combo-counter");
        s("combo-counter", "players-only", "Players Only", "Count only hits on players.");
        s("combo-counter", "timeout", "Reset After", "Seconds without a hit after which the combo resets.");
        s("combo-counter", "reset-on-hurt", "Reset on Damage", "Reset the combo when you take damage.");
        s("combo-counter", "min-combo", "Min Combo", "Show the counter starting from this many hits.");
        s("combo-counter", "label", "Label", "Text after the number.");
        s("combo-counter", "background", "Background", "Draws a panel under the counter.");
        s("combo-counter", "pop-strength", "Pop Strength", "How much the number grows on a new hit.");
        s("combo-counter", "preview-in-chat", "Preview in Chat", "Shows an example while chat is open.");
        s("combo-counter", "show-in-screens", "Show in Menus", "Draw even when a menu is open (except chat).");
        s("combo-counter", "number", "Number", "Combo number color.");
        s("combo-counter", "flash", "Flash", "Number flash color on a new hit.");
        s("combo-counter", "outline", "Outline", "Panel outline.");
        s("combo-counter", "glow", "Glow", "Glow along the top edge of the panel.");
        s("combo-counter", "shadow", "Shadow", "Soft shadow under the panel.");

        m("cooldowns-hud", "Cooldowns", "Shows cooldowns of pearls, chorus fruit, totems, shields and more.");
        scale("cooldowns-hud");
        s("cooldowns-hud", "layout", "Layout", "Cooldowns in a row or a column.");
        s("cooldowns-hud", "overlay", "Overlay", "Progress indicator over the icon.");
        s("cooldowns-hud", "bar", "Bar", "Progress bar under each item.");
        s("cooldowns-hud", "seconds", "Seconds", "Show the approximate time left.");
        s("cooldowns-hud", "background", "Background", "Draw a panel under the HUD.");
        s("cooldowns-hud", "text-shadow", "Text Shadow", "Draw text with a shadow.");
        s("cooldowns-hud", "ender-pearl", "Ender Pearl", "Show the ender pearl cooldown.");
        s("cooldowns-hud", "chorus-fruit", "Chorus Fruit", "Show the chorus fruit cooldown.");
        s("cooldowns-hud", "totem", "Totem of Undying", "Show the totem cooldown (if the server sets one).");
        s("cooldowns-hud", "shield", "Shield", "Show the shield cooldown after an axe hit.");
        s("cooldowns-hud", "wind-charge", "Wind Charge", "Show the wind charge cooldown.");
        s("cooldowns-hud", "background-color", "Background", "Panel background color.");
        s("cooldowns-hud", "accent", "Accent", "Top line and progress bar.");
        s("cooldowns-hud", "overlay-color", "Overlay", "Color of the remaining cooldown on the icon.");
        s("cooldowns-hud", "text", "Text", "Seconds color.");

        m("crosshair-hp-indicator", "Crosshair HP", "Shows your health and absorption next to the crosshair.");
        scale("crosshair-hp-indicator");
        s("crosshair-hp-indicator", "mode", "Mode", "Show health as a number or as hearts.");
        s("crosshair-hp-indicator", "absorption", "Absorption", "Shows absorption under health (only when you have it).");
        s("crosshair-hp-indicator", "low-health-color", "Low HP Color", "Colors the number when health is at or below the threshold.");
        s("crosshair-hp-indicator", "low-health-threshold", "Low HP Threshold", "Health at or below which the number turns red.");
        s("crosshair-hp-indicator", "text-shadow", "Text Shadow", "Draws text with a shadow.");
        s("crosshair-hp-indicator", "health-color", "Health", "Health number color.");
        s("crosshair-hp-indicator", "absorption-color", "Absorption", "Absorption number color.");

        m("keystrokes", "Keystrokes", "Shows movement keys, mouse buttons and CPS.");
        scale("keystrokes");
        s("keystrokes", "movement-keys", "Movement Keys", "Shows W A S D.");
        s("keystrokes", "mouse-buttons", "Mouse Buttons", "Shows the left and right mouse buttons.");
        s("keystrokes", "cps", "CPS", "Shows clicks per second under the mouse buttons.");
        s("keystrokes", "space-bar", "Space Bar", "Shows the jump key.");
        s("keystrokes", "sneak-key", "Sneak Key", "Shows the sneak key next to the space bar.");
        s("keystrokes", "bound-key-names", "Real Key Names", "Label keys with what they're bound to instead of W A S D.");
        s("keystrokes", "key-size", "Key Size", "Size of a single key.");
        s("keystrokes", "gap", "Gap", "Space between keys.");
        s("keystrokes", "fade-speed", "Fade Speed", "How fast a key fades after release.");
        s("keystrokes", "show-in-screens", "Show in Menus", "Draw even when a menu is open (except chat).");
        s("keystrokes", "key", "Key", "Background of a released key.");
        s("keystrokes", "pressed", "Pressed Key", "Background of a pressed key.");
        s("keystrokes", "outline", "Outline", "Key outline.");
        s("keystrokes", "glow", "Glow", "Glow around pressed keys.");
        s("keystrokes", "text", "Text", "Label color of a released key.");
        s("keystrokes", "pressed-text", "Pressed Text", "Label color of a pressed key.");
        s("keystrokes", "shadow", "Shadow", "Soft shadow under each key.");

        m("low-hp-warning", "Low HP Warning", "A pulsing red vignette around the screen edges when your health is low.");
        s("low-hp-warning", "threshold", "Threshold", "Health (in half-hearts) below which the warning shows. 8 = 4 hearts.");
        s("low-hp-warning", "include-absorption", "Include Absorption", "Count golden absorption hearts as health.");
        s("low-hp-warning", "scale-with-health", "Scale with Health", "The less health, the brighter the vignette.");
        s("low-hp-warning", "ignore-creative", "Not in Creative", "Don't show in creative and spectator mode.");
        s("low-hp-warning", "show-in-screens", "Show in Menus", "Draw even when the inventory or another menu is open (except chat).");
        s("low-hp-warning", "pulse-mode", "Pulse", "How the vignette pulses.");
        s("low-hp-warning", "pulse-speed", "Pulse Speed", "Pulses per second.");
        s("low-hp-warning", "faster-when-lower", "Faster at Low HP", "The pulse speeds up as your health drops.");
        s("low-hp-warning", "min-pulse", "Min Pulse", "Brightness between pulse beats (0-1).");
        s("low-hp-warning", "max-alpha", "Max Opacity", "Opacity at the very edge of the screen.");
        s("low-hp-warning", "thickness", "Width", "How far the vignette reaches into the screen (GUI pixels).");
        s("low-hp-warning", "color", "Color", "Vignette color (opacity is set by \"Max Opacity\").");
        s("low-hp-warning", "screen-tint", "Screen Tint", "Slightly tints the whole screen during a pulse.");
        s("low-hp-warning", "tint-alpha", "Tint Strength", "Maximum opacity of the screen tint.");

        m("potion-hud", "Potion HUD", "A list of active effects with timers.");
        scale("potion-hud");
        s("potion-hud", "hide-vanilla-icons", "Hide Vanilla Icons", "Hide the standard effect icons in the top right corner.");
        s("potion-hud", "icons", "Icons", "Draw the effect icon.");
        s("potion-hud", "level", "Level", "Show the effect level in Roman numerals.");
        s("potion-hud", "show-ambient", "Ambient Effects", "Show effects from beacons and conduits.");
        s("potion-hud", "respect-hidden", "Respect Hidden", "Skip effects whose icon the server hid.");
        s("potion-hud", "sort", "Sort", "Order of effects in the list.");
        s("potion-hud", "blink-below", "Blink Below", "Blink when fewer than this many seconds are left.");
        s("potion-hud", "effect-color-bar", "Color Bar", "A thin side bar in the effect's color.");
        s("potion-hud", "text-shadow", "Text Shadow", "Draw text with a shadow.");
        s("potion-hud", "background", "Background", "Panel background color.");
        s("potion-hud", "accent", "Accent", "Top line color.");
        s("potion-hud", "name", "Name", "Effect name color.");
        s("potion-hud", "harmful-name", "Harmful Effect", "Name color of harmful effects.");
        s("potion-hud", "timer", "Timer", "Timer color.");
        s("potion-hud", "blink", "Blink", "Blink color of expiring effects.");

        m("target-hud", "Target HUD", "A panel with the health and gear of the player you're fighting.");
        scale("target-hud");
        s("target-hud", "players-only", "Players Only", "Track only players. Otherwise any entity you hit is shown.");
        s("target-hud", "crosshair-target", "Crosshair Target", "Also use the entity under your crosshair.");
        s("target-hud", "timeout", "Reset After", "Seconds without combat after which the target disappears.");
        s("target-hud", "range", "Range", "The target disappears when it's farther than this.");
        s("target-hud", "preview-in-chat", "Preview in Chat", "Shows yourself while chat is open.");
        s("target-hud", "show-in-screens", "Show in Menus", "Draw even when a menu is open (inventory etc., except chat).");
        s("target-hud", "animation-speed", "Animation Speed", "Speed of the health bar and fade-in.");
        s("target-hud", "head", "Head", "Shows the player's skin face.");
        s("target-hud", "name", "Name", "Shows the target's name.");
        s("target-hud", "health-bar", "Health Bar", "Animated health bar.");
        s("target-hud", "damage-trail", "Damage Trail", "A lagging bar showing recently lost health.");
        s("target-hud", "health-number", "Health Number", "Shows health as a number.");
        s("target-hud", "absorption", "Absorption", "Shows absorption on the bar and in the number.");
        s("target-hud", "armor", "Armor", "Shows the target's worn armor.");
        s("target-hud", "held-item", "Held Item", "Shows the main-hand item next to the armor.");
        s("target-hud", "durability", "Durability", "Durability bars on item icons.");
        s("target-hud", "distance", "Distance", "Shows the distance to the target.");
        s("target-hud", "background", "Background", "Panel background.");
        s("target-hud", "outline", "Outline", "Panel outline.");
        s("target-hud", "glow", "Glow", "Blood moon glow along the top edge.");
        s("target-hud", "shadow", "Shadow", "Soft shadow under the panel.");
        s("target-hud", "bar-start", "Bar: Start", "Left color of the health bar.");
        s("target-hud", "bar-end", "Bar: End", "Right color of the health bar.");
        s("target-hud", "trail", "Damage Trail", "Damage trail color.");
        s("target-hud", "bar-background", "Bar Background", "Empty part of the health bar.");
        s("target-hud", "text", "Text", "Main text color.");
        s("target-hud", "secondary-text", "Secondary Text", "Color of the health number and distance.");
        s("target-hud", "accent", "Accent", "Head frame and hit flash.");
    }

    // ---------------------------------------------------------------- render

    private static void render() {
        m("custom-crosshair", "Custom Crosshair", "Replaces the vanilla crosshair and attack indicator with customizable ones, without color inversion.");
        scale("custom-crosshair");
        s("custom-crosshair", "resource-pack-textures", "Resource Pack Textures", "Draws the crosshair and attack indicator from the current resource pack in their real colors instead of the shapes below.");
        s("custom-crosshair", "style", "Style", "Crosshair shape.");
        s("custom-crosshair", "size", "Size", "Length of the cross arms / circle radius (GUI pixels).");
        s("custom-crosshair", "gap", "Gap", "Empty space between the center and the arms.");
        s("custom-crosshair", "thickness", "Thickness", "Line thickness.");
        s("custom-crosshair", "dot-size", "Dot Size", "Size of the center dot.");
        s("custom-crosshair", "outline", "Outline", "Draws an outline around the crosshair.");
        s("custom-crosshair", "outline-width", "Outline Width", "Outline width in GUI pixels.");
        s("custom-crosshair", "opacity", "Opacity", "Opacity of the crosshair and attack indicator.");
        s("custom-crosshair", "top-length", "Top Arm", "Length of the top arm (0 = none, makes a T shape).");
        s("custom-crosshair", "bottom-length", "Bottom Arm", "Length of the bottom arm (0 = none).");
        s("custom-crosshair", "left-length", "Left Arm", "Length of the left arm (0 = none).");
        s("custom-crosshair", "right-length", "Right Arm", "Length of the right arm (0 = none).");
        s("custom-crosshair", "center-dot", "Center Dot", "Draws a dot in the center.");
        s("custom-crosshair", "attack-cooldown-spread", "Cooldown Spread", "The crosshair spreads apart while your attack recharges.");
        s("custom-crosshair", "spread-amount", "Spread Amount", "How far the crosshair spreads at zero attack charge.");
        s("custom-crosshair", "target-color", "Target Color", "Changes the crosshair color when you aim at a target.");
        s("custom-crosshair", "players-only", "Players Only", "Target color only for players (otherwise any living entity).");
        s("custom-crosshair", "mode", "Mode", "When to show the attack cooldown bar under the crosshair.");
        s("custom-crosshair", "offset", "Offset", "Distance from the crosshair center to the top of the bar. 10 = vanilla.");
        s("custom-crosshair", "width", "Width", "Bar width without the outline.");
        s("custom-crosshair", "height", "Height", "Bar height without the outline.");
        s("custom-crosshair", "flash-when-full", "Flash When Full", "The bar briefly flashes when the attack is fully charged.");
        s("custom-crosshair", "color", "Color", "Crosshair color.");
        s("custom-crosshair", "outline-color", "Outline Color", "Crosshair outline color.");
        s("custom-crosshair", "bar-color", "Bar", "Bar fill color while charging.");
        s("custom-crosshair", "bar-full-color", "Bar (Charged)", "Bar color at full charge.");
        s("custom-crosshair", "bar-flash-color", "Flash", "Bar flash color at full charge.");
        s("custom-crosshair", "bar-background", "Bar Background", "Color of the empty part of the bar.");
        s("custom-crosshair", "bar-outline-color", "Bar Outline", "Bar outline color.");

        m("glint-color", "Glint Color", "Recolors the enchantment glint on items and armor.");
        s("glint-color", "items", "Items", "Recolor the glint on items (in hand, on the ground, in menus).");
        s("glint-color", "armor-and-entities", "Armor & Entities", "Recolor the glint on worn armor, elytra, tridents and shields.");
        s("glint-color", "brightness", "Brightness", "Intensity of the colored glint.");
        s("glint-color", "item-color", "Item Color", "Glint color on items.");
        s("glint-color", "armor-uses-item-color", "Armor Like Items", "Use the item color for armor too.");
        s("glint-color", "armor-color", "Armor Color", "Glint color on armor and entities.");

        m("swing-animations", "Swing Animations", "First-person item position, size and swing (1.7/1.8 style).");
        s("swing-animations", "preset", "Preset", "\"Classic 1.7\" enables the old animation set: a 6-tick swing, no bobbing on swap and no dip after a hit.");
        s("swing-animations", "swing-duration", "Swing Duration", "First-person swing length in ticks (vanilla 6, more = slower).");
        s("swing-animations", "ignore-haste-fatigue", "Ignore Haste/Fatigue", "Use the swing duration even if Haste or Mining Fatigue change it.");
        s("swing-animations", "no-swap-animation", "No Swap Animation", "Removes the item lowering/raising when switching slots (like 1.7/1.8).");
        s("swing-animations", "no-cooldown-dip", "No Cooldown Dip", "Removes the item dip after every hit caused by the attack cooldown (like 1.8).");
        s("swing-animations", "affect-empty-hand", "Empty Hand", "Apply the main-hand position, rotation and size to the bare hand too.");
        hand("main", "main hand");
        hand("off", "off hand");
    }

    /** Position / rotation / scale settings of one hand in Swing Animations. */
    private static void hand(String prefix, String hand) {
        String mod = "swing-animations";
        s(mod, prefix + "-scale", "Scale", "Size of the item in your " + hand + ".");
        for (String axis : new String[] {"x", "y", "z"}) {
            String a = axis.toUpperCase(java.util.Locale.ROOT);
            s(mod, prefix + "-offset-" + axis, "Offset " + a, "Offset of the item in your " + hand + " along " + a + ".");
            s(mod, prefix + "-rotation-" + axis, "Rotation " + a, "Rotation of the item in your " + hand + " around " + a + " (degrees).");
        }
    }

    // ---------------------------------------------------------------- world

    private static void world() {
        m("block-outline", "Block Outline", "Custom color, width and fill for the selected block outline.");
        s("block-outline", "line-width", "Line Width", "Outline width in roughly pixels. 1 = normal thin lines.");
        s("block-outline", "fill", "Fill", "Fill the selected block with a translucent color.");
        s("block-outline", "through-walls", "Through Walls", "Draw the outline on top of everything instead of depth-tested like vanilla.");
        s("block-outline", "line-color", "Line Color", "Outline color.");
        s("block-outline", "fill-color", "Fill Color", "Fill color.");

        m("custom-sky-fog", "Sky & Fog", "Custom sky and fog color, fog distance and client-side time of day.");
        s("custom-sky-fog", "custom-sky-color", "Custom Sky Color", "Replace the sky color.");
        s("custom-sky-fog", "sky-color", "Sky Color", "Sky color.");
        s("custom-sky-fog", "custom-fog-color", "Custom Fog Color", "Replace the fog and horizon color (in air).");
        s("custom-sky-fog", "fog-color", "Fog Color", "Fog color.");
        s("custom-sky-fog", "fog-distance", "Fog Distance", "Vanilla, multiplied or no fog in air.");
        s("custom-sky-fog", "distance-multiplier", "Distance Multiplier", "Multiplier for fog start and end. Above 1 = fog farther away.");
        s("custom-sky-fog", "affect-fluids", "In Fluids", "Also change fog distance underwater, in lava and in powder snow.");
        s("custom-sky-fog", "fixed-time", "Fixed Time", "Show the world at a set time of day (only for you).");
        s("custom-sky-fog", "time", "Time of Day", "Time in ticks: 1000 = morning, 6000 = noon, 13000 = sunset, 18000 = midnight.");

        m("world-border", "World Border", "Hides the world border or draws it in a static color.");
        s("world-border", "mode", "Mode", "Hide: don't draw the border. Static: your own color without flicker or scrolling animation.");
        s("world-border", "color", "Color", "Border color (also while it grows or shrinks).");
        s("world-border", "alpha", "Opacity", "Maximum border opacity (0-255).");
        s("world-border", "fade-with-distance", "Fade with Distance", "The border fades in as you approach, like vanilla. Off = constant opacity.");
    }

    // ---------------------------------------------------------------- client

    private static void client() {
        m("interface", "Interface", "Vetmos language and accent color. The base style is black and white.");
        s("interface", "language", "Language", "Vetmos interface language. / Язык интерфейса Vetmos.");
        s("interface", "accent", "Accent Color", "Color of active modules, toggles and highlights in the menu. Near-white by default.");
        s("interface", "gradient", "Gradient", "Smooth blend of the accent into a second color (shimmers across the menu).");
        s("interface", "accent-2", "Second Color", "Second gradient color.");
        s("interface", "gradient-speed", "Gradient Speed", "Gradient shimmer speed (0 = static).");
        s("interface", "chat-accent", "Chat Prefix Color", "Color the [Vetmos] chat prefix with the accent color (otherwise white).");
    }
}
