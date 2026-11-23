package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void vetmos$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != GLFW.GLFW_PRESS || window != mc.getWindow().getHandle()) return;
        if (mc.currentScreen != null || mc.player == null) return;
        ModuleManager.onKeyPressed(key);
    }
}
