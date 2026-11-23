package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static io.github.qan4ur.vetmos.modules.hud.HudRender2.*;

/**
 * Item cooldowns. Reads only {@link ItemCooldownManager#getCooldownProgress(ItemStack, float)}, i.e. exactly what the
 * vanilla hotbar overlay shows.
 */
public class CooldownsHud extends Module implements HudModule {
    public enum Layout {
        HORIZONTAL("Строка"), VERTICAL("Столбец");

        private final String title;

        Layout(String title) { this.title = title; }

        @Override
        public String toString() { return io.github.qan4ur.vetmos.util.Lang.tr("enum:Layout." + name(), title); }
    }

    public enum Style {
        RADIAL("Круговой"), SWEEP("Шторка"), NONE("Нет");

        private final String title;

        Style(String title) { this.title = title; }

        @Override
        public String toString() { return io.github.qan4ur.vetmos.util.Lang.tr("enum:Style." + name(), title); }
    }

    private static final String G = "Основное", I = "Предметы", C = "Цвета";

    private record Tracked(Item item, BoolSetting setting) {}

    private final EnumSetting<Layout> layout = enumeration(G, "layout", "Расположение", "Перезарядки в строку или в столбец.", Layout.HORIZONTAL);
    private final EnumSetting<Style> style = enumeration(G, "overlay", "Наложение", "Индикатор прогресса поверх иконки.", Style.RADIAL);
    private final BoolSetting bar = bool(G, "bar", "Полоска", "Полоска прогресса под каждым предметом.", true);
    private final BoolSetting seconds = bool(G, "seconds", "Секунды", "Показывать примерное оставшееся время.", true);
    private final BoolSetting background = bool(G, "background", "Фон", "Рисовать панель под HUD.", true);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    private final List<Tracked> trackedItems = List.of(
        new Tracked(Items.ENDER_PEARL, bool(I, "ender-pearl", "Эндер-жемчуг", "Показывать перезарядку эндер-жемчуга.", true)),
        new Tracked(Items.CHORUS_FRUIT, bool(I, "chorus-fruit", "Плод хоруса", "Показывать перезарядку плода хоруса.", true)),
        new Tracked(Items.TOTEM_OF_UNDYING, bool(I, "totem", "Тотем бессмертия", "Показывать перезарядку тотема (если сервер её задаёт).", true)),
        new Tracked(Items.SHIELD, bool(I, "shield", "Щит", "Показывать перезарядку щита после удара топором.", true)),
        new Tracked(Items.WIND_CHARGE, bool(I, "wind-charge", "Заряд ветра", "Показывать перезарядку заряда ветра.", true))
    );

    private final ColorSetting bgColor = color(C, "background-color", "Фон", "Цвет фона панели.", Theme.PANEL);
    private final ColorSetting accentColor = color(C, "accent", "Акцент", "Линия сверху и полоска прогресса.", Theme.SCARLET);
    private final ColorSetting overlayColor = color(C, "overlay-color", "Наложение", "Цвет оставшейся перезарядки на иконке.", ColorSetting.argb(90, 90, 90, 150));
    private final ColorSetting textColor = color(C, "text", "Текст", "Цвет секунд.", ColorSetting.rgb(200, 200, 200));

    private static final int SLOT = 20;
    private static final int PAD = 3;

    private static final class Track {
        Item item;
        ItemStack stack;
        float progress;
        float anim;
        boolean alive;
        // for the seconds estimate
        float startProgress;
        long startNs;
        float lastProgress;
        // editor preview: fixed length in seconds instead of the estimate
        double sampleSeconds;
    }

    private final Map<Item, Track> tracks = new LinkedHashMap<>();
    private final List<Track> visible = new ArrayList<>();
    private long lastFrameNs;

    // layout of the current frame, computed in update()
    private boolean horizontal;
    private int cellW, cellH, boxW = 1, boxH = 1;
    private long frameNs;

    public CooldownsHud() {
        super("cooldowns-hud", "Перезарядки", "Показывает перезарядку жемчуга, хоруса, тотема, щита и т.д.", Category.HUD);
    }

    @Override
    protected void onDisable() {
        tracks.clear();
        visible.clear();
        lastFrameNs = 0;
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.BOTTOM, 0, -85);
    }

    @Override
    public boolean shouldRenderHud() {
        update();
        return mc.player != null && !visible.isEmpty();
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
        frameNs = now;

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        for (Track t : tracks.values()) t.alive = false;
        boolean any = false;
        if (mc.player != null) {
            ItemCooldownManager cooldowns = mc.player.getItemCooldownManager();
            for (Tracked tracked : trackedItems) {
                if (!tracked.setting().get()) continue;
                Item item = tracked.item();
                ItemStack stack = findStack(item);
                float p = cooldowns.getCooldownProgress(stack, tickDelta);
                if (p <= 0) continue;

                Track t = tracks.computeIfAbsent(item, k -> new Track());
                if (t.item == null || p > t.lastProgress + 0.02f || t.startNs == 0) {
                    // new (or restarted) cooldown
                    t.startProgress = p;
                    t.startNs = now;
                }
                t.item = item;
                t.stack = stack;
                t.progress = p;
                t.lastProgress = p;
                t.sampleSeconds = 0;
                t.alive = true;
                any = true;
            }
        }

        // preview in the HUD editor when nothing is on cooldown: a looping pearl and chorus cooldown
        if (isEditor() && !any) {
            double time = now / 1e9;
            sample(Items.ENDER_PEARL, 3.0, time);
            sample(Items.CHORUS_FRUIT, 4.0, time + 1.3);
        }

        tracks.values().removeIf(t -> {
            t.anim = approach(t.anim, t.alive ? 1 : 0, dt, t.alive ? 14 : 10);
            if (!t.alive) t.startNs = 0;
            return !t.alive && t.anim < 0.02f;
        });

        visible.clear();
        if (tracks.isEmpty()) {
            boxW = boxH = 1;
            return;
        }
        visible.addAll(tracks.values());

        TextRenderer tr = mc.textRenderer;
        horizontal = layout.get() == Layout.HORIZONTAL;

        cellW = horizontal ? SLOT + 4 : SLOT + 4 + (seconds.get() ? tr.getWidth("00.0с") + 2 : 0);
        cellH = horizontal ? SLOT + (seconds.get() ? 10 : 0) + (bar.get() ? 3 : 0) + 2 : SLOT + 2;
        if (!horizontal && bar.get() && !seconds.get()) cellW += 24;

        float span = 0;
        for (Track t : visible) span += easeOutCubic(t.anim) * (horizontal ? cellW : cellH);
        boxW = Math.max(1, Math.round(horizontal ? span - 4 : cellW - 4)) + PAD * 2;
        boxH = Math.max(1, Math.round(horizontal ? cellH - 2 : span - 2)) + PAD * 2;
    }

    private void sample(Item item, double period, double time) {
        Track t = tracks.computeIfAbsent(item, k -> new Track());
        t.item = item;
        t.stack = item.getDefaultStack();
        t.progress = (float) (1 - (time % period) / period);
        t.lastProgress = t.progress;
        t.sampleSeconds = period;
        t.alive = true;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (visible.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int w = boxW, h = boxH;
        long now = frameNs;
        int accent = accentColor.get();

        Matrix3x2fStack m = ctx.getMatrices();

        float panelEase = 0;
        for (Track t : visible) panelEase = Math.max(panelEase, t.anim);
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accent, easeOutCubic(panelEase));

        float cursor = 0;
        for (Track t : visible) {
            float e = easeOutCubic(t.anim);
            float pop = t.alive ? easeOutBack(t.anim) : e;
            int alpha = (int) (255 * e);
            int cx = PAD + Math.round(horizontal ? cursor : 0);
            int cy = PAD + Math.round(horizontal ? 0 : cursor);

            m.pushMatrix();
            // scale the slot around its centre for the pop-in
            m.translate(cx + SLOT / 2f, cy + SLOT / 2f);
            float s = Math.max(0.01f, pop);
            m.scale(s, s);
            m.translate(-SLOT / 2f, -SLOT / 2f);

            ctx.fill(0, 0, SLOT, SLOT, withAlpha(0xFF000000, (int) (80 * e)));
            ctx.drawItem(t.stack, 2, 2);
            drawOverlay(ctx, t.progress, fade(overlayColor.get(), e));
            ctx.fill(0, SLOT - 1, SLOT, SLOT, withAlpha(accent, (int) (90 * e)));
            m.popMatrix();

            String secText = seconds.get() ? secondsLeft(t, now) : null;
            int tc = withAlpha(textColor.get(), alpha);
            int ac = withAlpha(accent, alpha);

            if (horizontal) {
                int ty = cy + SLOT + 1;
                if (secText != null) {
                    ctx.drawText(tr, secText, cx + (SLOT - tr.getWidth(secText)) / 2 + 1, ty, tc, shadow.get());
                    ty += 10;
                }
                if (bar.get()) drawBar(ctx, cx + 1, ty, SLOT - 2, t.progress, ac, e);
            } else {
                int tx = cx + SLOT + 4;
                if (secText != null) ctx.drawText(tr, secText, tx, cy + (bar.get() ? 3 : 6), tc, shadow.get());
                if (bar.get()) drawBar(ctx, tx, cy + (secText != null ? 14 : 9), cellW - SLOT - 8, t.progress, ac, e);
            }

            cursor += e * (horizontal ? cellW : cellH);
        }
    }

    private ItemStack findStack(Item item) {
        // prefer a real stack (servers can give items custom cooldown groups), fall back to the default stack
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) return stack;
        }
        return item.getDefaultStack();
    }

    private void drawOverlay(DrawContext ctx, float progress, int color) {
        progress = MathHelper.clamp(progress, 0, 1);
        switch (style.get()) {
            case SWEEP -> {
                int top = Math.round(SLOT * (1 - progress));
                ctx.fill(0, top, SLOT, SLOT, color);
            }
            case RADIAL -> {
                // clockwise pie from 12 o'clock; the covered part is the remaining cooldown. One fill per run of pixels per row.
                double startAngle = (1 - progress) * Math.PI * 2;
                for (int py = 0; py < SLOT; py++) {
                    int runStart = -1;
                    for (int px = 0; px <= SLOT; px++) {
                        boolean covered = false;
                        if (px < SLOT) {
                            double dx = px + 0.5 - SLOT / 2.0, dy = py + 0.5 - SLOT / 2.0;
                            double a = Math.atan2(dx, -dy);
                            if (a < 0) a += Math.PI * 2;
                            covered = a >= startAngle;
                        }
                        if (covered && runStart < 0) runStart = px;
                        else if (!covered && runStart >= 0) {
                            ctx.fill(runStart, py, px, py + 1, color);
                            runStart = -1;
                        }
                    }
                }
            }
            case NONE -> {
            }
        }
    }

    private static void drawBar(DrawContext ctx, int x, int y, int w, float pct, int color, float ease) {
        ctx.fill(x, y, x + w, y + 2, withAlpha(0xFF000000, (int) (150 * ease)));
        int fw = Math.round(w * MathHelper.clamp(pct, 0, 1));
        if (fw > 0) ctx.fill(x, y, x + fw, y + 2, color);
    }

    private static String secondsLeft(Track t, long now) {
        if (!t.alive) return "0.0с";
        if (t.sampleSeconds > 0) return String.format(Locale.ROOT, "%.1fс", t.progress * t.sampleSeconds);
        double elapsed = (now - t.startNs) / 1e9;
        double used = t.startProgress - t.progress;
        double remaining;
        if (elapsed > 0.1 && used > 0.005) {
            double total = elapsed / used;
            remaining = t.progress * total;
        } else {
            Double total = defaultSeconds(t.item);
            if (total == null) return "...";
            remaining = t.progress * total;
        }
        return String.format(Locale.ROOT, "%.1fс", Math.max(0, remaining));
    }

    /** Vanilla cooldown lengths, used only until the rate-based estimate kicks in (~0.1s). */
    private static Double defaultSeconds(Item item) {
        if (item == Items.ENDER_PEARL || item == Items.CHORUS_FRUIT) return 1.0;
        if (item == Items.SHIELD) return 5.0;
        if (item == Items.WIND_CHARGE) return 0.5;
        return null;
    }
}
