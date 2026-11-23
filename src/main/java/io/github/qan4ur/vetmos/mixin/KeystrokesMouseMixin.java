package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.hud.Keystrokes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds raw mouse presses to Keystrokes for the CPS counter (every click counts, not only one per frame). */
@Mixin(Mouse.class)
public abstract class KeystrokesMouseMixin {
    // private void onMouseButton(long window, int button, int action, int mods) - verified with javap (1.21.7 yarn build.1)
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void vetmos$keystrokesClick(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != GLFW.GLFW_PRESS || window != mc.getWindow().getHandle()) return;
        Keystrokes.onMouseClick(button);
    }
}
