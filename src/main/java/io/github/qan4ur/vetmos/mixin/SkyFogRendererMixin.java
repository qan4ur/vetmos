package io.github.qan4ur.vetmos.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.world.CustomSkyFog;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

/**
 * Verified with javap (1.21.7 yarn build.1), net.minecraft.client.render.fog.FogRenderer:
 * - private Vector4f getFogColor(Camera, float, ClientWorld, int, float, boolean): both the fog color and
 *   the value returned by the public applyFog (clear / sky background color).
 * - public Vector4f applyFog(Camera, int, boolean, RenderTickCounter, float, ClientWorld) calls once
 *   private void applyFog(ByteBuffer, int, Vector4f, float envStart, float envEnd, float renderDistStart, float renderDistEnd, float skyEnd, float cloudEnd)
 *   and has exactly one CameraSubmersionType local (slot 10, from getCameraSubmersionType).
 */
@Mixin(FogRenderer.class)
public abstract class SkyFogRendererMixin {
    @Unique
    private static final float VETMOS_NO_FOG = 1.0E7f;

    @ModifyReturnValue(method = "getFogColor", at = @At("RETURN"))
    private Vector4f vetmos$skyModifyFogColor(Vector4f original, @Local(argsOnly = true) Camera camera) {
        CustomSkyFog module = ModuleManager.get(CustomSkyFog.class);
        if (module == null) return original;
        return module.modifyFogColor(original, camera.getSubmersionType());
    }

    @WrapOperation(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void vetmos$skyModifyFogDistance(FogRenderer instance, ByteBuffer buffer, int index, Vector4f color,
                                             float envStart, float envEnd, float renderDistStart, float renderDistEnd, float skyEnd, float cloudEnd,
                                             Operation<Void> original, @Local CameraSubmersionType submersion) {
        CustomSkyFog module = ModuleManager.get(CustomSkyFog.class);

        if (module != null && module.shouldModifyFogDistance(submersion)) {
            if (module.disableFog()) {
                envStart = renderDistStart = VETMOS_NO_FOG;
                envEnd = renderDistEnd = skyEnd = cloudEnd = VETMOS_NO_FOG;
            } else {
                float m = module.fogMultiplier();
                envStart *= m;
                envEnd *= m;
                renderDistStart *= m;
                renderDistEnd *= m;
                skyEnd *= m;
                cloudEnd *= m;
            }
        }

        original.call(instance, buffer, index, color, envStart, envEnd, renderDistStart, renderDistEnd, skyEnd, cloudEnd);
    }
}
