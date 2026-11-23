package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.world.CustomSkyFog;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified with javap (1.21.7 yarn build.1):
 * - ClientWorld: public int getSkyColor(Vec3d, float)
 * - WorldAccess: public default long getLunarTime(); neither World nor ClientWorld overrides it.
 *   getSkyAngle / getMoonPhase use getLunarTime(), so overriding it here only changes the rendered sun/moon/sky.
 *   The level properties' timeOfDay is left alone, so the real time keeps ticking.
 * The mixin implements WorldAccess so the added getLunarTime() is an override and gets remapped.
 */
@Mixin(ClientWorld.class)
public abstract class SkyClientWorldMixin implements WorldAccess {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void vetmos$skyOnGetSkyColor(Vec3d cameraPos, float tickProgress, CallbackInfoReturnable<Integer> cir) {
        CustomSkyFog module = ModuleManager.get(CustomSkyFog.class);
        if (module == null) return;

        Integer color = module.getSkyColor();
        if (color != null) cir.setReturnValue(color);
    }

    @Override
    public long getLunarTime() {
        CustomSkyFog module = ModuleManager.get(CustomSkyFog.class);
        if (module != null) {
            long fixed = module.getFixedTime();
            if (fixed >= 0) return fixed;
        }

        return ((ClientWorld) (Object) this).getLevelProperties().getTimeOfDay();
    }
}
