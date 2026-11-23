package io.github.qan4ur.vetmos.util;

/** English translations for the info HUD + item counter modules (see {@link Lang} for the key format). */
final class LangEnInfo {
    private LangEnInfo() {}

    private static void s(String module, String setting, String title, String desc) {
        Lang.en(module + "." + setting, title);
        Lang.en(module + "." + setting + "#desc", desc);
    }

    /** Settings shared by every InfoHudModule. */
    private static void base(String m) {
        s(m, "background", "Background", "Draw a dark panel under the text.");
        s(m, "accent-line", "Accent line", "Thin accent-coloured line at the top of the panel.");
        s(m, "label", "Label", "Show a label before the value.");
        s(m, "compact", "Compact", "Smaller paddings and rows.");
        s(m, "text-shadow", "Text shadow", "Draw text with a shadow.");
        s(m, "scale", "Size", "Size of the element on screen (also mouse wheel in the HUD editor).");
        s(m, "background-color", "Background", "Panel colour.");
        s(m, "text", "Text", "Colour of the values.");
        s(m, "label-accent", "Accent-coloured label", "Paint labels with the interface accent colour.");
        s(m, "label-color", "Label", "Label colour (when the accent is off).");
    }

    static void register() {
        Lang.en("group:Пороги", "Thresholds");

        // HUD labels
        Lang.en("hud:fps", "FPS");
        Lang.en("hud:ping", "Ping");
        Lang.en("hud:ms", "ms");
        Lang.en("hud:overworld", "Overworld");
        Lang.en("hud:nether", "Nether");
        Lang.en("hud:biome", "Biome");
        Lang.en("hud:direction", "Facing");
        Lang.en("hud:speed", "Speed");
        Lang.en("hud:bps", "b/s");
        Lang.en("hud:kmh", "km/h");
        Lang.en("hud:time", "Time");
        Lang.en("hud:game-time", "Game");
        Lang.en("hud:day", "day");

        // FPS
        Lang.en("fps-hud", "FPS");
        Lang.en("fps-hud#desc", "Shows frames per second.");
        base("fps-hud");
        s("fps-hud", "color-by-value", "Colour by value", "Paint the number: low FPS yellow and red.");
        s("fps-hud", "low-fps", "Low FPS", "Below this value the number gets the warning colour.");

        // Ping
        Lang.en("ping-hud", "Ping");
        Lang.en("ping-hud#desc", "Shows your latency to the server from the player list.");
        base("ping-hud");
        s("ping-hud", "color-by-quality", "Colour by quality", "Green is a good ping, red is a bad one.");
        s("ping-hud", "hide-singleplayer", "Hide in singleplayer", "Don't show the ping in a singleplayer world.");

        // Coordinates
        Lang.en("coords-hud", "Coordinates");
        Lang.en("coords-hud#desc", "Shows XYZ coordinates, the other dimension's coordinates and the biome.");
        base("coords-hud");
        s("coords-hud", "style", "Style", "All coordinates on one line or each on its own.");
        s("coords-hud", "decimals", "Decimal places", "0 = block coordinates.");
        s("coords-hud", "dimension-coords", "Other dimension", "In the overworld show nether coordinates (/8), in the nether the overworld ones (x8).");
        s("coords-hud", "biome", "Biome", "Add a line with the biome.");
        Lang.en("enum:CoordsHud.Style.ONE_LINE", "One line");
        Lang.en("enum:CoordsHud.Style.LINES", "Lines");

        // Direction
        Lang.en("direction-hud", "Direction");
        Lang.en("direction-hud#desc", "Shows the cardinal direction and yaw, optionally a compass strip.");
        base("direction-hud");
        s("direction-hud", "yaw", "Yaw", "Show the rotation angle in degrees.");
        s("direction-hud", "eight-directions", "8 directions", "Include intercardinal directions (NE, SW...).");
        s("direction-hud", "axis", "Axis", "Show the axis you are looking along (+X, -Z...).");
        s("direction-hud", "compass", "Compass strip", "Draw a compass strip under the text.");
        s("direction-hud", "letters-in-english", "N/E/S/W letters", "English cardinal letters instead of Russian (always on with the English interface).");

        // Speed
        Lang.en("speed-hud", "Speed");
        Lang.en("speed-hud#desc", "Shows your movement speed in blocks per second.");
        base("speed-hud");
        s("speed-hud", "unit", "Unit", "Blocks per second or kilometres per hour.");
        s("speed-hud", "vertical", "Include vertical", "Count vertical speed too (falling, jumping).");
        s("speed-hud", "smoothing", "Smoothing", "Number of ticks to average the speed over.");
        s("speed-hud", "decimals", "Decimal places", "Precision of the value.");
        Lang.en("enum:SpeedHud.Unit.BPS", "Blocks/s");
        Lang.en("enum:SpeedHud.Unit.KMH", "Km/h");

        // Clock
        Lang.en("clock-hud", "Time");
        Lang.en("clock-hud#desc", "Shows the real and/or in-game time.");
        base("clock-hud");
        s("clock-hud", "show", "Show", "Real time, game time or both.");
        s("clock-hud", "12-hour", "12-hour format", "Time in AM/PM format.");
        s("clock-hud", "seconds", "Seconds", "Show seconds of the real time.");
        s("clock-hud", "day", "Day number", "Add the in-game day number.");
        Lang.en("enum:ClockHud.Show.REAL", "Real");
        Lang.en("enum:ClockHud.Show.GAME", "In-game");
        Lang.en("enum:ClockHud.Show.BOTH", "Both");

        // Biome
        Lang.en("biome-hud", "Biome");
        Lang.en("biome-hud#desc", "Shows the name of the biome you are standing in.");
        base("biome-hud");

        // Item counter
        String m = "item-counter";
        Lang.en(m, "Item Counter");
        Lang.en(m + "#desc", "Counts pearls, totems, apples, arrows and other items in your inventory.");
        s(m, "layout", "Layout", "Items in a row or in a column.");
        s(m, "zero", "Zero counts", "What to do with items you don't have.");
        s(m, "low-warning", "Low warning", "Highlight the counter when items run low (thresholds below).");
        s(m, "blink", "Blink", "Pulse when the count is low.");
        s(m, "background", "Background", "Draw a panel under the HUD.");
        s(m, "accent-line", "Accent line", "Thin accent-coloured line at the top of the panel.");
        s(m, "text-shadow", "Text shadow", "Draw text with a shadow.");
        s(m, "scale", "Size", "Size of the element on screen (also mouse wheel in the HUD editor).");
        s(m, "background-color", "Background", "Panel colour.");
        s(m, "text", "Text", "Count colour.");
        s(m, "zero-color", "Zero counts", "Counter colour when you have none.");
        s(m, "warning", "Warning", "Counter colour when the count is low.");
        Lang.en("enum:ItemCounter.Layout.HORIZONTAL", "Row");
        Lang.en("enum:ItemCounter.Layout.VERTICAL", "Column");
        Lang.en("enum:ItemCounter.Zero.SHOW", "Show");
        Lang.en("enum:ItemCounter.Zero.GRAY", "Dim");
        Lang.en("enum:ItemCounter.Zero.HIDE", "Hide");
        item(m, "totems", "Totems");
        item(m, "pearls", "Ender pearls");
        item(m, "golden-apples", "Golden apples");
        item(m, "enchanted-apples", "Enchanted apples");
        item(m, "arrows", "Arrows");
        item(m, "wind-charges", "Wind charges");
        item(m, "xp-bottles", "Bottles o' enchanting");
        item(m, "end-crystals", "End crystals");
        item(m, "obsidian", "Obsidian");
        item(m, "respawn-anchors", "Respawn anchors");
        item(m, "glowstone", "Glowstone");
        item(m, "cobwebs", "Cobwebs");
        item(m, "fireworks", "Fireworks");
        item(m, "chorus", "Chorus fruit");
        item(m, "splash-potions", "Splash potions");
        item(m, "golden-carrots", "Golden carrots");
    }

    private static void item(String m, String key, String title) {
        s(m, key, title, "Count " + title.toLowerCase() + ".");
        s(m, "low-" + key, title, "At this count and below the counter is highlighted as a warning.");
    }
}
