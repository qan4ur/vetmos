package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix3x2fStack;

/** Counts your consecutive landed hits, with a pop animation on every hit and a shake when the combo breaks. */
public class ComboCounter extends Module implements HudModule {
    private static final String G_MAIN = "Основное";
    private static final String G_COLORS = "Цвета";

    private final BoolSetting playersOnly = bool(G_MAIN, "players-only", "Только игроки", "Считать только удары по игрокам.", false);

    private final DoubleSetting timeout = decimal(G_MAIN, "timeout", "Сброс через",
        "Секунд без попаданий, после которых комбо сбрасывается.", 2.5, 0.5, 10.0, 1);

    private final BoolSetting resetOnHurt = bool(G_MAIN, "reset-on-hurt", "Сброс при уроне", "Сбрасывать комбо, когда вы получаете урон.", true);

    private final IntSetting minCombo = integer(G_MAIN, "min-combo", "Мин. комбо", "Показывать счётчик начиная с этого числа ударов.", 2, 1, 10);

    private final EnumSetting<Label> label = enumeration(G_MAIN, "label", "Подпись", "Текст после числа.", Label.COMBO_RU);

    private final BoolSetting background = bool(G_MAIN, "background", "Фон", "Рисует панель под счётчиком.", true);

    private final DoubleSetting popStrength = decimal(G_MAIN, "pop-strength", "Сила «прыжка»", "Насколько увеличивается число при новом ударе.", 0.45, 0.0, 1.5);

    private final BoolSetting previewInChat = bool(G_MAIN, "preview-in-chat", "Превью в чате", "Показывает пример, пока открыт чат.", true);

    private final BoolSetting showInScreens = bool(G_MAIN, "show-in-screens", "Показывать в меню", "Рисовать, даже когда открыто меню (кроме чата).", false);

    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting numberColor = color(G_COLORS, "number", "Число", "Цвет числа комбо.", Theme.SCARLET);

    private final ColorSetting flashColor = color(G_COLORS, "flash", "Вспышка", "Цвет вспышки числа при новом ударе.", 0xFFFFFFFF);

    private final ColorSetting labelColor = color(G_COLORS, "label", "Подпись", "Цвет подписи.", Theme.TEXT_SECONDARY);

    private final ColorSetting backgroundColor = color(G_COLORS, "background", "Фон", "Фон панели.", Theme.PANEL)
        .visibleWhen(background::get);

    private final ColorSetting outlineColor = color(G_COLORS, "outline", "Обводка", "Обводка панели.", ColorSetting.argb(240, 240, 240, 110))
        .visibleWhen(background::get);

    private final ColorSetting glowColor = color(G_COLORS, "glow", "Свечение", "Свечение вдоль верхнего края панели.", Theme.GLOW)
        .visibleWhen(background::get);

    private final ColorSetting shadowColor = color(G_COLORS, "shadow", "Тень", "Мягкая тень под панелью.", ColorSetting.argb(0, 0, 0, 90))
        .visibleWhen(background::get);

    // ---------------------------------------------------------------- state

    private LivingEntity pending;
    private int pendingTicks;
    private int pendingLastHurt;
    private int lastPlayerHurt;

    private int combo;
    private int displayCombo;
    private long lastHit;

    private double pop;
    private double fade;
    private double shake;

    private final PanelRender.FrameClock clock = new PanelRender.FrameClock();
    private boolean active;

    public ComboCounter() {
        super("combo-counter", "Счётчик комбо", "Считает ваши удары подряд без промахов и пропущенного урона.", Category.HUD);
    }

    private void resetState() {
        pending = null;
        combo = 0;
        displayCombo = 0;
        fade = 0;
        pop = 0;
        shake = 0;
        lastPlayerHurt = mc.player != null ? mc.player.hurtTime : 0;
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    public void onJoin() {
        resetState();
    }

    @Override
    public void onLeave() {
        resetState();
    }

    @Override
    public void onAttack(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;
        if (playersOnly.get() && !(living instanceof PlayerEntity)) return;
        pending = living;
        pendingTicks = 0;
        pendingLastHurt = living.hurtTime;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Taking damage restarts hurtTime at its max: an increase means we just got hit.
        int ph = mc.player.hurtTime;
        if (resetOnHurt.get() && ph > lastPlayerHurt) reset();
        lastPlayerHurt = ph;

        // A hit "lands" when the attacked entity's hurt animation restarts shortly after we swung.
        if (pending != null) {
            pendingTicks++;
            if (pending.isRemoved()) {
                pending = null;
            } else if (pending.hurtTime > pendingLastHurt) {
                hit();
                pending = null;
            } else {
                pendingLastHurt = pending.hurtTime;
                if (pendingTicks > 8) pending = null;
            }
        }

        if (combo > 0 && System.currentTimeMillis() - lastHit > timeout.get() * 1000) reset();
    }

    private void hit() {
        combo++;
        displayCombo = combo;
        lastHit = System.currentTimeMillis();
        pop = 1;
    }

    private void reset() {
        if (combo >= minCombo.get()) shake = 1;
        combo = 0;
    }

    // ---------------------------------------------------------------- animation (driven every frame by the HUD)

    private void update() {
        double dt = clock.tick();
        if (mc.player == null) return;

        active = combo >= minCombo.get();
        boolean preview = !active && previewInChat.get() && PanelRender.isChatOpen();
        if (preview) displayCombo = Math.max(displayCombo, 3);

        fade = PanelRender.approach(fade, active || preview ? 1 : 0, active ? 14 : 6, dt);
        pop = Math.max(0, pop - dt * 3.5);
        shake = Math.max(0, shake - dt * 2.5);
        if (fade < 0.004) {
            fade = 0;
            if (!active) displayCombo = 0;
        }
    }

    // ---------------------------------------------------------------- HUD

    private static final float NUM_SCALE = 2f;
    private static final int PANEL_PAD = 7;
    private static final int HALF_H = 13;

    private int shownCombo() {
        return displayCombo > 0 ? displayCombo : 5;
    }

    private float contentWidth(int number) {
        TextRenderer tr = mc.textRenderer;
        String lbl = label.get().text;
        int lblW = lbl.isEmpty() ? 0 : tr.getWidth(lbl);
        return tr.getWidth(Integer.toString(number)) * NUM_SCALE + (lblW > 0 ? 4 + lblW : 0);
    }

    @Override
    public double getHudWidth() {
        return Math.ceil(contentWidth(shownCombo()) / 2) * 2 + PANEL_PAD * 2;
    }

    @Override
    public double getHudHeight() {
        return HALF_H * 2;
    }

    @Override
    public boolean shouldRenderHud() {
        update();
        return fade > 0 && PanelRender.shouldRender(showInScreens.get());
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (inEditor) update(); // otherwise shouldRenderHud() already updated this frame
        boolean preview = inEditor && fade <= 0;
        if (!preview && fade <= 0) return;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (getHudWidth() / 2), HALF_H);
        if (preview) render(ctx, shownCombo(), true, 1, 1);
        else {
            double eased = PanelRender.easeOutCubic(fade);
            render(ctx, displayCombo, active, fade, active ? 0.85 + 0.15 * eased : 0.95 + 0.05 * eased);
        }
        m.popMatrix();
    }

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.CENTER, 0, -40);
    }

    /** Draws the counter centered on (0, 0). */
    private void render(DrawContext ctx, int number, boolean active, double fade, double s) {
        TextRenderer tr = mc.textRenderer;

        String num = Integer.toString(number);
        String lbl = label.get().text;

        int numW = tr.getWidth(num);
        int lblW = lbl.isEmpty() ? 0 : tr.getWidth(lbl);
        float totalW = numW * NUM_SCALE + (lblW > 0 ? 4 + lblW : 0);

        // Little shake when a combo breaks
        float shakeX = (float) (Math.sin(System.currentTimeMillis() / 18.0) * 3 * shake * shake);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(shakeX, 0);
        m.scale((float) s, (float) s);

        double a = fade;
        int left = (int) Math.floor(-totalW / 2);

        if (background.get()) {
            int x1 = left - PANEL_PAD, x2 = (int) Math.ceil(totalW / 2) + PANEL_PAD;
            PanelRender.panel(ctx, x1, -HALF_H, x2, HALF_H, 4,
                PanelRender.withAlpha(backgroundColor.get(), a),
                PanelRender.withAlpha(outlineColor.get(), a),
                PanelRender.withAlpha(shadowColor.get(), a), 4,
                PanelRender.withAlpha(glowColor.get(), a * (0.6 + 0.4 * pop)));
        }

        // Number with a pop scale around its own center
        double p = pop * pop;
        float popScale = (float) (1 + popStrength.get() * p);
        float numCx = left + numW * NUM_SCALE / 2f;
        int numColor = PanelRender.lerpColor(p, numberColor.get(), flashColor.get());
        if (!active) numColor = PanelRender.lerpColor(shake, numColor, 0xFF808080);

        m.pushMatrix();
        m.translate(numCx, 0);
        m.scale(NUM_SCALE * popScale, NUM_SCALE * popScale);
        PanelRender.text(ctx, num, -numW / 2, -4, PanelRender.withAlpha(numColor, a), true);
        m.popMatrix();

        if (lblW > 0) {
            int lx = Math.round(left + numW * NUM_SCALE + 4);
            PanelRender.text(ctx, lbl, lx, 1, PanelRender.withAlpha(labelColor.get(), a), true);
        }

        m.popMatrix();
    }

    public enum Label {
        COMBO_RU("КОМБО", "КОМБО"),
        COMBO_EN("COMBO", "COMBO"),
        HITS("УДАРЫ", "УДАРЫ"),
        NONE("", "Без подписи");

        public final String text;
        private final String title;

        Label(String text, String title) {
            this.text = text;
            this.title = title;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:Label." + name(), title);
        }
    }
}
