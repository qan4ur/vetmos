package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.render.SwingAnimations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1):
 * - private void renderFirstPersonItem(AbstractClientPlayerEntity, float, float, Hand, float, ItemStack, float, MatrixStack, VertexConsumerProvider, int)
 *   has 2 calls to renderItem(LivingEntity, ItemStack, ItemDisplayContext, MatrixStack, VertexConsumerProvider, int)
 *   and 1 call to renderArmHoldingItem(MatrixStack, VertexConsumerProvider, int, float, float, Arm).
 * - public void updateHeldItems(); fields client, mainHand, offHand, equipProgressMainHand, lastEquipProgressMainHand,
 *   equipProgressOffHand, lastEquipProgressOffHand.
 */
@Mixin(HeldItemRenderer.class)
public abstract class SwingHeldItemRendererMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float lastEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float lastEquipProgressOffHand;

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void vetmos$swingOnRenderItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingAnimations module = vetmos$swingModule();
        if (module != null) module.transformItem(hand, matrices);
    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V"))
    private void vetmos$swingOnRenderArm(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingAnimations module = vetmos$swingModule();
        if (module != null) module.transformEmptyHand(matrices);
    }

    /**
     * Runs after vanilla updated the equip progress. Recomputes it without the "switch item" drop
     * (no-swap-animation) and/or without the attack-cooldown dip.
     */
    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void vetmos$swingAfterUpdateHeldItems(CallbackInfo ci) {
        SwingAnimations module = vetmos$swingModule();
        if (module == null) return;

        boolean noSwap = module.noSwapAnimation();
        boolean noDip = module.noCooldownDip();
        if (!noSwap && !noDip) return;

        ClientPlayerEntity player = client.player;
        if (player == null || player.isRiding()) return;

        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();

        if (noSwap) {
            mainHand = main;
            offHand = off;
        }

        // Same formula as vanilla, but the target only drops to 0 for a real swap that we are not skipping
        float cooldown = player.getAttackCooldownProgress(1f);
        float mainTarget = (!noSwap && mainHand != main) ? 0 : (noDip ? 1 : cooldown * cooldown * cooldown);
        float offTarget = (!noSwap && offHand != off) ? 0 : 1;

        equipProgressMainHand = lastEquipProgressMainHand + MathHelper.clamp(mainTarget - lastEquipProgressMainHand, -0.4f, 0.4f);
        equipProgressOffHand = lastEquipProgressOffHand + MathHelper.clamp(offTarget - lastEquipProgressOffHand, -0.4f, 0.4f);

        if (noSwap) {
            // Hands stay fully raised, no raise animation either
            equipProgressOffHand = lastEquipProgressOffHand = 1;
            if (noDip) equipProgressMainHand = lastEquipProgressMainHand = 1;
        }
    }

    @Unique
    private static SwingAnimations vetmos$swingModule() {
        SwingAnimations module = ModuleManager.get(SwingAnimations.class);
        return module != null && module.isEnabled() ? module : null;
    }
}
