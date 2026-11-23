package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.util.Keys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void vetmos$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != GLFW.GLFW_PRESS || window != mc.getWindow().getHandle()) return;
        if (mc.currentScreen != null || mc.player == null) return;
        ModuleManager.onKeyPressed(Keys.mouse(button));
    }
}
