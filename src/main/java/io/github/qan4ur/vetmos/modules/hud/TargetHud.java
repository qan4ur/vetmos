package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudModule;
import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.Arrays;
import java.util.Locale;

/** A panel with the face, health, armor and distance of the player you are fighting. */
public class TargetHud extends Module implements HudModule {
    private static final String G_MAIN = "Основное";
    private static final String G_PARTS = "Элементы";
    private static final String G_COLORS = "Цвета";

    // ---------------------------------------------------------------- general

    private final BoolSetting playersOnly = bool(G_MAIN, "players-only", "Только игроки",
        "Следить только за игроками. Иначе показывается любое существо, которое вы бьёте.", true);

    private final BoolSetting crosshairTarget = bool(G_MAIN, "crosshair-target", "Цель под прицелом", "Также брать существо под прицелом.", true);

    private final DoubleSetting timeout = decimal(G_MAIN, "timeout", "Сброс через", "Секунд без боя, после которых цель пропадает.", 4.0, 0.5, 30.0, 1);

    private final DoubleSetting range = decimal(G_MAIN, "range", "Дистанция", "Цель пропадает, если она дальше этого расстояния.", 16.0, 3.0, 128.0, 1);

    private final BoolSetting previewInChat = bool(G_MAIN, "preview-in-chat", "Превью в чате", "Показывает вас самих, пока открыт чат.", true);

    private final BoolSetting showInScreens = bool(G_MAIN, "show-in-screens", "Показывать в меню",
        "Рисовать, даже когда открыто меню (инвентарь и т.п., кроме чата).", false);

    private final DoubleSetting animationSpeed = decimal(G_MAIN, "animation-speed", "Скорость анимации", "Скорость полоски здоровья и появления.", 1.0, 0.2, 5.0);

    private final DoubleSetting scale = hudScaleSetting();

    // ---------------------------------------------------------------- parts

    private final BoolSetting showHead = bool(G_PARTS, "head", "Голова", "Показывает лицо скина игрока.", true);

    private final BoolSetting showName = bool(G_PARTS, "name", "Ник", "Показывает имя цели.", true);

    private final BoolSetting showBar = bool(G_PARTS, "health-bar", "Полоска здоровья", "Анимированная полоска здоровья.", true);

    private final BoolSetting showTrail = bool(G_PARTS, "damage-trail", "След урона", "Отстающая полоска, показывающая недавно потерянное здоровье.", true)
        .visibleWhen(showBar::get);

    private final BoolSetting showHealthText = bool(G_PARTS, "health-number", "Число здоровья", "Показывает здоровье числом.", true);

    private final BoolSetting showAbsorption = bool(G_PARTS, "absorption", "Поглощение", "Показывает поглощение на полоске и в числе.", true);

    private final BoolSetting showArmor = bool(G_PARTS, "armor", "Броня", "Показывает надетую броню цели.", true);

    private final BoolSetting showHeld = bool(G_PARTS, "held-item", "Предмет в руке", "Показывает предмет в основной руке рядом с бронёй.", true)
        .visibleWhen(showArmor::get);

    private final BoolSetting showDurability = bool(G_PARTS, "durability", "Прочность", "Полоски прочности на иконках предметов.", true)
        .visibleWhen(showArmor::get);

    private final BoolSetting showDistance = bool(G_PARTS, "distance", "Расстояние", "Показывает расстояние до цели.", true);

    // ---------------------------------------------------------------- colors

    private final ColorSetting backgroundColor = color(G_COLORS, "background", "Фон", "Фон панели.", Theme.PANEL);
    private final ColorSetting outlineColor = color(G_COLORS, "outline", "Обводка", "Обводка панели.", ColorSetting.argb(240, 240, 240, 110));
    private final ColorSetting glowColor = color(G_COLORS, "glow", "Свечение", "Свечение кровавой луны вдоль верхнего края.", Theme.GLOW);
    private final ColorSetting shadowColor = color(G_COLORS, "shadow", "Тень", "Мягкая тень под панелью.", ColorSetting.argb(0, 0, 0, 90));
    private final ColorSetting barStartColor = color(G_COLORS, "bar-start", "Полоска: начало", "Левый цвет полоски здоровья.", Theme.DARK_RED);
    private final ColorSetting barEndColor = color(G_COLORS, "bar-end", "Полоска: конец", "Правый цвет полоски здоровья.", Theme.SCARLET);
    private final ColorSetting trailColor = color(G_COLORS, "trail", "След урона", "Цвет следа урона.", ColorSetting.argb(170, 170, 170, 200));
    private final ColorSetting absorptionColor = color(G_COLORS, "absorption", "Поглощение", "Цвет поглощения на полоске.", ColorSetting.argb(255, 120, 60, 220));
    private final ColorSetting barBackgroundColor = color(G_COLORS, "bar-background", "Фон полоски", "Пустая часть полоски здоровья.", ColorSetting.argb(40, 10, 14, 220));
    private final ColorSetting textColor = color(G_COLORS, "text", "Текст", "Основной цвет текста.", Theme.TEXT);
    private final ColorSetting secondaryTextColor = color(G_COLORS, "secondary-text", "Доп. текст", "Цвет числа здоровья и расстояния.", Theme.TEXT_SECONDARY);
    private final ColorSetting accentColor = color(G_COLORS, "accent", "Акцент", "Рамка головы и вспышка при ударе.", Theme.SCARLET);

    // ---------------------------------------------------------------- state

    private LivingEntity target;
    private long lastCombat;

    // Snapshot of the last shown entity (kept so the panel can fade out after the target is gone)
    private int shownId = Integer.MIN_VALUE;
    private boolean hasSnapshot;
    private String name = "";
    private float health, maxHealth = 20, absorption;
    private int hurtTime;
    private SkinTextures skin;
    private float distance;
    private final ItemStack[] items = new ItemStack[5];

    // Animation
    private final PanelRender.FrameClock clock = new PanelRender.FrameClock();
    private double fade;
    private double dispHealth, trailHealth, dispAbsorption;
    private double lastHealth;
    private double sinceDamage;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public TargetHud() {
        super("target-hud", "Таргет HUD", "Панель со здоровьем и снаряжением игрока, с которым вы сражаетесь.", Category.HUD);
    }

    private void resetState() {
        target = null;
        hasSnapshot = false;
        shownId = Integer.MIN_VALUE;
        fade = 0;
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
        if (isValid(entity)) {
            target = (LivingEntity) entity;
            lastCombat = System.currentTimeMillis();
        }
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player) return false;
        if (!living.isAlive()) return false;
        return !playersOnly.get() || entity instanceof PlayerEntity;
    }

    private void updateTarget() {
        if (mc.player == null) {
            target = null;
            return;
        }

        if (crosshairTarget.get() && isValid(mc.targetedEntity)) {
            if (target != mc.targetedEntity) target = (LivingEntity) mc.targetedEntity;
            lastCombat = System.currentTimeMillis();
        }

        if (target != null) {
            boolean gone = target.isRemoved() || target.isDead() || target.getWorld() != mc.world;
            boolean far = mc.player.distanceTo(target) > range.get();
            boolean timedOut = System.currentTimeMillis() - lastCombat > timeout.get() * 1000;
            if (gone || far || timedOut) target = null;
        }
    }

    private void snapshot(LivingEntity e) {
        if (e.getId() != shownId || !hasSnapshot) {
            shownId = e.getId();
            dispHealth = trailHealth = lastHealth = e.getHealth();
            dispAbsorption = e.getAbsorptionAmount();
            sinceDamage = 10;
        }
        hasSnapshot = true;
        name = e.getName().getString();
        health = Math.max(0, e.getHealth());
        maxHealth = Math.max(1, e.getMaxHealth());
        absorption = Math.max(0, e.getAbsorptionAmount());
        hurtTime = e.hurtTime;
        skin = e instanceof AbstractClientPlayerEntity p ? p.getSkinTextures() : null;
        distance = e == mc.player ? 0 : mc.player.distanceTo(e);
        for (int i = 0; i < 4; i++) items[i] = e.getEquippedStack(ARMOR_SLOTS[i]);
        items[4] = e.getEquippedStack(EquipmentSlot.MAINHAND);
    }

    // ---------------------------------------------------------------- animation (driven every frame by the HUD)

    private void update() {
        double dt = clock.tick();
        if (mc.player == null) return;
        double speed = animationSpeed.get();

        updateTarget();

        LivingEntity shown = target;
        if (shown == null && previewInChat.get() && PanelRender.isChatOpen()) shown = mc.player;
        if (shown != null) snapshot(shown);

        fade = PanelRender.approach(fade, shown != null ? 1 : 0, 10 * speed, dt);
        if (fade < 0.004) {
            fade = 0;
            if (shown == null) hasSnapshot = false;
        }

        if (hasSnapshot) {
            if (health < lastHealth - 0.01) sinceDamage = 0;
            else sinceDamage += dt;
            lastHealth = health;

            dispHealth = PanelRender.approach(dispHealth, health, 12 * speed, dt);
            dispAbsorption = PanelRender.approach(dispAbsorption, absorption, 12 * speed, dt);
            if (trailHealth < dispHealth) trailHealth = dispHealth;
            else if (sinceDamage > 0.35 / speed) trailHealth = PanelRender.approach(trailHealth, dispHealth, 4 * speed, dt);
        }
    }

    /** Fills the snapshot with yourself (or a dummy without a world) for the HUD editor preview. */
    private void previewSnapshot() {
        if (mc.player != null) {
            snapshot(mc.player);
            return;
        }
        shownId = Integer.MIN_VALUE;
        name = "Vetmos";
        health = 14;
        maxHealth = 20;
        absorption = 4;
        hurtTime = 0;
        skin = null;
        distance = 3.2f;
        Arrays.fill(items, ItemStack.EMPTY);
        dispHealth = lastHealth = health;
        trailHealth = 17;
        dispAbsorption = absorption;
    }

    // ---------------------------------------------------------------- HUD

    @Override
    public boolean shouldRenderHud() {
        update();
        return hasSnapshot && fade > 0 && PanelRender.shouldRender(showInScreens.get());
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (inEditor) update(); // otherwise shouldRenderHud() already updated this frame
        boolean preview = inEditor && (!hasSnapshot || fade <= 0);
        if (preview) previewSnapshot();
        else if (!hasSnapshot || fade <= 0) return;

        render(ctx, preview ? 1 : fade);
    }

    @Override
    public double getHudWidth() {
        layout();
        return layoutW;
    }

    @Override
    public double getHudHeight() {
        layout();
        return layoutH;
    }

    @Override
    public DoubleSetting hudScale() {
        return scale;
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.CENTER, HudPosition.AnchorY.CENTER, 0, 60);
    }

    // ---------------------------------------------------------------- layout (local, unscaled units)

    private static final int PAD = 6;
    private static final int HEAD_SIZE = 32;

    private int layoutW = 140, layoutH = 44;
    private int contentX, contentW, nameY, barY, bottomY, distW, itemCount, itemsW;
    private String shownName = "", distText = "", hpText = "";

    private void layout() {
        TextRenderer tr = mc.textRenderer;
        if (tr == null) return;
        boolean head = showHead.get();
        boolean bottomRow = showHealthText.get() || showArmor.get();

        contentX = head ? PAD + HEAD_SIZE + 7 : PAD;
        distText = String.format(Locale.ROOT, "%.1fм", distance);
        distW = showDistance.get() ? tr.getWidth(distText) : 0;

        int nameMax = 120;
        shownName = name.isEmpty() && mc.player != null ? mc.player.getName().getString() : name;
        if (tr.getWidth(shownName) > nameMax) shownName = tr.trimToWidth(shownName, nameMax - tr.getWidth("...")) + "...";
        int nameW = showName.get() ? tr.getWidth(shownName) : 0;

        itemCount = showArmor.get() ? (showHeld.get() ? 5 : 4) : 0;
        itemsW = itemCount * 13;
        hpText = healthText();
        int hpW = showHealthText.get() ? tr.getWidth(hpText) + 6 : 0;

        contentW = Math.max(98, Math.max(nameW + (distW > 0 ? distW + 8 : 0), hpW + itemsW));
        layoutW = contentX + contentW + PAD;

        int cy = PAD;
        nameY = cy;
        if (showName.get() || showDistance.get()) cy += 12;
        barY = cy;
        if (showBar.get()) cy += 10;
        bottomY = cy;
        if (bottomRow) cy += 13;
        int h = Math.max(head ? PAD * 2 + HEAD_SIZE : 0, cy + PAD - 1);
        if (h <= PAD * 2) h = PAD * 2 + 8;
        layoutH = h;
    }

    private void render(DrawContext ctx, double fade) {
        TextRenderer tr = mc.textRenderer;
        layout();

        boolean head = showHead.get();
        boolean bottomRow = showHealthText.get() || showArmor.get();
        int w = layoutW, h = layoutH;

        // open animation, scaled around the box center
        double eased = PanelRender.easeOutCubic(fade);
        double animScale = 0.88 + 0.12 * eased;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(w / 2f, h / 2f);
        m.scale((float) animScale, (float) animScale);
        m.translate(-w / 2f, -h / 2f);

        double a = fade;
        int bg = PanelRender.withAlpha(backgroundColor.get(), a);
        int outline = PanelRender.withAlpha(outlineColor.get(), a);
        int glow = PanelRender.withAlpha(glowColor.get(), a);
        int shadow = PanelRender.withAlpha(shadowColor.get(), a);
        int accent = PanelRender.withAlpha(accentColor.get(), a);
        int text = PanelRender.withAlpha(textColor.get(), a);
        int text2 = PanelRender.withAlpha(secondaryTextColor.get(), a);

        PanelRender.panel(ctx, 0, 0, w, h, 4, bg, outline, shadow, 4, glow);

        double hurt = hurtTime / 10.0;

        // head
        if (head) {
            int hx = PAD, hy = (h - HEAD_SIZE) / 2;
            int frame = PanelRender.lerpColor(hurt, PanelRender.withAlpha(0xFF2A0A0E, a), accent);
            PanelRender.roundedRect(ctx, hx - 1, hy - 1, hx + HEAD_SIZE + 1, hy + HEAD_SIZE + 1, 2, frame);
            if (skin != null) {
                int tint = PanelRender.lerpColor(hurt * 0.85, 0xFFFFFFFF, 0xFFFF5050);
                PlayerSkinDrawer.draw(ctx, skin, hx, hy, HEAD_SIZE, PanelRender.withAlpha(tint, a));
            } else {
                ctx.fill(hx, hy, hx + HEAD_SIZE, hy + HEAD_SIZE, PanelRender.withAlpha(0xFF3A0A12, a));
                String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
                m.pushMatrix();
                m.translate(hx + HEAD_SIZE / 2f, hy + HEAD_SIZE / 2f);
                m.scale(2, 2);
                PanelRender.text(ctx, letter, -tr.getWidth(letter) / 2, -4, text, true);
                m.popMatrix();
            }
        }

        // name / distance
        if (showName.get()) PanelRender.text(ctx, shownName, contentX, nameY, text, true);
        if (showDistance.get()) PanelRender.text(ctx, distText, contentX + contentW - distW, nameY, text2, true);

        // health bar
        if (showBar.get()) {
            int bx1 = contentX, bx2 = contentX + contentW;
            int by1 = barY, by2 = barY + 6;
            int bw = bx2 - bx1;

            PanelRender.roundedRect(ctx, bx1, by1, bx2, by2, 2, PanelRender.withAlpha(barBackgroundColor.get(), a));

            double ratio = PanelRender.clamp01(dispHealth / maxHealth);
            double trailRatio = PanelRender.clamp01(trailHealth / maxHealth);

            if (showTrail.get() && trailRatio > ratio) {
                int tx2 = bx1 + (int) Math.round(bw * trailRatio);
                PanelRender.roundedRect(ctx, bx1, by1, tx2, by2, 2, PanelRender.withAlpha(trailColor.get(), a));
            }

            int fx2 = bx1 + (int) Math.round(bw * ratio);
            if (fx2 > bx1) {
                int c1 = PanelRender.withAlpha(barStartColor.get(), a);
                int c2 = PanelRender.lerpColor(ratio, c1, PanelRender.withAlpha(barEndColor.get(), a));
                PanelRender.hGradient(ctx, bx1 + 1, by1, fx2 - 1, by2, c1, c2);
                ctx.fill(bx1, by1 + 1, bx1 + 1, by2 - 1, c1);
                if (fx2 - 1 > bx1) ctx.fill(fx2 - 1, by1 + 1, fx2, by2 - 1, c2);
                // glossy highlight on the top half
                ctx.fillGradient(bx1 + 1, by1, fx2 - 1, by1 + 3, PanelRender.withAlpha(0x50FFFFFF, a), PanelRender.withAlpha(0x00FFFFFF, a));
            }

            if (showAbsorption.get() && dispAbsorption > 0.05) {
                double absRatio = PanelRender.clamp01(dispAbsorption / maxHealth);
                int ax2 = bx1 + (int) Math.round(bw * absRatio);
                ctx.fill(bx1, by2 - 2, ax2, by2, PanelRender.withAlpha(absorptionColor.get(), a));
            }
        }

        // bottom row: health number + items
        if (bottomRow) {
            if (showHealthText.get()) {
                int hpColor = PanelRender.lerpColor(hurt, text2, text);
                PanelRender.text(ctx, hpText, contentX, bottomY + 2, hpColor, true);
            }

            // Items cannot be faded, so only draw them while mostly visible.
            if (itemCount > 0 && fade > 0.6) {
                int ix = contentX + contentW - itemsW;
                for (int i = 0; i < itemCount; i++) {
                    ItemStack stack = items[i];
                    if (stack == null || stack.isEmpty()) continue;
                    float px = ix + i * 13;
                    m.pushMatrix();
                    m.translate(px, bottomY);
                    m.scale(0.75f, 0.75f);
                    ctx.drawItem(stack, 0, 0);
                    if (showDurability.get()) ctx.drawStackOverlay(tr, stack, 0, 0);
                    m.popMatrix();
                }
            }
        }

        m.popMatrix();
    }

    private String healthText() {
        String hp = formatHp(health);
        if (showAbsorption.get() && absorption > 0) hp += " +" + formatHp(absorption);
        return hp + " ❤";
    }

    private static String formatHp(float v) {
        if (Math.abs(v - Math.round(v)) < 0.05f) return Integer.toString(Math.round(v));
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
