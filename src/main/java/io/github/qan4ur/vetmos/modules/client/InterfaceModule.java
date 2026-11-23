package io.github.qan4ur.vetmos.modules.client;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import io.github.qan4ur.vetmos.util.Theme;

/** Global look of Vetmos: accent colour of the menu and of chat messages. Always on. */
public class InterfaceModule extends Module {
    public final ColorSetting accent = color("Основное", "accent", "Акцентный цвет",
        "Цвет активных модулей, переключателей и выделений в меню. По умолчанию почти белый.", Theme.DEFAULT_ACCENT);
    public final BoolSetting chatAccent = bool("Основное", "chat-accent", "Цвет префикса в чате",
        "Красить префикс [Vetmos] в чате акцентным цветом (иначе белый).", true);

    public InterfaceModule() {
        super("interface", "Интерфейс", "Общий вид Vetmos: акцентный цвет меню и сообщений. Основной стиль чёрно-белый.", Category.CLIENT, true, io.github.qan4ur.vetmos.util.Keys.NONE);
    }

    /** The interface can't be turned off. */
    @Override
    protected void onDisable() {
        setEnabled(true);
    }
}
