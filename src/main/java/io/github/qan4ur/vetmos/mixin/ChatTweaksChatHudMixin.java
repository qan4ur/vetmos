package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.client.ChatTweaks;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Chat timestamps + stacking of identical consecutive messages.
 * Verified with javap (1.21.7 yarn build.1):
 * - public void addMessage(Text, MessageSignatureData, MessageIndicator): builds a ChatHudLine, then logChatMessage,
 *   addVisibleMessage(ChatHudLine) and private addMessage(ChatHudLine).
 * - private addVisibleMessage inserts the wrapped lines at index 0 of visibleMessages one by one, so the LAST wrapped line
 *   (endOfEntry = true) ends up at index 0 and the message's other lines follow with endOfEntry = false.
 * - private addMessage(ChatHudLine) inserts at index 0 of messages.
 * - fields: private final List<ChatHudLine> messages, private final List<ChatHudLine.Visible> visibleMessages, private int scrolledLines.
 * - public void clear(boolean).
 */
@Mixin(ChatHud.class)
public abstract class ChatTweaksChatHudMixin {
    @Shadow @Final private List<ChatHudLine> messages;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow private int scrolledLines;

    /** Undecorated text of the newest line and that line itself (identity is checked before stacking). */
    @Unique private Text vetmos$chatLastRaw;
    @Unique private ChatHudLine vetmos$chatLastLine;
    @Unique private int vetmos$chatLastCount = 1;
    @Unique private Text vetmos$chatPendingRaw;
    @Unique private int vetmos$chatPendingCount = 1;

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), argsOnly = true)
    private Text vetmos$chatTweaksDecorate(Text message) {
        vetmos$chatPendingRaw = null;
        vetmos$chatPendingCount = 1;
        ChatTweaks module = ChatTweaks.active();
        if (module == null || message == null) return message;

        try {
            int count = 1;
            if (module.stackingEnabled() && vetmos$chatLastRaw != null && vetmos$chatLastRaw.equals(message) && vetmos$chatRemoveLatest()) {
                count = vetmos$chatLastCount + 1;
            }
            vetmos$chatPendingRaw = message;
            vetmos$chatPendingCount = count;
            return module.decorate(message, count);
        } catch (RuntimeException e) {
            vetmos$chatPendingRaw = null;
            return message;
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At("TAIL"))
    private void vetmos$chatTweaksRemember(ChatHudLine line, CallbackInfo ci) {
        vetmos$chatLastLine = line;
        vetmos$chatLastRaw = vetmos$chatPendingRaw;
        vetmos$chatLastCount = vetmos$chatPendingCount;
        vetmos$chatPendingRaw = null;
        vetmos$chatPendingCount = 1;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void vetmos$chatTweaksClear(boolean clearHistory, CallbackInfo ci) {
        vetmos$chatLastRaw = null;
        vetmos$chatLastLine = null;
        vetmos$chatLastCount = 1;
    }

    /**
     * Removes the newest message and its visible lines, but only when both lists still look exactly as we left them.
     * Returns false (and changes nothing) otherwise.
     */
    @Unique
    private boolean vetmos$chatRemoveLatest() {
        ChatHudLine last = vetmos$chatLastLine;
        if (last == null || messages.isEmpty() || messages.get(0) != last) return false;
        if (visibleMessages.isEmpty()) return false;
        ChatHudLine.Visible top = visibleMessages.get(0);
        if (!top.endOfEntry() || top.addedTime() != last.creationTick()) return false;

        // Count the lines of the newest entry: index 0 (endOfEntry) + following lines until the previous entry's end
        int n = 1;
        while (n < visibleMessages.size()) {
            ChatHudLine.Visible v = visibleMessages.get(n);
            if (v.endOfEntry() || v.addedTime() != last.creationTick()) break;
            n++;
        }

        messages.remove(0);
        for (int i = 0; i < n; i++) visibleMessages.remove(0);
        if (scrolledLines > 0) scrolledLines = Math.max(0, scrolledLines - n);
        return true;
    }
}
