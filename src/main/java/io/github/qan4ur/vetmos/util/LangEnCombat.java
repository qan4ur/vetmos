package io.github.qan4ur.vetmos.util;

/** English translations for the damage numbers and hitboxes modules (see {@link Lang} for the key format). */
final class LangEnCombat {
    private LangEnCombat() {}

    private static void m(String module, String title, String desc) {
        Lang.en(module, title);
        Lang.en(module + "#desc", desc);
    }

    private static void s(String module, String setting, String title, String desc) {
        Lang.en(module + "." + setting, title);
        Lang.en(module + "." + setting + "#desc", desc);
    }

    static void register() {
        Lang.en("group:Фильтры", "Filters");
        Lang.en("group:Линии", "Lines");

        damageNumbers();
        hitboxes();
    }

    private static void damageNumbers() {
        String d = "damage-numbers";
        m(d, "Damage Numbers", "Floating damage numbers above entities.");
        s(d, "only-my-targets", "Only My Targets", "Show damage only on entities you hit recently.");
        s(d, "target-memory", "Target Memory", "How many seconds after a hit the entity counts as your target.");
        s(d, "players-only", "Players Only", "Show damage only on players.");
        s(d, "healing", "Healing", "Show health regained in another colour.");
        s(d, "max-distance", "Max Distance", "Maximum distance to the entity (blocks).");
        s(d, "big-hit", "Big Hit", "Damage from which the number is bigger and highlighted.");
        s(d, "decimals", "Decimals", "0 = whole numbers, 1 = tenths.");
        s(d, "normal-color", "Normal Damage", "Colour of normal damage numbers.");
        s(d, "big-use-accent", "Interface Accent", "Colour big hits with the interface accent.");
        s(d, "big-color", "Big Hit", "Colour of big hit numbers (when the accent is off).");
        s(d, "heal-color", "Healing", "Colour of healing numbers.");
        s(d, "lifetime", "Lifetime", "How many seconds a number stays on screen.");
        s(d, "rise-speed", "Rise Speed", "How fast numbers float up (blocks per second).");
        s(d, "drift", "Sideways Drift", "Numbers drift slightly to a random side.");
        s(d, "pop-in", "Spring Pop-In", "Numbers appear with a springy scale-up.");
        s(d, "size", "Size", "Size of the numbers.");
        s(d, "big-scale", "Big Hit Size", "How much bigger big hit numbers are.");
        s(d, "text-effect", "Outline", "Text shadow or outline.");
        Lang.en("enum:TextEffect.NONE", "None");
        Lang.en("enum:TextEffect.SHADOW", "Shadow");
        Lang.en("enum:TextEffect.OUTLINE", "Outline");
    }

    private static void hitboxes() {
        String h = "hitboxes";
        m(h, "Hitboxes", "Custom colour, thickness and fill for F3+B hitboxes.");
        s(h, "always-show", "Always Show", "Show hitboxes without F3+B.");
        s(h, "line-width", "Line Width", "Line thickness in roughly pixels. 1 = normal thin lines.");
        s(h, "fill", "Fill", "Fill hitboxes with a translucent colour.");
        s(h, "fill-alpha", "Fill Opacity", "Opacity of the fill (0-255).");
        s(h, "only-players", "Only Players", "Show hitboxes of players only.");
        s(h, "hide-items", "Hide Items", "Don't show hitboxes of dropped items.");
        s(h, "hide-projectiles", "Hide Projectiles", "Don't show hitboxes of arrows, potions, pearls and other projectiles.");
        s(h, "use-accent", "Interface Accent", "Colour hitboxes with the interface accent.");
        s(h, "color", "Color", "Hitbox colour (when the accent is off).");
        s(h, "separate-colors", "Separate Colors", "Own colour for players, mobs and items.");
        s(h, "player-color", "Players", "Hitbox colour of players.");
        s(h, "mob-color", "Mobs", "Hitbox colour of mobs and other creatures.");
        s(h, "item-color", "Items", "Hitbox colour of dropped items.");
        s(h, "look-direction", "Look Direction", "Line showing where the entity looks (the blue F3+B line).");
        s(h, "look-color", "Look Color", "Colour of the look direction line.");
        s(h, "eye-height", "Eye Height", "Frame at the entity's eye height.");
        s(h, "eye-color", "Eye Height Color", "Colour of the eye height frame.");
    }
}
