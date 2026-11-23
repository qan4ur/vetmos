package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;

/** Name of the biome at the player's feet (in the game language). */
public class BiomeHud extends InfoHudModule {
    public BiomeHud() {
        super("biome-hud", "Биом", "Показывает название биома, в котором вы стоите.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.TOP, -4, 23);
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        out.add(new Line(label("biome", "Биом"), preview ? previewName() : currentBiomeName()));
    }

    static String previewName() {
        return Text.translatable("biome.minecraft.plains").getString();
    }

    /** Localized name of the biome at the player's block position, via the biome's translation key. */
    static String currentBiomeName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return "?";
        RegistryEntry<Biome> entry = mc.world.getBiome(mc.player.getBlockPos());
        Optional<RegistryKey<Biome>> key = entry.getKey();
        if (key.isEmpty()) return "?";
        return Text.translatable(Util.createTranslationKey("biome", key.get().getValue())).getString();
    }
}
