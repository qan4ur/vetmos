package io.github.qan4ur.vetmos.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Placeholder — replaced by the real HUD editor. */
public class HudEditorScreen extends Screen {
    private final Screen parent;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD"));
        this.parent = parent;
    }
}
