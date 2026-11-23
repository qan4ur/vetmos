package io.github.qan4ur.vetmos.util;

import io.github.qan4ur.vetmos.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Chat messages with the red [Vetmos] prefix. Only shown locally. */
public final class ChatUtil {
    private ChatUtil() {}

    public static MutableText prefix() {
        return Text.literal("[").formatted(Formatting.DARK_GRAY)
            .append(Text.literal("Vetmos").withColor(prefixColor()))
            .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }

    private static int prefixColor() {
        io.github.qan4ur.vetmos.modules.client.InterfaceModule ui = io.github.qan4ur.vetmos.module.ModuleManager.get(io.github.qan4ur.vetmos.modules.client.InterfaceModule.class);
        return (ui == null || ui.chatAccent.get() ? Theme.accent() : Theme.WHITE) & 0xFFFFFF;
    }

    public static void send(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;
        mc.inGameHud.getChatHud().addMessage(prefix().append(text));
    }

    public static void info(String message) {
        send(Text.literal(message).formatted(Formatting.GRAY));
    }

    public static void error(String message) {
        send(Text.literal(message).withColor(0xFF5555));
    }

    public static void moduleToggled(Module module) {
        send(Text.literal(module.title + " ").formatted(Formatting.WHITE)
            .append(module.isEnabled()
                ? Text.literal("ВКЛ").withColor(Theme.accent() & 0xFFFFFF).formatted(Formatting.BOLD)
                : Text.literal("ВЫКЛ").formatted(Formatting.DARK_GRAY, Formatting.BOLD)));
    }
}
