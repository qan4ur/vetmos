package io.github.qan4ur.vetmos.util;

/** English translations for the zoom module (see {@link Lang} for the key format). */
final class LangEnZoom {
    private LangEnZoom() {}

    static void register() {
        Lang.en("zoom.key", "Zoom key");
        Lang.en("zoom.key#desc", "Key (or mouse button) that zooms. Doesn't conflict with vanilla key binds.");
        Lang.en("zoom", "Zoom");
        Lang.en("zoom#desc", "Built-in zoom: hold the key (C by default, change it in the settings) to zoom in.");

        Lang.en("group:Колёсико мыши", "Mouse wheel");
        Lang.en("group:Вид", "View");

        Lang.en("zoom.mode", "Mode");
        Lang.en("zoom.mode#desc", "Hold: zoom while the key is held. Toggle: press to zoom in, press again to zoom out.");
        Lang.en("zoom.factor", "Zoom factor");
        Lang.en("zoom.factor#desc", "How many times to magnify.");
        Lang.en("zoom.reset-on-release", "Reset zoom level");
        Lang.en("zoom.reset-on-release#desc", "After releasing, zoom returns to the base factor; otherwise the level chosen with the wheel is kept.");

        Lang.en("zoom.smooth", "Smooth zoom");
        Lang.en("zoom.smooth#desc", "Smooth zooming in and out.");
        Lang.en("zoom.speed", "Animation speed");
        Lang.en("zoom.speed#desc", "How fast the zoom animation plays (higher is faster).");
        Lang.en("zoom.easing", "Easing");
        Lang.en("zoom.easing#desc", "Zoom animation curve.");

        Lang.en("zoom.scroll", "Change with wheel");
        Lang.en("zoom.scroll#desc", "While zooming, the mouse wheel changes the zoom level (the hotbar does not scroll).");
        Lang.en("zoom.scroll-step", "Step");
        Lang.en("zoom.scroll-step#desc", "By how many percent one wheel notch changes the zoom level.");
        Lang.en("zoom.scroll-min", "Minimum");
        Lang.en("zoom.scroll-min#desc", "Minimum zoom level when scrolling.");
        Lang.en("zoom.scroll-max", "Maximum");
        Lang.en("zoom.scroll-max#desc", "Maximum zoom level when scrolling.");

        Lang.en("zoom.reduce-sensitivity", "Reduce sensitivity");
        Lang.en("zoom.reduce-sensitivity#desc", "Mouse sensitivity is reduced in proportion to the zoom.");
        Lang.en("zoom.cinematic", "Cinematic camera");
        Lang.en("zoom.cinematic#desc", "Turns on the smooth camera (like F8) while zooming.");
        Lang.en("zoom.hide-hand", "Hide hand");
        Lang.en("zoom.hide-hand#desc", "Don't render the hand and held item while zooming.");
        Lang.en("zoom.vignette", "Vignette");
        Lang.en("zoom.vignette#desc", "Subtle darkening of the screen edges while zoomed.");
        Lang.en("zoom.vignette-strength", "Vignette strength");
        Lang.en("zoom.vignette-strength#desc", "How strongly to darken the edges.");
        Lang.en("zoom.vignette-red", "Red tint");
        Lang.en("zoom.vignette-red#desc", "Blood moon red tint for the vignette.");

        Lang.en("enum:Zoom.Mode.HOLD", "Hold");
        Lang.en("enum:Zoom.Mode.TOGGLE", "Toggle");
        Lang.en("enum:Zoom.Easing.LINEAR", "Linear");
        Lang.en("enum:Zoom.Easing.EASE_OUT", "Ease out");
        Lang.en("enum:Zoom.Easing.EASE_IN_OUT", "Ease in-out");
        Lang.en("enum:Zoom.Easing.EXPO", "Exponential");
        Lang.en("enum:Zoom.Easing.BACK", "Spring");
    }
}
