package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.modules.render.NoHurtCam;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Verified with javap (1.21.7 yarn build.1): private void tiltViewWhenHurt(MatrixStack, float) calls
 * RotationAxis.rotationDegrees(F) four times: ordinal 0 = death roll (Z), 1 = -damageTiltYaw (Y),
 * 2 = the hurt tilt (Z, -sin * 14 * damageTiltStrength), 3 = +damageTiltYaw (Y). Only ordinal 2 is scaled;
 * the two yaw rotations cancel each other out.
 */
@Mixin(GameRenderer.class)
public abstract class NoHurtCamGameRendererMixin {
    @ModifyArg(method = "tiltViewWhenHurt",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2),
        index = 0)
    private float vetmos$noHurtCamScale(float degrees) {
        return degrees * NoHurtCam.tiltMultiplier();
    }
}
