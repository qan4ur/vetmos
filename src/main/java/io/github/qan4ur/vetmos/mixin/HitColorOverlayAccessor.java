package io.github.qan4ur.vetmos.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverlayTexture.class)
public interface HitColorOverlayAccessor {
    // private final NativeImageBackedTexture texture (verified with javap, 1.21.7 yarn build.1)
    @Accessor("texture")
    NativeImageBackedTexture vetmos$getTexture();
}
