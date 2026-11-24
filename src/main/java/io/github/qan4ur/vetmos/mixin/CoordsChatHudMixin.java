package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.util.ChatCoords;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Clickable coordinates in chat (click = copy to clipboard). Built in, always on. */
@Mixin(value = ChatHud.class, priority = 900)
public abstract class CoordsChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), argsOnly = true)
    private Text vetmos$linkCoords(Text message) {
        try {
            return ChatCoords.link(message);
        } catch (RuntimeException e) {
            return message;
        }
    }
}
