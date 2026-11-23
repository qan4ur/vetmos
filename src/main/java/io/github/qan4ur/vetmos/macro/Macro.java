package io.github.qan4ur.vetmos.macro;

import com.google.gson.JsonObject;
import io.github.qan4ur.vetmos.util.Keys;

/** One key bind that sends a chat message, a server command ("/...") or a Vetmos command ("..."). */
public class Macro {
    public int key = Keys.NONE;
    public String text = "";
    /** Seconds between two uses. */
    public double cooldown = 1.0;
    public boolean enabled = true;
    /** Optional label shown in the GUI (empty = the text itself). */
    public String name = "";

    /** System time of the last use (not saved). */
    public transient long lastUsed;

    public Macro() {}

    public Macro(int key, String text, double cooldown) {
        this.key = key;
        this.text = text;
        this.cooldown = cooldown;
    }

    /** Seconds left until the macro can be used again (0 = ready). */
    public double cooldownLeft() {
        double left = cooldown - (System.currentTimeMillis() - lastUsed) / 1000.0;
        return Math.max(0, left);
    }

    public String label() {
        return name == null || name.isBlank() ? text : name;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("key", key);
        o.addProperty("text", text);
        o.addProperty("cooldown", cooldown);
        o.addProperty("enabled", enabled);
        if (name != null && !name.isBlank()) o.addProperty("name", name);
        return o;
    }

    public static Macro fromJson(JsonObject o) {
        Macro m = new Macro();
        if (o.has("key")) m.key = o.get("key").getAsInt();
        if (o.has("text")) m.text = o.get("text").getAsString();
        if (o.has("cooldown")) m.cooldown = Math.max(0, o.get("cooldown").getAsDouble());
        if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
        if (o.has("name")) m.name = o.get("name").getAsString();
        return m;
    }
}
