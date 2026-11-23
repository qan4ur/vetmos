package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Shows your own health (and absorption) next to the crosshair, as a number with one decimal or as hearts.
 *
 * <p>The box has a fixed size (room for "88.8" and the absorption line) so the element does not jump when absorption
 * appears. Text is right-aligned, so the top-right corner of the block sits next to the crosshair.</p>
 */
public class CrosshairHpIndicator extends Module implements HudModule {
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier ABSORBING_FULL = Identifier.ofVanilla("hud/heart/absorbing_full");
    private static final Identifier ABSORBING_HALF = Identifier.ofVanilla("hud/heart/absorbing_half");

    /** Default top-right corner of the block relative to the screen center, in GUI pixels. */
    private static final int DEFAULT_RIGHT = -18, DEFAULT_TOP = -10;

    /** Fixed number box: width of "88.8" in the vanilla font (20) + 1 for the shadow; two lines of 9 + 1. */
    private static final int NUMBER_W = 21, LINE_H = 10, NUMBER_H = LINE_H * 2;

    private static final int HEARTS_PER_ROW = 10;

    private static final String G_MAIN = "Основное";
    private static final String G_COLORS = "Цвета";

    private final EnumSetting<DisplayMode> mode = enumeration(G_MAIN, "mode", "Режим", "Показывать здоровье числом или сердечками.", DisplayMode.NUMBER);

    private final BoolSetting absorption = bool(G_MAIN, "absorption", "Поглощение", "Показывает поглощение под здоровьем (только если оно есть).", true);

    private final BoolSetting lowHealth = bool(G_MAIN, "low-health-color", "Цвет при низком HP", "Красит число, когда здоровье на пороге или ниже.", true)
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER);

    private final DoubleSetting lowHealthThreshold = decimal(G_MAIN, "low-health-threshold", "Порог низкого HP",
        "Здоровье, на котором и ниже число становится красным.", 6.0, 0.5, 40.0, 1)
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER && lowHealth.get());

    private final BoolSetting shadow = bool(G_MAIN, "text-shadow", "Тень текста", "Рисует текст с тенью.", true)
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER);

    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting healthColor = color(G_COLORS, "health-color", "Здоровье", "Цвет числа здоровья.", Theme.TEXT)
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER);

    private final ColorSetting lowHealthColor = color(G_COLORS, "low-health-color", "Низкое здоровье", "Цвет числа при низком здоровье.",
        ColorSetting.rgb(255, 40, 40))
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER && lowHealth.get());

    private final ColorSetting absorptionColor = color(G_COLORS, "absorption-color", "Поглощение", "Цвет числа поглощения.", Theme.GOLD)
        .visibleWhen(() -> mode.get() == DisplayMode.NUMBER && absorption.get());

    public CrosshairHpIndicator() {
        super("crosshair-hp-indicator", "HP у прицела", "Показывает ваше здоровье и поглощение рядом с прицелом.", Category.HUD);
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public boolean shouldRenderHud() {
        return mc.player != null && mc.currentScreen == null && !mc.player.isSpectator();
    }

    @Override
    public double getHudWidth() {
        if (mode.get() == DisplayMode.HEARTS) return HEARTS_PER_ROW * 8 + 1;
        return NUMBER_W;
    }

    @Override
    public double getHudHeight() {
        if (mode.get() == DisplayMode.HEARTS) return healthRows() * 10 + 10 - 1;
        return NUMBER_H;
    }

    private int healthRows() {
        float max = mc != null && mc.player != null ? mc.player.getMaxHealth() : 20f;
        int containers = Math.max(1, (int) Math.ceil(max / 2f));
        return (containers + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
    }

    /** Box center = top-right corner (-18, -10) from the screen center, shifted by (-w/2, +h/2). */
    @Override
    public HudPosition defaultHudPosition() {
        double w = getHudWidth() * scale.get();
        double h = getHudHeight() * scale.get();
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.CENTER, DEFAULT_RIGHT - w / 2.0, DEFAULT_TOP + h / 2.0);
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        float health, abs, maxHealth;
        if (mc.player != null) {
            health = mc.player.getHealth();
            abs = mc.player.getAbsorptionAmount();
            maxHealth = mc.player.getMaxHealth();
        } else {
            // Preview without a world.
            health = 17.5f;
            abs = 4f;
            maxHealth = 20f;
        }

        if (inEditor && mc.player != null && abs <= 0) abs = 4f; // show where the absorption line goes

        if (mode.get() == DisplayMode.HEARTS) renderHearts(context, health, abs, maxHealth);
        else renderNumber(context, health, abs);
    }

    private void renderNumber(DrawContext context, float health, float abs) {
        TextRenderer tr = mc.textRenderer;
        int boxW = NUMBER_W;

        String hpText = format(health);
        int hpColor = lowHealth.get() && health <= lowHealthThreshold.get() ? lowHealthColor.get() : healthColor.get();
        context.drawText(tr, hpText, boxW - 1 - tr.getWidth(hpText), 0, hpColor, shadow.get());

        if (absorption.get() && abs > 0) {
            String absText = format(abs);
            context.drawText(tr, absText, boxW - 1 - tr.getWidth(absText), LINE_H, absorptionColor.get(), shadow.get());
        }
    }

    private void renderHearts(DrawContext context, float health, float abs, float maxHealth) {
        int boxW = (int) getHudWidth();

        int containers = Math.max(1, (int) Math.ceil(maxHealth / 2f));
        int halves = (int) Math.ceil(health); // vanilla rounds health up to half hearts
        int rows = (containers + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;

        for (int i = 0; i < containers; i++) {
            int row = i / HEARTS_PER_ROW;
            int col = i % HEARTS_PER_ROW;
            int inRow = Math.min(HEARTS_PER_ROW, containers - row * HEARTS_PER_ROW);
            int x = boxW - inRow * 8 - 1 + col * 8; // right-aligned row
            int y = row * 10;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9);
            int value = halves - i * 2;
            if (value >= 2) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, y, 9, 9);
            else if (value == 1) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_HALF, x, y, 9, 9);
        }

        if (absorption.get() && abs > 0) {
            int absHalves = (int) Math.ceil(abs);
            int absHearts = Math.min(HEARTS_PER_ROW, (absHalves + 1) / 2);
            int y = rows * 10;

            for (int i = 0; i < absHearts; i++) {
                int x = boxW - absHearts * 8 - 1 + i * 8;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9);
                int value = absHalves - i * 2;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, value >= 2 ? ABSORBING_FULL : ABSORBING_HALF, x, y, 9, 9);
            }
        }
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public enum DisplayMode {
        NUMBER("Число"),
        HEARTS("Сердечки");

        private final String title;

        DisplayMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:DisplayMode." + name(), title);
        }
    }
}
