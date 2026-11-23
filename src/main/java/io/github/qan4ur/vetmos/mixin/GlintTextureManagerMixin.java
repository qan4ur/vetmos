package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.render.GlintColor;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * public AbstractTexture getTexture(Identifier) - verified with javap (1.21.7 yarn build.1).
 * Texture render phases resolve their texture through this method whenever a layer is set up,
 * so redirecting the two glint identifiers recolors every glint render layer.
 */
@Mixin(TextureManager.class)
public abstract class GlintTextureManagerMixin {
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void vetmos$glintOnGetTexture(Identifier id, CallbackInfoReturnable<AbstractTexture> cir) {
        if (GlintColor.bypass) return;

        GlintColor module = ModuleManager.get(GlintColor.class);
        if (module == null) return;

        AbstractTexture override = module.getOverride(id);
        if (override != null) cir.setReturnValue(override);
    }
}
