package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.util.Theme;

/**
 * GUI colors: a monochrome base (near-black panels, white text, grey lines) plus one accent color read live from
 * {@code Theme.accent()} every frame, so changing the accent in the "Интерфейс" module recolors the GUI at once.
 */
public final class Palette {
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_IDLE = 0xFFD2D2D2;
    public static final int TEXT_SECONDARY = 0xFFBDBDBD;
    public static final int TEXT_MUTED = 0xFF8A8A8A;
    public static final int TEXT_FAINT = 0xFF5E5E5E;

    public static final int PANEL_TOP = 0xEC161616;
    public static final int PANEL_BOTTOM = 0xEC0B0B0B;
    public static final int HEADER_TOP = 0xFF262626;
    public static final int HEADER_BOTTOM = 0xFF151515;
    public static final int HEADER_TOP_HOVER = 0xFF303030;
    public static final int HEADER_BOTTOM_HOVER = 0xFF1B1B1B;
    public static final int OUTLINE = 0x2EFFFFFF;
    public static final int SHEEN = 0x1CFFFFFF;
    public static final int SHADOW = 0xB0000000;

    public static final int CONTROL = 0xFF181818;
    public static final int CONTROL_HOVER = 0xFF242424;
    public static final int CONTROL_LINE = 0xFF363636;
    public static final int CONTROL_LINE_HOVER = 0xFF5A5A5A;
    public static final int TRACK = 0xFF2A2A2A;

    private Palette() {}

    /** The user's accent color (opaque), read live from the theme every call. */
    public static int accent() {
        return Theme.accent() | 0xFF000000;
    }

    public static int accentA(int alpha) {
        return Anim.withAlpha(accent(), alpha);
    }

    /** Accent pushed towards white (hover / highlight variant). */
    public static int accentBright() {
        return Anim.mix(accent(), 0xFFFFFFFF, 0.3f);
    }

    /** Accent at lower brightness. */
    public static int accentDim() {
        return Anim.mix(accent(), 0xFF000000, 0.45f);
    }

    /** Soft glow in the accent color. */
    public static int glow() {
        return accentA(100);
    }

    /** Perceived brightness 0..1. */
    public static float luminance(int argb) {
        return (0.299f * (argb >> 16 & 255) + 0.587f * (argb >> 8 & 255) + 0.114f * (argb & 255)) / 255f;
    }

    /** Black or white, whichever reads better on top of the accent. */
    public static int onAccent() {
        return luminance(accent()) > 0.6f ? 0xFF101010 : 0xFFFFFFFF;
    }
}
