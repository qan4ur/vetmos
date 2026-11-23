package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/** Player coordinates, optionally with the matching nether / overworld position and the biome. */
public class CoordsHud extends InfoHudModule {
    public enum Style {
        ONE_LINE("В строку"), LINES("По строкам");

        private final String title;

        Style(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:CoordsHud.Style." + name(), title); }
    }

    private final EnumSetting<Style> style = enumeration(G, "style", "Вид", "Все координаты в одной строке или каждая в своей.", Style.ONE_LINE);
    private final IntSetting decimals = integer(G, "decimals", "Знаков после запятой", "0 — координаты блока.", 0, 0, 2);
    private final BoolSetting dimension = bool(G, "dimension-coords", "Координаты другого мира",
        "В обычном мире показывать координаты в Аду (/8), в Аду — в обычном мире (x8).", true);
    private final BoolSetting biome = bool(G, "biome", "Биом", "Добавить строку с биомом.", false);

    public CoordsHud() {
        super("coords-hud", "Координаты", "Показывает координаты XYZ, координаты другого мира и биом.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.LEFT, HudPosition.AnchorY.TOP, 4, 42);
    }

    private String num(double v) {
        int d = decimals.get();
        if (d == 0) return String.valueOf((int) Math.floor(v));
        return String.format(Locale.ROOT, "%." + d + "f", v);
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        double x, y, z;
        boolean nether, overworld;
        if (preview) {
            x = 128.4; y = 64; z = -312.7;
            nether = false;
            overworld = true;
        } else {
            x = mc.player.getX();
            y = mc.player.getY();
            z = mc.player.getZ();
            nether = mc.world.getRegistryKey() == World.NETHER;
            overworld = mc.world.getRegistryKey() == World.OVERWORLD;
        }

        if (style.get() == Style.ONE_LINE) {
            out.add(new Line("XYZ", num(x) + "  " + num(y) + "  " + num(z)));
        } else {
            out.add(new Line("X", num(x)));
            out.add(new Line("Y", num(y)));
            out.add(new Line("Z", num(z)));
        }

        if (dimension.get() && (nether || overworld)) {
            double k = nether ? 8 : 1 / 8.0;
            String lbl = nether ? label("overworld", "Мир") : label("nether", "Ад");
            out.add(new Line(lbl, num(x * k) + "  " + num(z * k), textColor.get() & 0x00FFFFFF | 0xB0000000));
        }

        if (biome.get()) out.add(new Line(label("biome", "Биом"), preview ? BiomeHud.previewName() : BiomeHud.currentBiomeName()));
    }
}
