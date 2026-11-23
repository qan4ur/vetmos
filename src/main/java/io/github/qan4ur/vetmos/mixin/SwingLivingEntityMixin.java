package io.github.qan4ur.vetmos.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.render.SwingAnimations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class SwingLivingEntityMixin {
    // private int getHandSwingDuration() - verified with javap (1.21.7 yarn build.1), 3 return points, all modified
    @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
    private int vetmos$swingModifyDuration(int original) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || (Object) this != mc.player) return original;

        SwingAnimations module = ModuleManager.get(SwingAnimations.class);
        return module != null ? module.modifySwingDuration(original) : original;
    }
}
