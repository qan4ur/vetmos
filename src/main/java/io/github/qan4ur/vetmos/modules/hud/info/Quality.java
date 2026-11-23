package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.util.Theme;

/** Muted quality colours shared by the info HUD elements (good / medium / poor / bad). */
final class Quality {
    static final int GOOD = 0xFF7BD88F;
    static final int MEDIUM = 0xFFE6D36A;
    static final int POOR = 0xFFF0A35E;
    static final int BAD = Theme.WARNING;

    private Quality() {}
}
