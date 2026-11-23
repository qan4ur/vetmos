package io.github.qan4ur.vetmos.gui;

/** Easing functions, a per-frame delta time and small animated values used by the GUI. */
public final class Anim {
    /** Seconds since the previous GUI frame (clamped), updated by the screens once per frame. */
    public static float dt = 1 / 60f;
    private static long lastFrame = -1;

    private Anim() {}

    /** Call once at the start of every screen frame. */
    public static void frame() {
        long now = System.nanoTime();
        float gap = lastFrame < 0 ? 1 : (now - lastFrame) / 1_000_000_000f;
        // a long gap means the GUI was just (re)opened: start animations from a normal frame
        dt = gap > 0.25f ? 1 / 60f : Math.max(0, Math.min(0.1f, gap));
        lastFrame = now;
    }

    public static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float f = 1 - t;
        return 1 - f * f * f;
    }

    public static float easeInOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.4f, c3 = c1 + 1;
        float f = t - 1;
        return 1 + c3 * f * f * f + c1 * f * f;
    }

    /** Frame-rate independent exponential approach. */
    public static float approach(float current, float target, float speed) {
        float k = 1 - (float) Math.exp(-speed * dt);
        float v = current + (target - current) * k;
        return Math.abs(target - v) < 0.0005f ? target : v;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Linear interpolation between two ARGB colors. */
    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16
            | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    /** Same color with its alpha multiplied by f. */
    public static int alpha(int color, float f) {
        int a = Math.round((color >>> 24) * clamp01(f));
        return a << 24 | (color & 0xFFFFFF);
    }

    /** Same color with the given alpha (0..255). */
    public static int withAlpha(int color, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (color & 0xFFFFFF);
    }

    /**
     * A value that moves linearly towards 0 or 1 over a fixed duration; {@link #eased()} applies an ease-in-out
     * curve. Used for expand/collapse and other on/off transitions.
     */
    public static final class Toggle {
        private float t;
        private boolean on;
        private final float duration;

        public Toggle(float duration, boolean on) {
            this.duration = duration;
            this.on = on;
            this.t = on ? 1 : 0;
        }

        public void set(boolean on) {
            this.on = on;
        }

        public void snap(boolean on) {
            this.on = on;
            this.t = on ? 1 : 0;
        }

        public boolean on() {
            return on;
        }

        public void update() {
            float step = dt / duration;
            t = on ? Math.min(1, t + step) : Math.max(0, t - step);
        }

        public float raw() {
            return t;
        }

        public float eased() {
            return easeInOutCubic(t);
        }

        public boolean idle() {
            return on ? t >= 1 : t <= 0;
        }
    }
}
