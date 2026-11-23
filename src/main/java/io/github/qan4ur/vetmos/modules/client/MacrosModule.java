package io.github.qan4ur.vetmos.modules.client;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.util.Keys;

/**
 * Macros: bind chat messages, server commands ("/...") or Vetmos commands ("...") to keys, each with its own cooldown.
 * The list itself is edited in the module's settings window (or with .macro).
 */
public class MacrosModule extends Module {
    public final BoolSetting cooldownMessage = bool("Основное", "cooldown-message", "Сообщение о перезарядке",
        "Писать в чат, сколько осталось до повторного использования, если нажать раньше времени.", true);

    public MacrosModule() {
        super("macros", "Макросы", "Бинды на клавиши: сообщение в чат, /команда или команда Vetmos. У каждого макроса своя перезарядка.",
            Category.CLIENT, true, Keys.NONE);
    }
}
