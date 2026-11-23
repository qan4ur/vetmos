package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Counts important PvP items in the inventory (including the offhand) and shows them with icons. */
public class ItemCounter extends Module implements HudModule {
    public enum Layout {
        HORIZONTAL("Строка"), VERTICAL("Столбец");

        private final String title;

        Layout(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:ItemCounter.Layout." + name(), title); }
    }

    public enum Zero {
        SHOW("Показывать"), GRAY("Затемнять"), HIDE("Скрывать");

        private final String title;

        Zero(String title) { this.title = title; }

        @Override
        public String toString() { return Lang.tr("enum:ItemCounter.Zero." + name(), title); }
    }

    private static final String G = "Основное", I = "Предметы", T = "Пороги", C = "Цвета";

    /** One counted group: its toggle, low-count threshold, icon and the items it includes. */
    private final class Group {
        final BoolSetting show;
        final IntSetting low;
        final ItemStack icon;
        final Item[] items;
        final int preview;
        int count;

        Group(String key, String title, boolean on, int threshold, int preview, Item... items) {
            this.show = bool(I, key, title, "Считать: " + title.toLowerCase() + ".", on);
            this.low = integer(T, "low-" + key, title, "Количество, при котором и ниже счётчик подсвечивается тревогой.", threshold, 0, 64);
            this.low.visibleWhen(() -> show.get() && warn.get());
            this.icon = new ItemStack(items[0]);
            this.items = items;
            this.preview = preview;
        }

        boolean matches(Item item) {
            for (Item i : items) if (i == item) return true;
            return false;
        }
    }

    private final EnumSetting<Layout> layout = enumeration(G, "layout", "Расположение", "Предметы в строку или в столбец.", Layout.HORIZONTAL);
    private final EnumSetting<Zero> zero = enumeration(G, "zero", "Нулевые", "Что делать с предметами, которых нет.", Zero.GRAY);
    private final BoolSetting warn = bool(G, "low-warning", "Предупреждение", "Подсвечивать счётчик, когда предметов мало (пороги ниже).", true);
    private final BoolSetting blink = bool(G, "blink", "Мигание", "Пульсация при малом количестве.", true).visibleWhen(warn::get);
    private final BoolSetting background = bool(G, "background", "Фон", "Рисовать панель под HUD.", true);
    private final BoolSetting accentLine = bool(G, "accent-line", "Акцентная линия", "Тонкая линия цвета акцента сверху панели.", true)
        .visibleWhen(background::get);
    private final BoolSetting shadow = bool(G, "text-shadow", "Тень текста", "Рисовать текст с тенью.", true);
    private final DoubleSetting scale = hudScaleSetting();

    private final ColorSetting bgColor = color(C, "background-color", "Фон", "Цвет панели.", Theme.PANEL).visibleWhen(background::get);
    private final ColorSetting textColor = color(C, "text", "Текст", "Цвет количества.", Theme.TEXT);
    private final ColorSetting zeroColor = color(C, "zero-color", "Нулевые", "Цвет счётчика, когда предметов нет.", Theme.TEXT_MUTED);
    private final ColorSetting warnColor = color(C, "warning", "Тревога", "Цвет счётчика при малом количестве.", Theme.WARNING)
        .visibleWhen(warn::get);

    private final List<Group> groups = new ArrayList<>();
    private final List<Group> visible = new ArrayList<>();

    private static final int ICON = 18, PAD = 4;
    private int cellW, cellH, boxW = 1, boxH = 1;
    private boolean horizontal;

    public ItemCounter() {
        super("item-counter", "Счётчик предметов", "Считает жемчуг, тотемы, яблоки, стрелы и другие предметы в инвентаре.", Category.HUD);
        groups.add(new Group("totems", "Тотемы", true, 1, 3, Items.TOTEM_OF_UNDYING));
        groups.add(new Group("pearls", "Эндер-жемчуг", true, 3, 14, Items.ENDER_PEARL));
        groups.add(new Group("golden-apples", "Золотые яблоки", true, 4, 32, Items.GOLDEN_APPLE));
        groups.add(new Group("enchanted-apples", "Зачарованные яблоки", true, 1, 0, Items.ENCHANTED_GOLDEN_APPLE));
        groups.add(new Group("arrows", "Стрелы", true, 16, 128, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW));
        groups.add(new Group("wind-charges", "Заряды ветра", true, 4, 2, Items.WIND_CHARGE));
        groups.add(new Group("xp-bottles", "Пузырьки опыта", true, 16, 64, Items.EXPERIENCE_BOTTLE));
        // optional extras (off by default)
        groups.add(new Group("end-crystals", "Кристаллы Края", false, 4, 24, Items.END_CRYSTAL));
        groups.add(new Group("obsidian", "Обсидиан", false, 16, 64, Items.OBSIDIAN));
        groups.add(new Group("respawn-anchors", "Якоря возрождения", false, 2, 8, Items.RESPAWN_ANCHOR));
        groups.add(new Group("glowstone", "Светокамень", false, 4, 32, Items.GLOWSTONE));
        groups.add(new Group("cobwebs", "Паутина", false, 4, 16, Items.COBWEB));
        groups.add(new Group("fireworks", "Фейерверки", false, 8, 48, Items.FIREWORK_ROCKET));
        groups.add(new Group("chorus", "Плоды хоруса", false, 4, 16, Items.CHORUS_FRUIT));
        groups.add(new Group("splash-potions", "Взрывные зелья", false, 2, 6, Items.SPLASH_POTION));
        groups.add(new Group("golden-carrots", "Золотая морковь", false, 8, 64, Items.GOLDEN_CARROT));
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.RIGHT, HudPosition.AnchorY.BOTTOM, -4, -4);
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
        boolean preview = mc.player == null || mc.world == null;
        for (Group g : groups) g.count = 0;
        if (!preview) {
            PlayerInventory inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.isEmpty()) continue;
                Item item = s.getItem();
                for (Group g : groups) if (g.show.get() && g.matches(item)) g.count += s.getCount();
            }
        } else {
            for (Group g : groups) g.count = g.preview;
        }

        boolean editor = HudRender2.isEditor();
        visible.clear();
        for (Group g : groups) {
            if (!g.show.get()) continue;
            if (g.count == 0 && zero.get() == Zero.HIDE && !editor) continue;
            visible.add(g);
        }

        TextRenderer tr = mc.textRenderer;
        int maxText = 0;
        for (Group g : visible) maxText = Math.max(maxText, tr.getWidth(String.valueOf(g.count)));
        horizontal = layout.get() == Layout.HORIZONTAL;
        if (horizontal) {
            cellW = Math.max(ICON, maxText + 2) + 2;
            cellH = ICON + 9;
        } else {
            cellW = ICON + 3 + Math.max(maxText, 6);
            cellH = ICON + 1;
        }
        int n = visible.size();
        if (n == 0) {
            boxW = boxH = 1;
            return;
        }
        int line = background.get() && accentLine.get() ? 1 : 0;
        boxW = (horizontal ? cellW * n - 2 : cellW) + PAD * 2;
        boxH = (horizontal ? cellH : cellH * n - 1) + PAD * 2 + line;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (visible.isEmpty()) return;
        TextRenderer tr = mc.textRenderer;
        boolean line = background.get() && accentLine.get();
        if (background.get()) {
            PanelRender.roundedRect(ctx, 0, 0, boxW, boxH, 2, bgColor.get());
            if (line) PanelRender.hGradient(ctx, 2, 0, boxW - 2, 1, Theme.accentAt(0), Theme.accentAt(1));
        }

        float pulse = blink.get() ? HudRender2.pulse(2) : 1f;
        boolean sh = shadow.get();
        int top = PAD + (line ? 1 : 0);

        for (int i = 0; i < visible.size(); i++) {
            Group g = visible.get(i);
            int cx = PAD + (horizontal ? i * cellW : 0);
            int cy = top + (horizontal ? 0 : i * cellH);
            int ix = horizontal ? cx + (cellW - 2 - 16) / 2 : cx + 1;
            int iy = cy + 1;

            boolean empty = g.count == 0;
            boolean low = warn.get() && (empty ? zero.get() == Zero.SHOW : g.count <= g.low.get());

            if (low) {
                int wc = warnColor.get();
                ctx.fill(ix - 1, iy - 1, ix + 17, iy + 17, HudRender2.withAlpha(wc, (int) (30 + 50 * pulse)));
            }

            ctx.drawItem(g.icon, ix, iy);
            if (empty && zero.get() == Zero.GRAY) ctx.fill(ix, iy, ix + 16, iy + 16, 0x9A0C0C0C); // drawn on a layer above the item

            int color;
            if (low) color = HudRender2.lerpColor(warnColor.get(), Theme.WHITE, blink.get() ? pulse * 0.35f : 0f);
            else if (empty && zero.get() == Zero.GRAY) color = zeroColor.get();
            else color = textColor.get();

            String s = String.valueOf(g.count);
            if (horizontal) {
                PanelRender.text(ctx, s, cx + (cellW - 2 - tr.getWidth(s)) / 2 + 1, cy + ICON + 1, color, sh);
            } else {
                PanelRender.text(ctx, s, cx + ICON + 3, cy + 5, color, sh);
            }
        }
    }
}
