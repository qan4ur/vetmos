package io.github.qan4ur.vetmos.modules.hud.vanilla;

import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.mixin.TitlesInGameHudAccessor;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

/**
 * Movable replacement of the vanilla /title text (title + subtitle). Same fade logic as vanilla, re-drawn from the
 * InGameHud state inside the HUD box.
 */
public class TitlesHud extends Module implements HudModule {
    private static final String G = "Основное";

    private final DoubleSetting opacity = decimal(G, "opacity", "Прозрачность", "Множитель непрозрачности текста.", 1.0, 0.1, 1.0);
    private final BoolSetting hideSubtitle = bool(G, "hide-subtitle", "Скрыть подзаголовок", "Не показывать подзаголовок.", false);
    private final DoubleSetting titleSize = decimal(G, "title-size", "Размер заголовка", "Размер заголовка относительно обычного текста (в ванилле 4).", 4.0, 1.0, 6.0);
    private final DoubleSetting subtitleSize = decimal(G, "subtitle-size", "Размер подзаголовка", "Размер подзаголовка относительно обычного текста (в ванилле 2).", 2.0, 0.5, 4.0);
    private final DoubleSetting scale = hudScaleSetting();

    /** Tick progress captured from the vanilla layer (vanilla uses the non-frozen-ignoring progress). */
    private float tickProgress;
    private boolean haveTickProgress;

    // state of the current frame
    private Text title, subtitle;
    private int alpha;

    public TitlesHud() {
        super("titles-hud", "Заголовки", "Перемещаемые заголовки и подзаголовки (/title).", Category.HUD);
    }

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.TITLE_AND_SUBTITLE, original -> (context, tickCounter) -> {
            if (!isEnabled()) {
                original.render(context, tickCounter);
                return;
            }
            tickProgress = tickCounter.getTickProgress(false);
            haveTickProgress = true;
        });
    }

    // ---------------------------------------------------------------- data

    private void refresh(boolean editor, float fallbackTick) {
        title = null;
        subtitle = null;
        alpha = 0;
        if (mc.inGameHud != null) {
            TitlesInGameHudAccessor hud = (TitlesInGameHudAccessor) mc.inGameHud;
            int remain = hud.vetmos$titlesRemainTicks();
            if (hud.vetmos$titlesTitle() != null && remain > 0) {
                int fadeIn = hud.vetmos$titlesFadeInTicks(), stay = hud.vetmos$titlesStayTicks(), fadeOut = hud.vetmos$titlesFadeOutTicks();
                float left = remain - (haveTickProgress ? tickProgress : fallbackTick);
                int a = 255;
                if (remain > fadeOut + stay) a = (int) ((fadeIn + stay + fadeOut - left) * 255f / fadeIn);
                if (remain <= fadeOut) a = (int) (left * 255f / fadeOut);
                a = MathHelper.clamp(a, 0, 255);
                if (a > 0) {
                    title = hud.vetmos$titlesTitle();
                    subtitle = hud.vetmos$titlesSubtitle();
                    alpha = a;
                }
            }
        }
        if (title == null && editor) {
            title = Text.literal(io.github.qan4ur.vetmos.util.Lang.tr("hud:sample-title", "Заголовок"));
            subtitle = Text.literal(io.github.qan4ur.vetmos.util.Lang.tr("hud:sample-subtitle", "Подзаголовок"));
            alpha = 255;
        }
        if (hideSubtitle.get()) subtitle = null;
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        // vanilla (with text backgrounds) spans -48..+32 around the screen center -> center of the box at -8
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.CENTER, 0, -8);
    }

    @Override
    public boolean shouldRenderHud() {
        refresh(false, 0);
        return title != null;
    }

    @Override
    public double getHudWidth() {
        refresh(HudManager.isEditorOpen(), 0);
        TextRenderer tr = mc.textRenderer;
        double w = title == null ? 60 : (tr.getWidth(title) + 4) * titleSize.get();
        if (subtitle != null) w = Math.max(w, (tr.getWidth(subtitle) + 4) * subtitleSize.get());
        return Math.max(20, w);
    }

    @Override
    public double getHudHeight() {
        double ts = titleSize.get();
        // title text at 2*ts (room for its background), subtitle text 50 px below the title text in vanilla (= 12.5 * ts)
        return hideSubtitle.get() ? 13 * ts : 14.5 * ts + 11 * subtitleSize.get();
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        if (title == null) return;
        int a = Math.round(alpha * opacity.getFloat());
        if (a <= 0) return;
        int color = (a << 24) | 0xFFFFFF;
        TextRenderer tr = mc.textRenderer;
        float boxW = (float) getHudWidthCached();
        Matrix3x2fStack m = context.getMatrices();

        float ts = titleSize.getFloat();
        m.pushMatrix();
        m.translate(boxW / 2, ts * 2);
        m.scale(ts, ts);
        int tw = tr.getWidth(title);
        context.drawTextWithBackground(tr, title, -tw / 2, 0, tw, color);
        m.popMatrix();

        if (subtitle != null) {
            float ss = subtitleSize.getFloat();
            m.pushMatrix();
            m.translate(boxW / 2, 14.5f * ts);
            m.scale(ss, ss);
            int sw = tr.getWidth(subtitle);
            context.drawTextWithBackground(tr, subtitle, -sw / 2, 0, sw, color);
            m.popMatrix();
        }
    }

    private double getHudWidthCached() {
        TextRenderer tr = mc.textRenderer;
        double w = (tr.getWidth(title) + 4) * titleSize.get();
        if (subtitle != null) w = Math.max(w, (tr.getWidth(subtitle) + 4) * subtitleSize.get());
        return Math.max(20, w);
    }
}
