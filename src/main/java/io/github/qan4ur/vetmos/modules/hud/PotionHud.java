package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static io.github.qan4ur.vetmos.modules.hud.HudRender2.*;

/** Active status effects with timers. Effect names come from the game's own (localized) translations. */
public class PotionHud extends Module implements HudModule {
    public enum Sort {
        DURATION("По времени"), NAME("По имени"), HARMFUL("Сначала вредные");

        private final String title;

        Sort(String title) { this.title = title; }

        @Override
        public String toString() { return io.github.qan4ur.vetmos.util.Lang.tr("enum:Sort." + name(), title); }
    }

    private static final String G = "Основное", C = "Цвета";

    private final BoolSetting hideVanilla = bool(G, "hide-vanilla-icons", "Скрыть ванильные иконки", "Скрывать стандартные иконки эффектов в правом верхнем углу.", true);
    private final BoolSetting showIcon = bool(G, "icons", "Иконки", "Рисовать иконку эффекта.", true);
    private final BoolSetting showLevel = bool(G, "level", "Уровень", "Показывать уровень эффекта римскими цифрами.", true);
    private final BoolSetting showAmbient = bool(G, "show-ambient", "Фоновые эффекты", "Показывать эффекты маяков и проводников.", true);
    private final BoolSetting respectHidden = bool(G, "respect-hidden", "Учитывать скрытые", "Пропускать эффекты, у которых сервер скрыл иконку.", true);
    private final EnumSetting<Sort> sort = enumeration(G, "sort", "Сортировка", "Порядок эффектов в списке.", Sort.DURATION);
    private final IntSetting blinkBelow = integer(G, "blink-below", "Мигать при", "Мигать, когда осталось меньше стольких секунд.", 10, 0, 120);
    private final BoolSetting effectColorBar = bool(G, "effect-color-bar", "Цветная полоска", "Тонкая полоска сбоку в цвете эффекта.", true);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting bgColor = color(C, "background", "Фон", "Цвет фона панели.", Theme.PANEL);
    private final ColorSetting accentColor = color(C, "accent", "Акцент", "Цвет линии сверху.", Theme.SCARLET);
    private final ColorSetting nameColor = color(C, "name", "Название", "Цвет названия эффекта.", Theme.TEXT);
    private final ColorSetting harmfulColor = color(C, "harmful-name", "Вредный эффект", "Цвет названия вредных эффектов.", ColorSetting.rgb(255, 90, 90));
    private final ColorSetting timerColor = color(C, "timer", "Таймер", "Цвет таймера.", ColorSetting.rgb(190, 190, 190));
    private final ColorSetting blinkColor = color(C, "blink", "Мигание", "Цвет мигания заканчивающихся эффектов.", Theme.GLOW);

    private static final int ROW_H = 22;
    private static final int PAD = 3;

    private static final class Row {
        StatusEffectInstance instance;
        float progress;
        boolean alive;
    }

    private final Map<RegistryEntry<StatusEffect>, Row> rows = new LinkedHashMap<>();
    private final List<Row> order = new ArrayList<>();
    private List<StatusEffectInstance> samples;
    private float animWidth = -1;
    private long lastFrameNs;

    // layout of the current frame, computed in update()
    private int boxW = 1, boxH = 1;

    public PotionHud() {
        super("potion-hud", "Эффекты", "Список активных эффектов с таймерами.", Category.HUD);
    }

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, original -> (context, tickCounter) -> {
            if (!shouldHideVanilla()) original.render(context, tickCounter);
        });
    }

    @Override
    protected void onDisable() {
        rows.clear();
        order.clear();
        animWidth = -1;
        lastFrameNs = 0;
    }

    public boolean shouldHideVanilla() {
        return isEnabled() && hideVanilla.get();
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.CENTER, -4, 0);
    }

    @Override
    public boolean shouldRenderHud() {
        update();
        return mc.player != null && !order.isEmpty();
    }

    @Override
    public double getHudWidth() {
        update();
        return boxW;
    }

    @Override
    public double getHudHeight() {
        return boxH;
    }

    private void update() {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0 ? 0 : (float) Math.min((now - lastFrameNs) / 1e9, 0.1);
        lastFrameNs = now;

        // sync rows with active effects
        for (Row row : rows.values()) row.alive = false;
        boolean any = false;
        if (mc.player != null) {
            for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
                if (!showAmbient.get() && inst.isAmbient()) continue;
                if (respectHidden.get() && !inst.shouldShowIcon()) continue;
                Row row = rows.computeIfAbsent(inst.getEffectType(), k -> new Row());
                row.instance = inst;
                row.alive = true;
                any = true;
            }
        }

        // preview in the HUD editor when there are no real effects
        if (isEditor() && !any) {
            for (StatusEffectInstance inst : samples()) {
                Row row = rows.computeIfAbsent(inst.getEffectType(), k -> new Row());
                row.instance = inst;
                row.alive = true;
            }
        }

        rows.values().removeIf(row -> {
            row.progress = approach(row.progress, row.alive ? 1 : 0, dt, 12);
            if (row.alive && row.progress > 0.995f) row.progress = 1;
            return !row.alive && row.progress < 0.02f;
        });

        order.clear();
        if (rows.isEmpty()) {
            animWidth = -1;
            boxW = boxH = 1;
            return;
        }

        order.addAll(rows.values());
        Comparator<Row> cmp = switch (sort.get()) {
            case DURATION -> Comparator.comparingInt(r -> r.instance.isInfinite() ? Integer.MAX_VALUE : r.instance.getDuration());
            case NAME -> Comparator.comparing(r -> r.instance.getEffectType().value().getName().getString());
            case HARMFUL -> Comparator.<Row>comparingInt(r -> r.instance.getEffectType().value().isBeneficial() ? 1 : 0)
                .thenComparingInt(r -> r.instance.isInfinite() ? Integer.MAX_VALUE : r.instance.getDuration());
        };
        order.sort(cmp);

        // measure
        TextRenderer tr = mc.textRenderer;
        int iconW = showIcon.get() ? 20 : 0;
        int textW = 0;
        float totalH = 0;
        for (Row row : order) {
            int nameW = tr.getWidth(label(row.instance));
            int timeW = tr.getWidth(timer(row.instance));
            textW = Math.max(textW, Math.max(nameW, timeW));
            totalH += ROW_H * easeOutCubic(row.progress);
        }
        float targetW = PAD * 2 + iconW + textW + 6;
        animWidth = animWidth < 0 ? targetW : approach(animWidth, targetW, dt, 14);
        boxW = Math.round(animWidth);
        boxH = Math.round(totalH) + PAD * 2;
    }

    private List<StatusEffectInstance> samples() {
        if (samples == null) {
            samples = List.of(
                new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 154, 1),
                new StatusEffectInstance(StatusEffects.SPEED, 20 * 87, 1),
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 7, 0)
            );
        }
        return samples;
    }

    /** Rows slide in from the side the element is attached to. */
    private boolean isRightAligned() {
        return HudManager.getPosition(this).anchorX == HudPosition.AnchorX.RIGHT;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (order.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int w = boxW, h = boxH;
        int iconW = showIcon.get() ? 20 : 0;
        boolean right = isRightAligned();

        Matrix3x2fStack m = ctx.getMatrices();

        drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), 1);

        float pulse = pulse(1.6);
        float y = PAD;
        for (Row row : order) {
            float e = easeOutCubic(row.progress);
            StatusEffectInstance inst = row.instance;
            int ry = Math.round(y);
            int alpha = (int) (255 * e);
            float slide = (1 - e) * 16 * (right ? 1 : -1);

            boolean low = !inst.isInfinite() && inst.getDuration() < blinkBelow.get() * 20;
            float k = low ? pulse : 0;

            m.pushMatrix();
            m.translate(slide, 0);

            if (low) {
                int bc = blinkColor.get();
                ctx.fill(PAD, ry, w - PAD, ry + ROW_H - 2, withAlpha(bc, (int) ((bc >>> 24) * k * e)));
            }

            if (effectColorBar.get()) {
                int ec = withAlpha(inst.getEffectType().value().getColor(), alpha);
                ctx.fill(PAD, ry + 2, PAD + 1, ry + ROW_H - 4, ec);
            }

            int tx = PAD + 3;
            if (showIcon.get()) {
                float iconAlpha = e * (low ? 0.45f + 0.55f * (1 - k) : 1f);
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(inst.getEffectType()), tx, ry + 1, 18, 18, ColorHelper.getWhite(iconAlpha));
                tx += iconW;
            }

            int nc = inst.getEffectType().value().isBeneficial() ? nameColor.get() : harmfulColor.get();
            ctx.drawText(tr, label(inst), tx, ry + 1, withAlpha(nc, alpha), shadow.get());

            int tc = timerColor.get();
            if (low) tc = lerpColor(tc, accentColor.get() | 0xFF000000, k);
            ctx.drawText(tr, timer(inst), tx, ry + 11, withAlpha(tc, alpha), shadow.get());

            m.popMatrix();
            y += ROW_H * e;
        }
    }

    private String label(StatusEffectInstance inst) {
        String name = inst.getEffectType().value().getName().getString();
        if (!showLevel.get() || inst.getAmplifier() <= 0) return name;
        return name + " " + roman(inst.getAmplifier() + 1);
    }

    private static String timer(StatusEffectInstance inst) {
        if (inst.isInfinite()) return "∞";
        int total = Math.max(0, inst.getDuration() / 20);
        int min = total / 60, sec = total % 60;
        if (min >= 60) return String.format("%d:%02d:%02d", min / 60, min % 60, sec);
        return String.format("%d:%02d", min, sec);
    }

    private static final int[] ROMAN_VAL = { 10, 9, 5, 4, 1 };
    private static final String[] ROMAN_SYM = { "X", "IX", "V", "IV", "I" };

    private static String roman(int n) {
        if (n <= 0 || n > 39) return String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VAL.length; i++) {
            while (n >= ROMAN_VAL[i]) {
                sb.append(ROMAN_SYM[i]);
                n -= ROMAN_VAL[i];
            }
        }
        return sb.toString();
    }
}
