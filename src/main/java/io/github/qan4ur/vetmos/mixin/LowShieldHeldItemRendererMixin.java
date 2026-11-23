package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.LowShield;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1): public void renderItem(LivingEntity, ItemStack, ItemDisplayContext, MatrixStack,
 * VertexConsumerProvider, int). renderFirstPersonItem calls it inside its own push/pop with FIRST_PERSON_* contexts, so a
 * transform applied at its HEAD is undone by the caller.
 * This injects into renderItem itself (not renderFirstPersonItem), so it does not touch the injection points of
 * SwingHeldItemRendererMixin; the swing transform is applied first (before the call), then this one.
 */
@Mixin(HeldItemRenderer.class)
public abstract class LowShieldHeldItemRendererMixin {
    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"))
    private void vetmos$lowShieldTransform(LivingEntity entity, ItemStack stack, ItemDisplayContext context, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        LowShield.transform(stack, context, matrices);
    }
}
