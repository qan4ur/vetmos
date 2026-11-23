package io.github.qan4ur.vetmos.util;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Clean UI font (Fira Sans, SIL OFL) used only inside the Vetmos GUI. Defined in assets/vetmos/font/ui.json and
 * ui_bold.json; missing glyphs fall back to the vanilla font. Can be turned off in "Интерфейс" (pixel font).
 */
public final class Fonts {
    public static final Identifier UI = Identifier.of("vetmos", "ui");
    public static final Identifier UI_BOLD = Identifier.of("vetmos", "ui_bold");

    private Fonts() {}

    /** Whether the clean font is enabled. */
    public static boolean enabled() {
        try {
            InterfaceModule m = ModuleManager.get(InterfaceModule.class);
            return m == null || m.customFont.get();
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Style with the UI font (or the vanilla font when disabled). */
    public static Style style(boolean bold) {
        if (!enabled()) return bold ? Style.EMPTY.withBold(true) : Style.EMPTY;
        return Style.EMPTY.withFont(bold ? UI_BOLD : UI);
    }

    /** Text in the UI font. */
    public static MutableText text(String s) {
        return Text.literal(s).setStyle(style(false));
    }

    /** Bold text in the UI font. */
    public static MutableText bold(String s) {
        return Text.literal(s).setStyle(style(true));
    }

    /** Applies the UI font to an existing text, keeping its other styling (colour etc.). */
    public static MutableText apply(Text t, boolean bold) {
        if (!enabled()) return t.copy();
        Identifier font = bold ? UI_BOLD : UI;
        return t.copy().styled(s -> s.withFont(font));
    }
}
