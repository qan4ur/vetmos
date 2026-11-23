package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the vanilla crosshair and the attack indicator under it with versions drawn in their real colors
 * (vanilla uses an inverting blend that disappears on grey backgrounds).
 *
 * <p>The vanilla crosshair HUD element is wrapped in {@link #onInit()}: while this module is enabled and its HUD
 * element will be drawn, vanilla is skipped. With the debug screen (F3) open vanilla draws its axes as usual.</p>
 *
 * <p>Everything is drawn around one "center pixel" in the middle of an odd-sized box, so at scale 1 the shapes are
 * pixel-exact and sit exactly where the vanilla crosshair is.</p>
 */
public class CustomCrosshair extends Module implements HudModule {
    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");
    private static final Identifier INDICATOR_FULL_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_full");
    private static final Identifier INDICATOR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_background");
    private static final Identifier INDICATOR_PROGRESS_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_progress");

    private static final long FLASH_MS = 250;

    private static final String G_MAIN = "Основное";
    private static final String G_CUSTOM = "Своя форма";
    private static final String G_DYNAMIC = "Динамика";
    private static final String G_INDICATOR = "Индикатор атаки";
    private static final String G_COLORS = "Цвета";

    // ---------------------------------------------------------------- general

    private final BoolSetting textures = bool(G_MAIN, "resource-pack-textures", "Текстуры ресурспака",
        "Рисует прицел и индикатор атаки из текущего ресурспака в их настоящих цветах вместо фигур ниже.", false);

    private final EnumSetting<Style> style = enumeration(G_MAIN, "style", "Стиль", "Форма прицела.", Style.CROSS)
        .visibleWhen(() -> !textures.get());

    private final IntSetting size = integer(G_MAIN, "size", "Размер", "Длина лучей креста / радиус круга (в пикселях GUI).", 4, 1, 32)
        .visibleWhen(() -> !textures.get() && style.get() != Style.DOT && style.get() != Style.CUSTOM);

    private final IntSetting gap = integer(G_MAIN, "gap", "Зазор", "Пустое место между центром и лучами.", 2, 0, 32)
        .visibleWhen(() -> !textures.get() && style.get() != Style.DOT);

    private final IntSetting thickness = integer(G_MAIN, "thickness", "Толщина", "Толщина линий.", 1, 1, 8)
        .visibleWhen(() -> !textures.get() && style.get() != Style.DOT);

    private final IntSetting dotSize = integer(G_MAIN, "dot-size", "Размер точки", "Размер точки в центре.", 1, 1, 8)
        .visibleWhen(() -> !textures.get() && hasDot());

    private final BoolSetting outline = bool(G_MAIN, "outline", "Обводка", "Рисует обводку вокруг прицела.", true)
        .visibleWhen(() -> !textures.get());

    private final IntSetting outlineWidth = integer(G_MAIN, "outline-width", "Ширина обводки", "Ширина обводки в пикселях GUI.", 1, 1, 4)
        .visibleWhen(() -> !textures.get() && outline.get());

    private final DoubleSetting opacity = decimal(G_MAIN, "opacity", "Непрозрачность", "Непрозрачность прицела и индикатора атаки.", 1.0, 0.05, 1.0);

    private final DoubleSetting scale = hudScaleSetting();

    // ---------------------------------------------------------------- custom shape

    private final IntSetting topLength = integer(G_CUSTOM, "top-length", "Верхний луч", "Длина верхнего луча (0 = нет, получится буква Т).", 4, 0, 32)
        .visibleWhen(this::isCustom);

    private final IntSetting bottomLength = integer(G_CUSTOM, "bottom-length", "Нижний луч", "Длина нижнего луча (0 = нет).", 4, 0, 32)
        .visibleWhen(this::isCustom);

    private final IntSetting leftLength = integer(G_CUSTOM, "left-length", "Левый луч", "Длина левого луча (0 = нет).", 4, 0, 32)
        .visibleWhen(this::isCustom);

    private final IntSetting rightLength = integer(G_CUSTOM, "right-length", "Правый луч", "Длина правого луча (0 = нет).", 4, 0, 32)
        .visibleWhen(this::isCustom);

    private final BoolSetting customDot = bool(G_CUSTOM, "center-dot", "Точка в центре", "Рисует точку в центре.", false)
        .visibleWhen(this::isCustom);

    // ---------------------------------------------------------------- dynamic

    private final BoolSetting dynamicSpread = bool(G_DYNAMIC, "attack-cooldown-spread", "Разлёт при перезарядке",
        "Прицел раздвигается, пока удар перезаряжается.", true)
        .visibleWhen(() -> !textures.get());

    private final IntSetting spreadAmount = integer(G_DYNAMIC, "spread-amount", "Сила разлёта",
        "Насколько раздвигается прицел при нулевом заряде удара.", 4, 1, 16)
        .visibleWhen(() -> !textures.get() && dynamicSpread.get());

    private final BoolSetting targetColorEnabled = bool(G_DYNAMIC, "target-color", "Цвет на цели",
        "Меняет цвет прицела, когда вы наводитесь на цель.", true)
        .visibleWhen(() -> !textures.get());

    private final BoolSetting playersOnly = bool(G_DYNAMIC, "players-only", "Только игроки",
        "Цвет на цели только для игроков (иначе любое живое существо).", true)
        .visibleWhen(() -> !textures.get() && targetColorEnabled.get());

    // ---------------------------------------------------------------- attack indicator

    private final EnumSetting<IndicatorMode> indicatorMode = enumeration(G_INDICATOR, "mode", "Режим",
        "Когда показывать полоску перезарядки удара под прицелом.", IndicatorMode.FOLLOW_VANILLA);

    private final IntSetting indicatorOffset = integer(G_INDICATOR, "offset", "Отступ",
        "Расстояние от центра прицела до верха полоски. 10 = как в ванилле.", 10, 2, 40)
        .visibleWhen(() -> indicatorMode.get() != IndicatorMode.OFF);

    private final IntSetting indicatorWidth = integer(G_INDICATOR, "width", "Ширина", "Ширина полоски без обводки.", 15, 3, 61)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final IntSetting indicatorHeight = integer(G_INDICATOR, "height", "Высота", "Высота полоски без обводки.", 2, 1, 8)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final BoolSetting indicatorOutline = bool(G_INDICATOR, "outline", "Обводка", "Обводка полоски в 1 пиксель.", true)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final BoolSetting indicatorFlash = bool(G_INDICATOR, "flash-when-full", "Вспышка при заряде",
        "Полоска коротко вспыхивает, когда удар полностью заряжен.", true)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    // ---------------------------------------------------------------- colors

    private final ColorSetting color = color(G_COLORS, "color", "Цвет", "Цвет прицела.", 0xFFFFFFFF)
        .visibleWhen(() -> !textures.get());

    private final ColorSetting targetColor = color(G_COLORS, "target-color", "Цвет на цели", "Цвет прицела при наведении на цель.", Theme.WARNING)
        .visibleWhen(() -> !textures.get() && targetColorEnabled.get());

    private final ColorSetting outlineColor = color(G_COLORS, "outline-color", "Цвет обводки", "Цвет обводки прицела.", 0xFF000000)
        .visibleWhen(() -> !textures.get() && outline.get());

    private final ColorSetting barColor = color(G_COLORS, "bar-color", "Полоска", "Цвет заполнения полоски во время зарядки.", Theme.GREY)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final ColorSetting barFullColor = color(G_COLORS, "bar-full-color", "Полоска (заряжено)", "Цвет полоски при полном заряде.",
        ColorSetting.rgb(255, 255, 255))
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final ColorSetting barFlashColor = color(G_COLORS, "bar-flash-color", "Вспышка", "Цвет вспышки полоски при полном заряде.", 0xFFFFFFFF)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF && indicatorFlash.get());

    private final ColorSetting barBackground = color(G_COLORS, "bar-background", "Фон полоски", "Цвет пустой части полоски.",
        ColorSetting.argb(60, 0, 8, 200))
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF);

    private final ColorSetting barOutlineColor = color(G_COLORS, "bar-outline-color", "Обводка полоски", "Цвет обводки полоски.", 0xFF000000)
        .visibleWhen(() -> !textures.get() && indicatorMode.get() != IndicatorMode.OFF && indicatorOutline.get());

    private final List<int[]> rects = new ArrayList<>();
    private boolean wasFull = true;
    private long flashStart;

    public CustomCrosshair() {
        super("custom-crosshair", "Свой прицел", "Заменяет ванильный прицел и индикатор атаки на настраиваемые, без инверсии цвета.", Category.RENDER);
    }

    private boolean isCustom() {
        return !textures.get() && style.get() == Style.CUSTOM;
    }

    private boolean hasDot() {
        return style.get() == Style.DOT || style.get() == Style.CROSS_DOT || (style.get() == Style.CUSTOM && customDot.get());
    }

    // ---------------------------------------------------------------- vanilla replacement

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tickCounter) -> {
            if (shouldSkipVanilla()) return;
            original.render(ctx, tickCounter);
        });
    }

    /** Skip vanilla while we draw instead: not with F3 open (vanilla axes stay) and not when our HUD is not drawn. */
    private boolean shouldSkipVanilla() {
        if (!isEnabled() || mc.player == null || mc.inGameHud == null) return false;
        if (mc.inGameHud.getDebugHud().shouldShowDebugHud()) return false;
        return HudManager.isEditorOpen() || shouldRenderHud();
    }

    /** Mirrors the conditions of vanilla InGameHud#renderCrosshair (perspective, debug screen, spectator). */
    private boolean vanillaWouldDraw() {
        if (mc.player == null || mc.interactionManager == null || mc.inGameHud == null) return false;
        if (!mc.options.getPerspective().isFirstPerson()) return false;
        if (mc.inGameHud.getDebugHud().shouldShowDebugHud()) return false;
        return mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || shouldRenderSpectatorCrosshair(mc.crosshairTarget);
    }

    /** Same rule as the private vanilla InGameHud#shouldRenderSpectatorCrosshair. */
    private boolean shouldRenderSpectatorCrosshair(HitResult hit) {
        if (hit == null) return false;
        if (hit.getType() == HitResult.Type.ENTITY) return ((EntityHitResult) hit).getEntity() instanceof NamedScreenHandlerFactory;
        if (hit.getType() == HitResult.Type.BLOCK && mc.world != null) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            return mc.world.getBlockState(pos).createScreenHandlerFactory(mc.world, pos) != null;
        }
        return false;
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public boolean shouldRenderHud() {
        return vanillaWouldDraw();
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.CENTER, 0, 0);
    }

    /** Half extent (without the center pixel) of the crosshair shape, including max spread and outline. */
    private int crosshairExtent() {
        if (textures.get()) return 8;

        int o = outline.get() ? outlineWidth.get() : 0;
        int t = thickness.get();
        int g = gap.get() + (dynamicSpread.get() ? spreadAmount.get() : 0);
        int ext = switch (style.get()) {
            case DOT -> dotSize.get() / 2 + 1;
            case CIRCLE -> size.get() + g + t / 2 + 2;
            case CUSTOM -> g + t + Math.max(Math.max(topLength.get(), bottomLength.get()), Math.max(leftLength.get(), rightLength.get()));
            default -> g + t + size.get();
        };
        if (hasDot()) ext = Math.max(ext, dotSize.get() / 2 + 1);
        return ext + o;
    }

    private int indicatorHalfWidth() {
        if (indicatorMode.get() == IndicatorMode.OFF) return 0;
        if (textures.get()) return 9;
        return indicatorWidth.get() / 2 + 2;
    }

    private int indicatorBottom() {
        if (indicatorMode.get() == IndicatorMode.OFF) return 0;
        if (textures.get()) return indicatorOffset.get() + 16;
        return indicatorOffset.get() + indicatorHeight.get() + 2;
    }

    private int halfWidth() {
        return Math.max(crosshairExtent(), indicatorHalfWidth());
    }

    private int halfHeight() {
        return Math.max(crosshairExtent(), indicatorBottom());
    }

    @Override
    public double getHudWidth() {
        return halfWidth() * 2 + 1;
    }

    @Override
    public double getHudHeight() {
        return halfHeight() * 2 + 1;
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        Matrix3x2fStack m = context.getMatrices();
        m.pushMatrix();
        try {
            // Snap the box to whole GUI pixels (floor), which puts the center pixel exactly where vanilla draws it.
            float s = scale.getFloat();
            float tx = m.m20(), ty = m.m21();
            m.translate((float) ((Math.floor(tx) - tx) / s), (float) ((Math.floor(ty) - ty) / s));

            int cx = halfWidth();
            int cy = halfHeight();

            float progress;
            if (mc.player != null) progress = mc.player.getAttackCooldownProgress(tickDelta);
            else progress = (System.currentTimeMillis() % 1500) / 1200f; // preview without a world
            progress = Math.min(1f, Math.max(0f, progress));

            if (textures.get()) renderTextures(context, cx, cy, progress, inEditor);
            else {
                renderShape(context, cx, cy, progress);
                renderIndicator(context, cx, cy, progress, inEditor);
            }
        } finally {
            m.popMatrix();
        }
    }

    // ---------------------------------------------------------------- shapes

    private void renderShape(DrawContext context, int cx, int cy, float progress) {
        int t = thickness.get();
        int s = size.get();
        int g = gap.get();
        if (dynamicSpread.get()) g += Math.round((1f - progress) * spreadAmount.get());

        rects.clear();
        switch (style.get()) {
            case CROSS -> addCross(cx, cy, s, s, s, s, g, t, true);
            case DOT -> addBlock(cx, cy, dotSize.get());
            case CIRCLE -> addRing(cx, cy, s + g, t);
            case CROSS_DOT -> {
                addCross(cx, cy, s, s, s, s, g, t, false);
                addBlock(cx, cy, dotSize.get());
            }
            case CUSTOM -> {
                addCross(cx, cy, topLength.get(), bottomLength.get(), leftLength.get(), rightLength.get(), g, t, !customDot.get());
                if (customDot.get()) addBlock(cx, cy, dotSize.get());
            }
        }

        int mainColor = withOpacity(isOnTarget() ? targetColor.get() : color.get());

        if (outline.get()) {
            int ow = outlineWidth.get();
            int oc = withOpacity(outlineColor.get());
            for (int[] r : rects) context.fill(r[0] - ow, r[1] - ow, r[2] + ow, r[3] + ow, oc);
        }
        for (int[] r : rects) context.fill(r[0], r[1], r[2], r[3], mainColor);
    }

    private boolean isOnTarget() {
        if (!targetColorEnabled.get()) return false;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return false;
        if (playersOnly.get()) return hit.getEntity() instanceof PlayerEntity;
        return hit.getEntity() instanceof LivingEntity;
    }

    /** The center block covers [c - t/2, c - t/2 + t) on both axes; the arms are placed symmetrically around it. */
    private void addCross(int cx, int cy, int top, int bottom, int left, int right, int g, int t, boolean fillCenterOnZeroGap) {
        int lo = -(t / 2);
        int hi = lo + t;

        if (left > 0) rects.add(new int[]{cx + lo - g - left, cy + lo, cx + lo - g, cy + hi});
        if (right > 0) rects.add(new int[]{cx + hi + g, cy + lo, cx + hi + g + right, cy + hi});
        if (top > 0) rects.add(new int[]{cx + lo, cy + lo - g - top, cx + hi, cy + lo - g});
        if (bottom > 0) rects.add(new int[]{cx + lo, cy + hi + g, cx + hi, cy + hi + g + bottom});

        if (fillCenterOnZeroGap && g == 0) rects.add(new int[]{cx + lo, cy + lo, cx + hi, cy + hi});
    }

    private void addBlock(int cx, int cy, int d) {
        int lo = -(d / 2);
        rects.add(new int[]{cx + lo, cy + lo, cx + lo + d, cy + lo + d});
    }

    /** Pixel-exact ring built from horizontal runs. Pixel (x, y) is inside if innerR <= dist(center pixel) < outerR. */
    private void addRing(int cx, int cy, int radius, int t) {
        double outer = radius + t / 2.0;
        double inner = Math.max(0, radius - t / 2.0);
        double outer2 = outer * outer, inner2 = inner * inner;
        int r = (int) Math.ceil(outer) + 1;

        for (int dy = -r; dy <= r; dy++) {
            int runStart = Integer.MIN_VALUE;
            for (int dx = -r; dx <= r + 1; dx++) {
                double d2 = dx * dx + dy * dy;
                boolean in = dx <= r && d2 < outer2 && d2 >= inner2;
                if (in && runStart == Integer.MIN_VALUE) runStart = dx;
                else if (!in && runStart != Integer.MIN_VALUE) {
                    rects.add(new int[]{cx + runStart, cy + dy, cx + dx, cy + dy + 1});
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
    }

    // ---------------------------------------------------------------- attack indicator

    private enum IndicatorState { HIDDEN, CHARGING, FULL }

    /**
     * Vanilla: only with the "crosshair" attack indicator option; a bar while charging, the "full" sprite when
     * looking at a living, alive entity with full charge and an attack speed faster than 5 ticks, otherwise nothing.
     */
    private IndicatorState indicatorState(float progress, boolean inEditor) {
        IndicatorMode mode = indicatorMode.get();
        if (mode == IndicatorMode.OFF) return IndicatorState.HIDDEN;

        IndicatorState preview = progress >= 1f ? IndicatorState.FULL : IndicatorState.CHARGING;
        if (mc.player == null) return inEditor ? preview : IndicatorState.HIDDEN;
        if (mode == IndicatorMode.ALWAYS) return preview;

        if (mc.options.getAttackIndicator().getValue() != AttackIndicator.CROSSHAIR) {
            // Still preview it in the HUD editor so the element can be positioned.
            return inEditor ? preview : IndicatorState.HIDDEN;
        }

        float vanillaProgress = mc.player.getAttackCooldownProgress(0f);
        Entity target = mc.targetedEntity;
        boolean full = target instanceof LivingEntity && vanillaProgress >= 1f
            && mc.player.getAttackCooldownProgressPerTick() > 5f && target.isAlive();

        if (full) return IndicatorState.FULL;
        if (vanillaProgress < 1f) return IndicatorState.CHARGING;
        return inEditor ? IndicatorState.FULL : IndicatorState.HIDDEN;
    }

    private void renderIndicator(DrawContext context, int cx, int cy, float progress, boolean inEditor) {
        IndicatorState state = indicatorState(progress, inEditor);

        // Tracked even while hidden so the flash starts exactly when the charge completes.
        boolean isFull = progress >= 1f;
        if (isFull && !wasFull) flashStart = System.currentTimeMillis();
        wasFull = isFull;

        if (state == IndicatorState.HIDDEN) return;

        int w = indicatorWidth.get();
        int h = indicatorHeight.get();
        int x = cx - w / 2;
        int y = cy + indicatorOffset.get() + 1;

        if (indicatorOutline.get()) context.fill(x - 1, y - 1, x + w + 1, y + h + 1, withOpacity(barOutlineColor.get()));

        if (state == IndicatorState.FULL) {
            int c = barFullColor.get();
            if (indicatorFlash.get()) {
                long since = System.currentTimeMillis() - flashStart;
                if (since >= 0 && since < FLASH_MS) c = lerpColor(barFlashColor.get(), c, since / (float) FLASH_MS);
            }
            context.fill(x, y, x + w, y + h, withOpacity(c));
        } else {
            int filled = Math.min(w, (int) (progress * (w + 1)));
            context.fill(x, y, x + w, y + h, withOpacity(barBackground.get()));
            if (filled > 0) context.fill(x, y, x + filled, y + h, withOpacity(barColor.get()));
        }
    }

    // ---------------------------------------------------------------- resource pack textures

    private void renderTextures(DrawContext context, int cx, int cy, float progress, boolean inEditor) {
        int white = withOpacity(0xFFFFFFFF);

        // Vanilla: 15x15 sprite at (sw - 15) / 2, i.e. the center pixel is sprite pixel 7.
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, CROSSHAIR_TEXTURE, cx - 7, cy - 7, 15, 15, white);

        IndicatorState state = indicatorState(progress, inEditor);
        if (state == IndicatorState.HIDDEN) return;

        // Vanilla: x = sw / 2 - 8, y = sh / 2 - 7 + 16 (= center pixel -7 / +10 on even screen sizes).
        int x = cx - 7;
        int y = cy + indicatorOffset.get();

        if (state == IndicatorState.FULL) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_FULL_TEXTURE, x, y, 16, 16, white);
        } else {
            int filled = (int) (progress * 17f);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_BACKGROUND_TEXTURE, x, y, 16, 4, white);
            if (filled > 0) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, x, y, filled, 4, white);
            }
        }
    }

    // ---------------------------------------------------------------- color helpers

    private int withOpacity(int argb) {
        int a = (argb >>> 24) & 0xFF;
        a = (int) Math.round(a * opacity.get());
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.min(1f, Math.max(0f, t));
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    public enum Style {
        CROSS("Крест"),
        DOT("Точка"),
        CIRCLE("Круг"),
        CROSS_DOT("Крест + точка"),
        CUSTOM("Своя форма");

        private final String title;

        Style(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:Style." + name(), title);
        }
    }

    public enum IndicatorMode {
        FOLLOW_VANILLA("Как в настройках игры"),
        ALWAYS("Всегда"),
        OFF("Выкл");

        private final String title;

        IndicatorMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:IndicatorMode." + name(), title);
        }
    }
}
