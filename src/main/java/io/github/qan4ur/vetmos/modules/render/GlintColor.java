package io.github.qan4ur.vetmos.modules.render;

import com.mojang.blaze3d.textures.GpuTexture;
import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Optional;

/**
 * Recolors the enchantment glint. Builds tinted copies of the vanilla glint textures (brightness x color) and
 * GlintTextureManagerMixin hands them out from TextureManager.getTexture() for the two glint identifiers.
 * The vanilla textures are never replaced or closed, so disabling just stops the redirect. A resource reload
 * recreates the vanilla GpuTexture, which is detected each tick and rebuilt from the new resource.
 */
public class GlintColor extends Module {
    private static final String GENERAL = "Основное";
    private static final String COLORS = "Цвета";

    // General

    private final BoolSetting items = bool(GENERAL, "items", "Предметы",
        "Перекрашивать блеск на предметах (в руке, на земле, в интерфейсе).", true);

    private final BoolSetting armor = bool(GENERAL, "armor-and-entities", "Броня и сущности",
        "Перекрашивать блеск на надетой броне, элитрах, трезубцах и щитах.", true);

    private final DoubleSetting brightness = decimal(GENERAL, "brightness", "Яркость",
        "Интенсивность окрашенного блеска.", 1.2, 0.1, 3, 2);

    // Colors

    private final ColorSetting itemColor = color(COLORS, "item-color", "Цвет предметов",
        "Цвет блеска на предметах.", ColorSetting.rgb(240, 240, 240))
        .visibleWhen(items::get);

    private final BoolSetting sameColor = bool(COLORS, "armor-uses-item-color", "Броня как предметы",
        "Использовать цвет предметов и для брони.", true)
        .visibleWhen(armor::get);

    private final ColorSetting armorColor = color(COLORS, "armor-color", "Цвет брони",
        "Цвет блеска на броне и сущностях.", ColorSetting.rgb(130, 0, 15))
        .visibleWhen(() -> armor.get() && !sameColor.get());

    /** Set while the vanilla texture is read so the mixin doesn't redirect our own lookup. */
    public static boolean bypass;

    private final Tinted item = new Tinted(ItemRenderer.ITEM_ENCHANTMENT_GLINT, "vetmos_glint_item");
    private final Tinted entity = new Tinted(ItemRenderer.ENTITY_ENCHANTMENT_GLINT, "vetmos_glint_entity");
    private boolean dirty = true;
    private int lastItemColor, lastArmorColor;

    public GlintColor() {
        super("glint-color", "Цвет зачарования", "Перекрашивает блеск зачарования на предметах и броне.", Category.RENDER);
        items.onChange(this::markDirty);
        armor.onChange(this::markDirty);
        brightness.onChange(this::markDirty);
        itemColor.onChange(this::markDirty);
        sameColor.onChange(this::markDirty);
        armorColor.onChange(this::markDirty);
    }

    @Override
    protected void onEnable() {
        // Built on the next tick (the module can be enabled from the config before the render system is up)
        dirty = true;
    }

    @Override
    protected void onDisable() {
        item.close();
        entity.close();
    }

    private void markDirty() {
        dirty = true;
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;

        int ic = itemColor.get();
        int ac = sameColor.get() ? ic : armorColor.get();
        if (ic != lastItemColor || ac != lastArmorColor) dirty = true;

        boolean rebuild = dirty;
        dirty = false;

        if (items.get()) item.update(ic, rebuild);
        else item.close();

        if (armor.get()) entity.update(ac, rebuild);
        else entity.close();

        lastItemColor = ic;
        lastArmorColor = ac;
    }

    /** Called from GlintTextureManagerMixin. Returns null to use vanilla. */
    public AbstractTexture getOverride(Identifier id) {
        if (bypass || !isEnabled()) return null;
        if (id.equals(item.id)) return item.texture;
        if (id.equals(entity.id)) return entity.texture;
        return null;
    }

    private class Tinted {
        final Identifier id;
        final String name;
        NativeImageBackedTexture texture;
        GpuTexture source;
        boolean failed;

        Tinted(Identifier id, String name) {
            this.id = id;
            this.name = name;
        }

        void update(int color, boolean rebuild) {
            GpuTexture current = vanillaGpuTexture();
            if (current != source) {
                // First build or the resource pack was reloaded
                rebuild = true;
                failed = false;
            }
            if (failed || (!rebuild && texture != null)) return;

            source = current;
            close();

            try {
                Optional<Resource> resource = mc.getResourceManager().getResource(id);
                if (resource.isEmpty()) {
                    failed = true;
                    return;
                }

                NativeImage image;
                try (InputStream in = resource.get().getInputStream()) {
                    image = NativeImage.read(in);
                }

                tint(image, color);

                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, image);
                tex.setClamp(false);        // the glint scrolls, needs REPEAT
                tex.setFilter(true, false); // vanilla glint is blurred
                texture = tex;
            } catch (Throwable t) {
                failed = true;
                Vetmos.LOG.error("GlintColor: failed to build tinted glint for {}", id, t);
            }
        }

        private void tint(NativeImage image, int color) {
            double mul = brightness.get();
            int cr = color >> 16 & 255, cg = color >> 8 & 255, cb = color & 255;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int argb = image.getColorArgb(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Brightest channel keeps the glint's strength (the vanilla glint is purple, plain luminance would be too dark)
                    double l = Math.max(r, Math.max(g, b)) / 255.0 * mul;

                    image.setColorArgb(x, y, (a << 24) | (clamp(cr * l) << 16) | (clamp(cg * l) << 8) | clamp(cb * l));
                }
            }
        }

        private int clamp(double v) {
            return (int) Math.max(0, Math.min(255, Math.round(v)));
        }

        private GpuTexture vanillaGpuTexture() {
            bypass = true;
            try {
                return mc.getTextureManager().getTexture(id).getGlTexture();
            } catch (Throwable t) {
                return null;
            } finally {
                bypass = false;
            }
        }

        void close() {
            if (texture != null) {
                texture.close();
                texture = null;
            }
        }
    }
}
