package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1):
 * private void onMouseScroll(long window, double horizontal, double vertical) - consumed while zooming (no hotbar scroll);
 * private void updateMouse(double timeDelta) ends with ClientPlayerEntity.changeLookDirection(DD)V (single call),
 * whose two arguments (yaw / pitch deltas, after the smooth camera) are scaled while zooming.
 */
@Mixin(Mouse.class)
public abstract class ZoomMouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void vetmos$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (window != mc.getWindow().getHandle() || mc.currentScreen != null || mc.player == null || mc.getOverlay() != null) return;
        if (Zoom.handleScroll(vertical)) ci.cancel();
    }

    @ModifyArg(method = "updateMouse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
        index = 0)
    private double vetmos$zoomSensitivityX(double dx) {
        return dx * Zoom.sensitivityMultiplier();
    }

    @ModifyArg(method = "updateMouse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
        index = 1)
    private double vetmos$zoomSensitivityY(double dy) {
        return dy * Zoom.sensitivityMultiplier();
    }
}
