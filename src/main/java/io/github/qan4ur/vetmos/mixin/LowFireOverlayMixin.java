package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.LowFire;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1): private static void renderFireOverlay(MatrixStack, VertexConsumerProvider),
 * single return; draws two quads with VertexConsumer.color(FFFF) (alpha 0.9f) four times each in a loop, and does its own
 * push/translate(.., -0.3, 0)/pop per quad.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class LowFireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void vetmos$lowFireHead(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (LowFire.shouldHide()) {
            ci.cancel();
            return;
        }
        matrices.push();
        float offset = LowFire.offset();
        if (offset != 0) matrices.translate(0f, -offset, 0f);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void vetmos$lowFireReturn(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        matrices.pop();
    }

    @ModifyArg(method = "renderFireOverlay",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"),
        index = 3)
    private static float vetmos$lowFireAlpha(float alpha) {
        return LowFire.alpha(alpha);
    }
}
