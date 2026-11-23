package io.github.qan4ur.vetmos.util;

/** English translations for the movable vanilla HUD modules (see {@link Lang} for the key format). */
final class LangEnVanilla {
    private LangEnVanilla() {}

    private static void s(String module, String setting, String title, String desc) {
        Lang.en(module + "." + setting, title);
        Lang.en(module + "." + setting + "#desc", desc);
    }

    private static void scale(String m) {
        s(m, "scale", "Size", "Size of the element on screen (also mouse wheel in the HUD editor).");
    }

    static void register() {
        // ---------------------------------------------------------------- scoreboard
        String m = "scoreboard-hud";
        Lang.en(m, "Scoreboard");
        Lang.en(m + "#desc", "Movable sidebar scoreboard: hide the red numbers and style the background.");
        s(m, "hide-numbers", "Hide numbers", "Don't show the red score numbers on the right.");
        s(m, "max-lines", "Max lines", "How many lines to show (vanilla: 15).");
        s(m, "line-spacing", "Line spacing", "Extra space between lines.");
        s(m, "align", "Alignment", "Alignment of the line text.");
        s(m, "text-shadow", "Text shadow", "Draw text with a shadow.");
        scale(m);
        s(m, "background", "Background", "Background colour and opacity (the title row is slightly darker).");
        s(m, "text", "Text", "Colour of text without its own formatting.");
        Lang.en("enum:ScoreAlign.LEFT", "Left");
        Lang.en("enum:ScoreAlign.CENTER", "Center");
        Lang.en("enum:ScoreAlign.RIGHT", "Right");
        // editor preview
        Lang.en("hud:sb-mode", "Mode: ");
        Lang.en("hud:sb-duels", "Duels");
        Lang.en("hud:sb-kills", "Kills: ");
        Lang.en("hud:sb-deaths", "Deaths: ");
        Lang.en("hud:sb-online", "Online: ");

        // ---------------------------------------------------------------- boss bar
        m = "boss-bar-hud";
        Lang.en(m, "Boss Bar");
        Lang.en(m + "#desc", "Movable and resizable boss health bars.");
        s(m, "max-bars", "Max bars", "How many boss bars to show at once.");
        s(m, "hide-names", "Hide names", "Don't show the names above the bars.");
        s(m, "text-shadow", "Text shadow", "Draw the names with a shadow.");
        s(m, "spacing", "Spacing", "Space between the bars.");
        scale(m);
        Lang.en("hud:sample-boss", "Ender Dragon");

        // ---------------------------------------------------------------- titles
        m = "titles-hud";
        Lang.en(m, "Titles");
        Lang.en(m + "#desc", "Movable titles and subtitles (/title).");
        s(m, "opacity", "Opacity", "Text opacity multiplier.");
        s(m, "hide-subtitle", "Hide subtitle", "Don't show the subtitle.");
        s(m, "title-size", "Title size", "Title size relative to normal text (vanilla: 4).");
        s(m, "subtitle-size", "Subtitle size", "Subtitle size relative to normal text (vanilla: 2).");
        scale(m);
        Lang.en("hud:sample-title", "Title");
        Lang.en("hud:sample-subtitle", "Subtitle");

        // ---------------------------------------------------------------- action bar
        m = "action-bar-hud";
        Lang.en(m, "Action Bar");
        Lang.en(m + "#desc", "Movable message above the hotbar (action bar).");
        s(m, "opacity", "Opacity", "Text opacity multiplier.");
        s(m, "rainbow-tint", "Rainbow colour", "Shifting colour for \"Now playing\", like vanilla.");
        s(m, "text-shadow", "Text shadow", "Draw text with a shadow.");
        scale(m);
        Lang.en("hud:sample-action-bar", "Message above the hotbar");
    }
}
