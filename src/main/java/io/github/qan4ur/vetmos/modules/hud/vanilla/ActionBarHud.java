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

/**
 * Movable replacement of the action bar message (the text above the hotbar, also used for "Now playing").
 * Same fade and rainbow tint as vanilla, re-drawn from the InGameHud state inside the HUD box.
 */
public class ActionBarHud extends Module implements HudModule {
    private static final String G = "Основное";

    private final DoubleSetting opacity = decimal(G, "opacity", "Прозрачность", "Множитель непрозрачности текста.", 1.0, 0.1, 1.0);
    private final BoolSetting tint = bool(G, "rainbow-tint", "Радужный цвет", "Переливающийся цвет для «Сейчас играет», как в ванилле.", true);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    private float tickProgress;
    private boolean haveTickProgress;

    private Text message;
    private int alpha;
    private float left;
    private boolean tinted;

    public ActionBarHud() {
        super("action-bar-hud", "Сообщение над хотбаром", "Перемещаемое сообщение над хотбаром (action bar).", Category.HUD);
    }

    @Override
    public void onInit() {
        HudElementRegistry.replaceElement(VanillaHudElements.OVERLAY_MESSAGE, original -> (context, tickCounter) -> {
            if (!isEnabled()) {
                original.render(context, tickCounter);
                return;
            }
            tickProgress = tickCounter.getTickProgress(false);
            haveTickProgress = true;
        });
    }

    private void refresh(boolean editor) {
        message = null;
        alpha = 0;
        tinted = false;
        if (mc.inGameHud != null) {
            TitlesInGameHudAccessor hud = (TitlesInGameHudAccessor) mc.inGameHud;
            int remaining = hud.vetmos$titlesOverlayRemaining();
            if (hud.vetmos$titlesOverlayMessage() != null && remaining > 0) {
                left = remaining - (haveTickProgress ? tickProgress : 0);
                int a = Math.min(255, (int) (left * 255f / 20f));
                if (a > 0) {
                    message = hud.vetmos$titlesOverlayMessage();
                    alpha = a;
                    tinted = hud.vetmos$titlesOverlayTinted();
                }
            }
        }
        if (message == null && editor) {
            message = Text.literal(io.github.qan4ur.vetmos.util.Lang.tr("hud:sample-action-bar", "Сообщение над хотбаром"));
            alpha = 255;
        }
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        // vanilla: text at y = height - 72, its background ends 2 px below the text -> box bottom at height - 61
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.BOTTOM, 0, -61);
    }

    @Override
    public boolean shouldRenderHud() {
        refresh(false);
        return message != null;
    }

    @Override
    public double getHudWidth() {
        refresh(HudManager.isEditorOpen());
        return message == null ? 60 : mc.textRenderer.getWidth(message) + 4;
    }

    @Override
    public double getHudHeight() {
        return 13;
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        if (message == null) return;
        int a = Math.round(alpha * opacity.getFloat());
        if (a <= 0) return;
        int color = tinted && tint.get() ? MathHelper.hsvToArgb(left / 50f, 0.7f, 0.6f, a) : (a << 24) | 0xFFFFFF;
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(message);
        if (shadow.get()) {
            context.drawTextWithBackground(tr, message, 2, 2, w, color);
        } else {
            int bg = mc.options.getTextBackgroundColor(0f);
            if (bg != 0) context.fill(0, 0, w + 4, 13, net.minecraft.util.math.ColorHelper.mix(bg, color));
            context.drawText(tr, message, 2, 2, color, false);
        }
    }
}
