package io.github.qan4ur.vetmos.modules.client;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Chat timestamps and stacking of identical consecutive messages ("text ×3").
 * The ChatHud work is done in ChatTweaksChatHudMixin; this class only holds the settings and builds the text.
 */
public class ChatTweaks extends Module {
    private static final String TIME = "Время";
    private static final String STACK = "Сложение";

    // Timestamps

    private final BoolSetting timestamps = bool(TIME, "timestamps", "Время сообщений", "Показывать время перед каждым сообщением.", true);

    private final EnumSetting<ChatTimeFormat> format = enumeration(TIME, "format", "Формат", "Формат времени.", ChatTimeFormat.HH_MM)
        .visibleWhen(timestamps::get);

    private final EnumSetting<ChatBrackets> brackets = enumeration(TIME, "brackets", "Скобки", "Чем обрамлять время.", ChatBrackets.SQUARE)
        .visibleWhen(timestamps::get);

    private final BoolSetting accentTime = bool(TIME, "accent-color", "Цвет акцента", "Красить время в цвет акцента интерфейса.", false)
        .visibleWhen(timestamps::get);

    private final ColorSetting timeColor = color(TIME, "color", "Цвет", "Цвет времени.", Theme.TEXT_MUTED)
        .visibleWhen(() -> timestamps.get() && !accentTime.get());

    // Stacking

    private final BoolSetting stacking = bool(STACK, "stacking", "Складывать повторы",
        "Одинаковые сообщения подряд складываются в одно с счётчиком «×3».", true);

    private final ColorSetting stackColor = color(STACK, "stack-color", "Цвет счётчика", "Цвет счётчика повторов.", Theme.TEXT_MUTED)
        .visibleWhen(stacking::get);

    public ChatTweaks() {
        super("chat-tweaks", "Чат", "Время перед сообщениями и сложение одинаковых сообщений подряд.", Category.CLIENT);
    }

    /** The module when enabled, otherwise null. */
    public static ChatTweaks active() {
        ChatTweaks m = ModuleManager.get(ChatTweaks.class);
        return m != null && m.isEnabled() ? m : null;
    }

    public boolean stackingEnabled() {
        return stacking.get();
    }

    /** Builds the displayed line: [time] + original message + " ×count" (count > 1). */
    public Text decorate(Text message, int count) {
        Text result = message;
        if (stacking.get() && count > 1) {
            result = Text.empty().append(message).append(Text.literal(" ×" + count).withColor(stackColor.get() & 0xFFFFFF));
        }
        if (timestamps.get()) {
            String time = LocalTime.now().format(format.get().formatter);
            int rgb = (accentTime.get() ? Theme.accent() : timeColor.get()) & 0xFFFFFF;
            MutableText stamp = Text.literal(brackets.get().wrap(time) + " ").withColor(rgb);
            result = Text.empty().append(stamp).append(result);
        }
        return result;
    }

    public enum ChatTimeFormat {
        HH_MM("HH:mm", "14:05"),
        HH_MM_SS("HH:mm:ss", "14:05:09"),
        H_MM_A("h:mm a", "2:05 PM");

        final DateTimeFormatter formatter;
        private final String example;

        ChatTimeFormat(String pattern, String example) {
            this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
            this.example = example;
        }

        @Override
        public String toString() {
            return Lang.tr("enum:ChatTimeFormat." + name(), example);
        }
    }

    public enum ChatBrackets {
        SQUARE("[", "]", "Квадратные [ ]"),
        ANGLE("<", ">", "Угловые < >"),
        ROUND("(", ")", "Круглые ( )"),
        PIPE("", " |", "Черта |"),
        NONE("", "", "Без скобок");

        private final String open, close, title;

        ChatBrackets(String open, String close, String title) {
            this.open = open;
            this.close = close;
            this.title = title;
        }

        String wrap(String s) {
            return open + s + close;
        }

        @Override
        public String toString() {
            return Lang.tr("enum:ChatBrackets." + name(), title);
        }
    }
}
