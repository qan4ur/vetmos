package io.github.qan4ur.vetmos.util;

import net.minecraft.text.*;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes coordinates in chat messages clickable: a click copies them to the clipboard. Built in, always on.
 * Recognises "-515 64 -515", "-515, 64, -515", "-515/64/-515", "x: -515 y: 64 z: -515" and decimals.
 */
public final class ChatCoords {
    private static final String NUM = "-?\\d{1,8}(?:[.,]\\d{1,3})?";
    private static final Pattern PLAIN = Pattern.compile(
        "(?<![\\w.\\-])(" + NUM + ")(?:\\s*[,;/]\\s*|\\s+)(" + NUM + ")(?:\\s*[,;/]\\s*|\\s+)(" + NUM + ")(?![\\w.:])");
    private static final Pattern LABELLED = Pattern.compile(
        "(?i)\\bx\\s*[:=]?\\s*(" + NUM + ")[\\s,;]+y\\s*[:=]?\\s*(" + NUM + ")[\\s,;]+z\\s*[:=]?\\s*(" + NUM + ")");

    private ChatCoords() {}

    /** Returns the message with clickable coordinates, or the original if there are none. */
    public static Text link(Text message) {
        if (message == null) return null;
        String plain = message.getString();
        if (!PLAIN.matcher(plain).find() && !LABELLED.matcher(plain).find()) return message;

        MutableText out = Text.empty();
        boolean[] changed = { false };
        message.visit((style, segment) -> {
            appendLinked(out, style, segment, changed);
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? out : message;
    }

    private static void appendLinked(MutableText out, Style style, String segment, boolean[] changed) {
        Matcher m = findFrom(segment, 0);
        int pos = 0;
        while (m != null) {
            if (m.start() > pos) out.append(Text.literal(segment.substring(pos, m.start())).setStyle(style));
            String value = num(m.group(1)) + " " + num(m.group(2)) + " " + num(m.group(3));
            Text hover = Text.literal(Lang.ui("coords-copy", "Нажми, чтобы скопировать координаты") + "\n")
                .append(Text.literal(value).withColor(0xFFFFFF));
            Style linked = style
                .withClickEvent(new ClickEvent.CopyToClipboard(value))
                .withHoverEvent(new HoverEvent.ShowText(hover))
                .withUnderline(true);
            out.append(Text.literal(m.group()).setStyle(linked));
            changed[0] = true;
            pos = m.end();
            m = findFrom(segment, pos);
        }
        if (pos < segment.length()) out.append(Text.literal(segment.substring(pos)).setStyle(style));
    }

    /** Earliest match of either pattern starting at {@code from}, or null. */
    private static Matcher findFrom(String s, int from) {
        Matcher a = LABELLED.matcher(s);
        Matcher b = PLAIN.matcher(s);
        boolean fa = a.find(from), fb = b.find(from);
        if (fa && fb) return a.start() <= b.start() ? a : b;
        return fa ? a : fb ? b : null;
    }

    private static String num(String n) {
        return n.replace(',', '.');
    }
}
