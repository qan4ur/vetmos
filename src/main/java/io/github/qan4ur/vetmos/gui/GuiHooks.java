package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * GUI-side hooks. Opens the ClickGUI (which then shows the welcome guide) shortly after the first world join while
 * the guide has not been seen yet. Registered once from {@link GuiLang#register()}.
 */
public final class GuiHooks {
    private static boolean registered;
    private static boolean shownThisSession;
    private static int inWorldTicks;

    private GuiHooks() {}

    public static void init() {
        if (registered) return;
        registered = true;
        try {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (shownThisSession || ConfigManager.isWelcomeShown()) return;
                if (client.player == null || client.world == null || client.currentScreen != null || client.getOverlay() != null) {
                    inWorldTicks = 0;
                    return;
                }
                if (++inWorldTicks >= 60) { // ~3 s after joining, so the world is visible behind the guide
                    shownThisSession = true;
                    client.setScreen(new ClickGuiScreen());
                }
            });
        } catch (Throwable t) {
            registered = false;
        }
    }

    /** The ClickGUI showed the guide itself (first manual open). */
    static void markShown() {
        shownThisSession = true;
    }
}
