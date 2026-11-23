package io.github.qan4ur.vetmos.util;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface language. Russian texts live in the code (module/setting titles and descriptions);
 * English texts come from the table below, keyed by:
 * <ul>
 *   <li>{@code module} and {@code module#desc} - module title / description</li>
 *   <li>{@code module.setting} and {@code module.setting#desc} - setting title / description</li>
 *   <li>{@code group:Основное} - setting group title</li>
 *   <li>{@code category:COMBAT} - category title</li>
 *   <li>{@code ui:some-key} - fixed GUI strings (see {@link #ui(String, String)})</li>
 * </ul>
 * Missing English entries fall back to the Russian text.
 */
public final class Lang {
    private static final Map<String, String> EN = new HashMap<>();

    private Lang() {}

    public static boolean isEnglish() {
        try {
            InterfaceModule m = ModuleManager.get(InterfaceModule.class);
            return m != null && m.language.get() == InterfaceModule.Language.EN;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** English text for the key, or the given Russian text when Russian is selected or no translation exists. */
    public static String tr(String key, String russian) {
        if (!isEnglish()) return russian;
        String en = EN.get(key);
        return en != null ? en : russian;
    }

    /** Fixed GUI string: {@code Lang.ui("reset", "Сбросить")}. */
    public static String ui(String key, String russian) {
        return tr("ui:" + key, russian);
    }

    /** Registers English translations (called from {@link LangEn}). */
    static void en(String key, String english) {
        EN.put(key, english);
    }

    static {
        LangEn.register();
    }
}
