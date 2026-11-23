package io.github.qan4ur.vetmos.hud;

import com.google.gson.JsonObject;

/**
 * Where a HUD element sits. Offsets are in GUI units and measured from the anchor to the matching point of the
 * element box: LEFT anchors the left edge, CENTER the box center, RIGHT the right edge (same for Y).
 */
public class HudPosition {
    public enum AnchorX { LEFT, CENTER, RIGHT }

    public enum AnchorY { TOP, CENTER, BOTTOM }

    public AnchorX anchorX;
    public AnchorY anchorY;
    public double x, y;

    public HudPosition(AnchorX anchorX, AnchorY anchorY, double x, double y) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.x = x;
        this.y = y;
    }

    public HudPosition copy() {
        return new HudPosition(anchorX, anchorY, x, y);
    }

    /** Top-left corner of a box of the given size on a screen of the given size. */
    public double left(double boxWidth, double screenWidth) {
        return switch (anchorX) {
            case LEFT -> x;
            case CENTER -> screenWidth / 2 + x - boxWidth / 2;
            case RIGHT -> screenWidth + x - boxWidth;
        };
    }

    public double top(double boxHeight, double screenHeight) {
        return switch (anchorY) {
            case TOP -> y;
            case CENTER -> screenHeight / 2 + y - boxHeight / 2;
            case BOTTOM -> screenHeight + y - boxHeight;
        };
    }

    /** Moves the box so that its top-left corner is at (left, top), picking the closest anchors. */
    public void setFromTopLeft(double left, double top, double boxWidth, double boxHeight, double screenWidth, double screenHeight) {
        double cx = left + boxWidth / 2, cy = top + boxHeight / 2;
        anchorX = cx < screenWidth / 3 ? AnchorX.LEFT : cx > screenWidth * 2 / 3 ? AnchorX.RIGHT : AnchorX.CENTER;
        anchorY = cy < screenHeight / 3 ? AnchorY.TOP : cy > screenHeight * 2 / 3 ? AnchorY.BOTTOM : AnchorY.CENTER;
        x = switch (anchorX) {
            case LEFT -> left;
            case CENTER -> cx - screenWidth / 2;
            case RIGHT -> left + boxWidth - screenWidth;
        };
        y = switch (anchorY) {
            case TOP -> top;
            case CENTER -> cy - screenHeight / 2;
            case BOTTOM -> top + boxHeight - screenHeight;
        };
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("anchorX", anchorX.name());
        o.addProperty("anchorY", anchorY.name());
        o.addProperty("x", x);
        o.addProperty("y", y);
        return o;
    }

    public static HudPosition fromJson(JsonObject o, HudPosition fallback) {
        try {
            return new HudPosition(AnchorX.valueOf(o.get("anchorX").getAsString()), AnchorY.valueOf(o.get("anchorY").getAsString()),
                o.get("x").getAsDouble(), o.get("y").getAsDouble());
        } catch (RuntimeException e) {
            return fallback.copy();
        }
    }
}
