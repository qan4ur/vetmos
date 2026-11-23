package io.github.qan4ur.vetmos.modules.client;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.util.Theme;

/** Global look of Vetmos: language and accent colour of the menu and of chat messages. Always on. */
public class InterfaceModule extends Module {
    public enum Language {
        RU("Русский"), EN("English");

        private final String display;

        Language(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public final EnumSetting<Language> language = enumeration("Основное", "language", "Язык",
        "Язык интерфейса Vetmos. / Interface language.", Language.RU);
    public final ColorSetting accent = color("Акцент", "accent", "Акцентный цвет",
        "Цвет активных модулей, переключателей и выделений в меню. По умолчанию почти белый.", Theme.DEFAULT_ACCENT);
    public final BoolSetting gradient = bool("Акцент", "gradient", "Градиент",
        "Плавный переход акцента во второй цвет (переливается по меню).", false);
    public final ColorSetting accent2 = color("Акцент", "accent-2", "Второй цвет",
        "Второй цвет градиента.", 0xFF8A8A8A).visibleWhen(gradient::get);
    public final DoubleSetting gradientSpeed = decimal("Акцент", "gradient-speed", "Скорость градиента",
        "Скорость переливания градиента (0 = статичный).", 1.0, 0.0, 5.0, 1).visibleWhen(gradient::get);
    public final BoolSetting chatAccent = bool("Основное", "chat-accent", "Цвет префикса в чате",
        "Красить префикс [Vetmos] в чате акцентным цветом (иначе белый).", true);

    public InterfaceModule() {
        super("interface", "Интерфейс", "Язык и акцентный цвет Vetmos. Основной стиль чёрно-белый.", Category.CLIENT, true, io.github.qan4ur.vetmos.util.Keys.NONE);
    }

    /** The interface can't be turned off. */
    @Override
    protected void onDisable() {
        setEnabled(true);
    }
}
