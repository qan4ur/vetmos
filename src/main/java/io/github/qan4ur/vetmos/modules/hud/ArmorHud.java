package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

import static io.github.qan4ur.vetmos.modules.hud.HudRender2.*;

/** Your armor and held items with durability. */
public class ArmorHud extends Module implements HudModule {
    public enum Layout {
        HORIZONTAL("Строка"), VERTICAL("Столбец");

        private final String title;

        Layout(String title) { this.title = title; }

        @Override
        public String toString() { return title; }
    }

    public enum Display {
        PERCENT("Проценты"), BAR("Полоска"), BOTH("Оба");

        private final String title;

        Display(String title) { this.title = title; }

        @Override
        public String toString() { return title; }
    }

    private static final String G = "Основное", C = "Цвета";

    private final EnumSetting<Layout> layout = enumeration(G, "layout", "Расположение", "Предметы в строку или в столбец.", Layout.HORIZONTAL);
    private final EnumSetting<Display> display = enumeration(G, "durability", "Прочность", "Как показывать прочность.", Display.BOTH);
    private final BoolSetting mainHand = bool(G, "main-hand", "Основная рука", "Показывать предмет в основной руке.", true);
    private final BoolSetting offHand = bool(G, "off-hand", "Вторая рука", "Показывать предмет во второй руке.", false);
    private final BoolSetting hideEmpty = bool(G, "hide-empty", "Скрывать пустые", "Не рисовать пустые слоты.", false);
    private final IntSetting warnThreshold = integer(G, "warn-threshold", "Порог тревоги", "Предметы с прочностью ниже этого % мигают красным.", 20, 0, 100);
    private final BoolSetting blink = bool(G, "blink", "Мигание", "Пульсация предметов с низкой прочностью.", true);
    private final DoubleSetting blinkSpeed = decimal(G, "blink-speed", "Скорость мигания", "Вспышек в секунду.", 2, 0.2, 8, 1)
        .visibleWhen(blink::get);
    private final BoolSetting background = bool(G, "background", "Фон", "Рисовать панель под HUD.", true);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting bgColor = color(C, "background-color", "Фон", "Цвет фона панели.", Theme.PANEL);
    private final ColorSetting accentColor = color(C, "accent", "Акцент", "Линия сверху и рамки слотов.", Theme.SCARLET);
    private final ColorSetting highColor = color(C, "full-durability", "Полная прочность", "Цвет текста и полоски при полной прочности.", ColorSetting.rgb(235, 235, 235));
    private final ColorSetting lowColor = color(C, "low-durability", "Нулевая прочность", "Цвет текста и полоски при нулевой прочности.", Theme.DARK_RED);
    private final ColorSetting warnColor = color(C, "warning", "Тревога", "Цвет мигания предметов ниже порога.", Theme.GLOW);
    private final ColorSetting textColor = color(C, "text", "Текст", "Цвет количества предметов.", Theme.TEXT);

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final int SLOT = 20;
    private static final int PAD = 3;

    private final List<ItemStack> stacks = new ArrayList<>();
    private List<ItemStack> sampleStacks;
    private float appear;
    private long lastFrameNs;

    // layout of the current frame, computed in update()
    private boolean horizontal, showText, showBar;
    private int cellW, cellH, boxW = 1, boxH = 1;
    private float ease;

    public ArmorHud() {
        super("armor-hud", "Броня", "Показывает броню и предметы в руках с прочностью.", Category.HUD);
    }

    @Override
    protected void onEnable() {
        appear = 0;
        lastFrameNs = 0;
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.BOTTOM, 0, -60);
    }

    @Override
    public boolean shouldRenderHud() {
        update();
        return mc.player != null && !stacks.isEmpty();
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

    /** Recomputes the per-frame state; cheap, so it may run more than once per frame. */
    private void update() {
        long now = System.nanoTime();
        double frameTime = lastFrameNs == 0 ? 0 : Math.min((now - lastFrameNs) / 1e9, 0.1);
        lastFrameNs = now;

        stacks.clear();
        if (mc.player != null) {
            for (EquipmentSlot slot : ARMOR) addStack(mc.player.getEquippedStack(slot));
            if (mainHand.get()) addStack(mc.player.getMainHandStack());
            if (offHand.get()) addStack(mc.player.getOffHandStack());
        }

        if (isEditor() && stacks.stream().allMatch(ItemStack::isEmpty)) {
            stacks.clear();
            List<ItemStack> samples = samples();
            for (int i = 0; i < samples.size(); i++) {
                if (i == 4 && !mainHand.get()) continue;
                if (i == 5 && !offHand.get()) continue;
                stacks.add(samples.get(i));
            }
        }

        if (stacks.isEmpty()) {
            boxW = boxH = 1;
            return;
        }

        appear = approach(appear, 1, frameTime, 8);
        ease = easeOutCubic(appear);

        TextRenderer tr = mc.textRenderer;
        horizontal = layout.get() == Layout.HORIZONTAL;
        showText = display.get() != Display.BAR;
        showBar = display.get() != Display.PERCENT;

        if (horizontal) {
            cellW = SLOT + 2;
            cellH = SLOT + (showText ? 10 : 0) + (showBar ? 3 : 0);
        } else {
            cellW = SLOT + 4 + (showText ? tr.getWidth("100%") + 2 : 0) + (showBar && !showText ? 24 : 0);
            cellH = SLOT + 2;
        }

        int count = stacks.size();
        boxW = (horizontal ? cellW * count - 2 : cellW) + PAD * 2;
        boxH = (horizontal ? cellH : cellH * count - 2) + PAD * 2;
    }

    private List<ItemStack> samples() {
        if (sampleStacks == null) {
            sampleStacks = new ArrayList<>();
            sampleStacks.add(damaged(Items.NETHERITE_HELMET, 0.92f));
            sampleStacks.add(damaged(Items.NETHERITE_CHESTPLATE, 0.74f));
            sampleStacks.add(damaged(Items.DIAMOND_LEGGINGS, 0.48f));
            sampleStacks.add(damaged(Items.DIAMOND_BOOTS, 0.15f));
            sampleStacks.add(damaged(Items.NETHERITE_SWORD, 0.86f));
            sampleStacks.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        return sampleStacks;
    }

    private static ItemStack damaged(Item item, float durability) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageable()) stack.setDamage(Math.round(stack.getMaxDamage() * (1 - durability)));
        return stack;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (stacks.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int w = boxW, h = boxH;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0, (1 - ease) * 6);

        int alpha = (int) (255 * ease);
        int accent = accentColor.get();
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accent, ease);

        float pulse = pulse(blinkSpeed.get());
        int count = stacks.size();

        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            int cx = PAD + (horizontal ? i * cellW : 0);
            int cy = PAD + (horizontal ? 0 : i * cellH);

            boolean damageable = !stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0;
            float pct = damageable ? 1f - (float) stack.getDamage() / stack.getMaxDamage() : 1f;
            int pctInt = Math.round(pct * 100);
            boolean warn = damageable && pctInt < warnThreshold.get();

            // slot frame
            int frame = withAlpha(accent, (int) (60 * ease));
            ctx.fill(cx, cy, cx + SLOT, cy + SLOT, withAlpha(0xFF000000, (int) (70 * ease)));
            ctx.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, frame);

            if (warn) {
                float k = blink.get() ? pulse : 1f;
                int wc = warnColor.get();
                ctx.fill(cx, cy, cx + SLOT, cy + SLOT, withAlpha(wc, (int) ((wc >>> 24) * k * ease)));
                drawOutline(ctx, cx, cy, SLOT, SLOT, withAlpha(accent, (int) (255 * k * ease)));
            }

            if (!stack.isEmpty()) ctx.drawItem(stack, cx + 2, cy + 2);

            int color = lerpColor(lowColor.get(), highColor.get(), pct);
            if (warn && blink.get()) color = lerpColor(color, 0xFFFFFFFF, pulse * 0.6f);
            color = withAlpha(color, alpha);

            if (!damageable && stack.getCount() > 1) {
                String c = String.valueOf(stack.getCount());
                // drawn after the item so it stays on top
                ctx.drawText(tr, c, cx + SLOT - tr.getWidth(c), cy + SLOT - 8, withAlpha(textColor.get(), alpha), shadow.get());
            }

            if (!damageable) continue;

            String text = pctInt + "%";
            if (horizontal) {
                int ty = cy + SLOT + 1;
                if (showText) {
                    ctx.drawText(tr, text, cx + (SLOT - tr.getWidth(text)) / 2 + 1, ty, color, shadow.get());
                    ty += 10;
                }
                if (showBar) drawBar(ctx, cx + 1, ty, SLOT - 2, pct, color, ease);
            } else {
                int tx = cx + SLOT + 4;
                if (showText) ctx.drawText(tr, text, tx, cy + (showBar ? 3 : 6), color, shadow.get());
                if (showBar) {
                    int bw = cellW - SLOT - 6;
                    drawBar(ctx, tx, cy + (showText ? 14 : 9), bw, pct, color, ease);
                }
            }
        }

        m.popMatrix();
    }

    private void addStack(ItemStack stack) {
        if (stack.isEmpty() && hideEmpty.get()) return;
        stacks.add(stack);
    }

    private static void drawBar(DrawContext ctx, int x, int y, int w, float pct, int color, float ease) {
        ctx.fill(x, y, x + w, y + 2, withAlpha(0xFF000000, (int) (150 * ease)));
        int fw = Math.max(0, Math.round(w * MathHelper.clamp(pct, 0, 1)));
        if (fw > 0) ctx.fill(x, y, x + fw, y + 2, color);
    }
}
