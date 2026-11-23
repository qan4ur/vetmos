package io.github.qan4ur.vetmos.util;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;

/**
 * Monochrome palette (black / white / greys) plus one user-configurable accent colour.
 * The constants are used as defaults; the getters read the "Интерфейс" module at render time.
 */
public final class Theme {
    // Neutral palette (ARGB)
    public static final int WHITE = 0xFFFFFFFF;
    public static final int LIGHT = 0xFFF0F0F0;
    public static final int GREY = 0xFFBDBDBD;
    public static final int MID = 0xFF6E6E6E;
    public static final int DARK = 0xFF2A2A2A;
    public static final int BLACK = 0xFF000000;

    public static final int PANEL = 0xC80C0C0C;
    public static final int PANEL_SOLID = 0xF00A0A0A;
    public static final int PANEL_LIGHT = 0xE61C1C1C;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFBDBDBD;
    public static final int TEXT_MUTED = 0xFF8A8A8A;

    /** Warning colour (low HP, low durability) — the only red that stays. */
    public static final int WARNING = 0xFFE5484D;
    public static final int GOLD = 0xFFD4AF37;

    // Kept for compatibility with older code; now monochrome.
    public static final int SCARLET = LIGHT;
    public static final int SCARLET_BRIGHT = WHITE;
    public static final int DARK_RED = DARK;
    public static final int BLOOD = 0xFF1A1A1A;
    public static final int GLOW = 0x40FFFFFF;

    /** Default accent: near-white. */
    public static final int DEFAULT_ACCENT = LIGHT;

    private Theme() {}

    private static InterfaceModule ui() {
        try {
            return ModuleManager.get(InterfaceModule.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Accent colour chosen by the user (opaque). */
    public static int accent() {
        InterfaceModule m = ui();
        return m != null ? (m.accent.get() | 0xFF000000) : DEFAULT_ACCENT;
    }

    /** Accent at ~55% brightness (for borders, inactive parts). */
    public static int accentDim() {
        return mix(accent(), BLACK, 0.45f);
    }

    /** Accent with the given alpha (0-255). */
    public static int accentAlpha(int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (accent() & 0xFFFFFF);
    }

    /** Mixes a colour toward the accent by t (0 = argb, 1 = accent), keeping argb's alpha. */
    public static int accentMix(int argb, float t) {
        int mixed = mix(argb, accent(), t);
        return (argb & 0xFF000000) | (mixed & 0xFFFFFF);
    }

    /** Linear mix of two ARGB colours. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16
            | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }
}
