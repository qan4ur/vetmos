package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified with javap (1.21.7 yarn build.1):
 * private float getFov(Camera, float tickDelta, boolean changingFov) - only the world FOV (changingFov = true) is zoomed;
 * private void renderHand(float tickDelta, boolean sleeping, Matrix4f) - cancelled while zooming to hide the hand.
 */
@Mixin(GameRenderer.class)
public abstract class ZoomGameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void vetmos$zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (!changingFov) return;
        double divisor = Zoom.fovDivisor();
        if (divisor != 1) cir.setReturnValue((float) (cir.getReturnValueF() / divisor));
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void vetmos$zoomHideHand(float tickDelta, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        if (Zoom.shouldHideHand()) ci.cancel();
    }
}
