package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.Hitboxes;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1): the private instance method
 * {@code renderHitboxes(MatrixStack, EntityRenderState, EntityHitboxAndView, VertexConsumerProvider)} draws the F3+B
 * hitbox of one entity (called from the private render(...) when {@code state.hitbox != null}). It is overloaded with a
 * private static {@code renderHitboxes(MatrixStack, EntityHitboxAndView, VertexConsumer, float)}, hence the full descriptor.
 * While the Hitboxes module is on, the vanilla drawing is skipped and the module draws its own boxes.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxesEntityRenderDispatcherMixin {
    @Inject(method = "renderHitboxes(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"), cancellable = true)
    private void vetmos$hitboxesCancelVanilla(MatrixStack matrices, EntityRenderState state, EntityHitboxAndView hitbox,
                                              VertexConsumerProvider consumers, CallbackInfo ci) {
        if (Hitboxes.replacesVanilla()) ci.cancel();
    }
}
