package io.github.qan4ur.vetmos.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.world.WorldBorder;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1), WorldBorderRendering:
 * public void render(net.minecraft.world.border.WorldBorder, Vec3d, double viewDistanceBlocks, double farPlaneDistance)
 * - color: single call to WorldBorderStage.getColor()I
 * - scrolling animation: single call to Util.getMeasuringTimeMs()J
 * - alpha: single call to MathHelper.clamp(DDD)D (distance based fade)
 */
@Mixin(WorldBorderRendering.class)
public abstract class WorldBorderRenderingMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vetmos$borderOnRender(net.minecraft.world.border.WorldBorder border, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance, CallbackInfo ci) {
        WorldBorder module = ModuleManager.get(WorldBorder.class);
        if (module != null && module.hide()) ci.cancel();
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorderStage;getColor()I"))
    private int vetmos$borderModifyColor(int original) {
        WorldBorder module = ModuleManager.get(WorldBorder.class);
        return module != null ? module.getColor(original) : original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    private long vetmos$borderFreezeAnimation(long original) {
        WorldBorder module = ModuleManager.get(WorldBorder.class);
        return module != null && module.isStatic() ? 0L : original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"))
    private double vetmos$borderModifyAlpha(double original) {
        WorldBorder module = ModuleManager.get(WorldBorder.class);
        return module != null ? module.getAlpha(original) : original;
    }
}
