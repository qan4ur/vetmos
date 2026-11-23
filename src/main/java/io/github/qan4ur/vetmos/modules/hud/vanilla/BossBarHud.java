package io.github.qan4ur.vetmos.modules.hud.vanilla;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.mixin.BossBarHudBarsAccessor;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Movable replacement of the vanilla boss bars. Draws the bars itself with the vanilla sprites (same layout as
 * vanilla: name above, 182x5 bar, 19 px per bar), so names can be hidden and the number of bars limited.
 */
public class BossBarHud extends Module implements HudModule {
    private static final String G = "Основное";

    private static final int BAR_W = 182, BAR_H = 5;

    // Same sprite ids and order as vanilla BossBarHud (BossBar.Color / BossBar.Style ordinals), verified with javap
    private static final Identifier[] BACKGROUND = ids("pink_background", "blue_background", "red_background", "green_background",
        "yellow_background", "purple_background", "white_background");
    private static final Identifier[] PROGRESS = ids("pink_progress", "blue_progress", "red_progress", "green_progress",
        "yellow_progress", "purple_progress", "white_progress");
    private static final Identifier[] NOTCHED_BACKGROUND = ids("notched_6_background", "notched_10_background", "notched_12_background", "notched_20_background");
    private static final Identifier[] NOTCHED_PROGRESS = ids("notched_6_progress", "notched_10_progress", "notched_12_progress", "notched_20_progress");

    private final IntSetting maxBars = integer(G, "max-bars", "Макс. полос", "Сколько боссбаров показывать одновременно.", 6, 1, 20);
    private final BoolSetting hideNames = bool(G, "hide-names", "Скрыть названия", "Не показывать названия над полосами.", false);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать названия с тенью.", true);
    private final IntSetting spacing = integer(G, "spacing", "Отступ", "Расстояние между полосами.", 5, 0, 20);
    private final DoubleSetting scale = hudScaleSetting();

    private record Bar(Text name, float percent, BossBar.Color color, BossBar.Style style) {}

    private final List<Bar> bars = new ArrayList<>();
    private int boxWidth = BAR_W;

    public BossBarHud() {
        super("boss-bar-hud", "Боссбар", "Перемещаемые полосы здоровья боссов с настройкой размера.", Category.HUD);
    }

    private static Identifier[] ids(String... names) {
        Identifier[] out = new Identifier[names.length];
        for (int i = 0; i < names.length; i++) out[i] = Identifier.ofVanilla("boss_bar/" + names[i]);
        return out;
    }

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, original -> (context, tickCounter) -> {
            if (!isEnabled()) original.render(context, tickCounter);
        });
    }

    // ---------------------------------------------------------------- data

    private void refresh(boolean editor) {
        bars.clear();
        if (mc.inGameHud != null) {
            for (ClientBossBar bar : ((BossBarHudBarsAccessor) mc.inGameHud.getBossBarHud()).vetmos$getBossBars().values()) {
                if (bars.size() >= maxBars.get()) break;
                bars.add(new Bar(bar.getName(), bar.getPercent(), bar.getColor(), bar.getStyle()));
            }
        }
        if (bars.isEmpty() && editor) {
            bars.add(new Bar(Text.literal(io.github.qan4ur.vetmos.util.Lang.tr("hud:sample-boss", "Дракон Края")), 0.7f, BossBar.Color.PURPLE, BossBar.Style.PROGRESS));
        }
        int w = BAR_W;
        if (!hideNames.get()) for (Bar bar : bars) w = Math.max(w, mc.textRenderer.getWidth(bar.name));
        boxWidth = w;
    }

    private int nameHeight() {
        return hideNames.get() ? 0 : 9;
    }

    private int rowHeight() {
        // vanilla: 9 px name + 5 px bar + 5 px gap = 19
        return nameHeight() + BAR_H + spacing.get();
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        // vanilla: first name at y = 3, bar at y = 12, centered
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.TOP, 0, 3);
    }

    @Override
    public boolean shouldRenderHud() {
        refresh(false);
        return !bars.isEmpty();
    }

    @Override
    public double getHudWidth() {
        refresh(HudManager.isEditorOpen());
        return boxWidth;
    }

    @Override
    public double getHudHeight() {
        int n = Math.max(1, bars.size());
        return (n - 1) * rowHeight() + nameHeight() + BAR_H;
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        TextRenderer tr = mc.textRenderer;
        int barX = (boxWidth - BAR_W) / 2;
        int y = 0;
        for (Bar bar : bars) {
            if (!hideNames.get()) {
                context.drawText(tr, bar.name, (boxWidth - tr.getWidth(bar.name)) / 2, y, 0xFFFFFFFF, shadow.get());
            }
            int barY = y + nameHeight();
            drawBar(context, barX, barY, bar, BAR_W, BACKGROUND, NOTCHED_BACKGROUND);
            int progress = MathHelper.lerpPositive(bar.percent, 0, BAR_W);
            if (progress > 0) drawBar(context, barX, barY, bar, progress, PROGRESS, NOTCHED_PROGRESS);
            y += rowHeight();
        }
    }

    private static void drawBar(DrawContext context, int x, int y, Bar bar, int width, Identifier[] plain, Identifier[] notched) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, plain[bar.color.ordinal()], BAR_W, BAR_H, 0, 0, x, y, width, BAR_H);
        if (bar.style != BossBar.Style.PROGRESS) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, notched[bar.style.ordinal() - 1], BAR_W, BAR_H, 0, 0, x, y, width, BAR_H);
        }
    }
}
