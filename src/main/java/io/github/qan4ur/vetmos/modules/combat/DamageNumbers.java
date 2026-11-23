package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.modules.hud.HudRender2;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.util.*;

/**
 * Floating damage (and optional healing) numbers above entities. Health of nearby living entities is sampled every
 * tick; a drop spawns a number that pops in with a spring, floats up with a little sideways drift and fades out.
 * Drawn in a HUD layer (hidden on F1) by projecting the world position to the screen.
 */
public class DamageNumbers extends Module {
    private static final String G = "Основное", C = "Цвета", A = "Анимация", S = "Стиль";

    // General
    private final BoolSetting onlyMine = bool(G, "only-my-targets", "Только мои цели",
        "Показывать урон только у сущностей, которых вы недавно ударили.", true);
    private final IntSetting memory = integer(G, "target-memory", "Память цели",
        "Сколько секунд после удара сущность считается вашей целью.", 5, 1, 30).visibleWhen(onlyMine::get);
    private final BoolSetting playersOnly = bool(G, "players-only", "Только игроки", "Показывать урон только у игроков.", false);
    private final BoolSetting healing = bool(G, "healing", "Лечение", "Показывать восстановление здоровья другим цветом.", false);
    private final IntSetting maxDistance = integer(G, "max-distance", "Дальность", "Максимальная дистанция до сущности (блоки).", 32, 4, 128);
    private final DoubleSetting bigThreshold = decimal(G, "big-hit", "Сильный удар",
        "Урон, начиная с которого число больше и выделено цветом.", 6.0, 1, 40, 1);
    private final IntSetting decimals = integer(G, "decimals", "Знаков после запятой", "0 — целые числа, 1 — с десятыми.", 1, 0, 1);

    // Colours
    private final ColorSetting normalColor = color(C, "normal-color", "Обычный урон", "Цвет обычных чисел урона.", Theme.WHITE);
    private final BoolSetting bigAccent = bool(C, "big-use-accent", "Акцент интерфейса",
        "Красить сильные удары в цвет акцента интерфейса.", true);
    private final ColorSetting bigColor = color(C, "big-color", "Сильный удар", "Цвет чисел сильных ударов (когда акцент выключен).", Theme.WARNING)
        .visibleWhen(() -> !bigAccent.get());
    private final ColorSetting healColor = color(C, "heal-color", "Лечение", "Цвет чисел лечения.", ColorSetting.rgb(34, 197, 94))
        .visibleWhen(healing::get);

    // Animation
    private final DoubleSetting lifetime = decimal(A, "lifetime", "Время жизни", "Сколько секунд число остаётся на экране.", 1.2, 0.3, 5, 1);
    private final DoubleSetting riseSpeed = decimal(A, "rise-speed", "Скорость подъёма", "Скорость всплытия (блоков в секунду).", 0.8, 0, 4, 1);
    private final BoolSetting drift = bool(A, "drift", "Смещение в сторону", "Числа слегка уплывают в случайную сторону.", true);
    private final BoolSetting popIn = bool(A, "pop-in", "Пружинное появление", "Число появляется с пружинящим увеличением.", true);

    // Style
    private final DoubleSetting size = decimal(S, "size", "Размер", "Размер чисел.", 1.2, 0.3, 4);
    private final DoubleSetting bigScale = decimal(S, "big-scale", "Размер сильного удара", "Во сколько раз число сильного удара больше.", 1.4, 1, 3);
    private final EnumSetting<TextEffect> effect = enumeration(S, "text-effect", "Обводка", "Тень или обводка текста.", TextEffect.SHADOW);

    public enum TextEffect {
        NONE("Нет"), SHADOW("Тень"), OUTLINE("Обводка");

        private final String title;

        TextEffect(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return Lang.tr("enum:TextEffect." + name(), title);
        }
    }

    private static final int MAX_NUMBERS = 96;

    private static final class Popup {
        final double x, y, z;
        final String text;
        final int kind; // 0 normal, 1 big, 2 heal
        final float driftX;
        final long bornNs;

        Popup(double x, double y, double z, String text, int kind, float driftX, long bornNs) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.kind = kind;
            this.driftX = driftX;
            this.bornNs = bornNs;
        }
    }

    private final Map<Integer, Float> lastHealth = new HashMap<>();
    private final Map<Integer, Long> targets = new HashMap<>();
    private final ArrayDeque<Popup> popups = new ArrayDeque<>();
    private final Set<Integer> seen = new HashSet<>();
    private final Random random = new Random();
    private ClientWorld lastWorld;

    public DamageNumbers() {
        super("damage-numbers", "Цифры урона", "Всплывающие числа урона над сущностями.", Category.COMBAT);
    }

    @Override
    public void onInit() {
        HudRender2.initProjection();
        HudElementRegistry.addLast(Vetmos.id("damage-numbers"), this::render);
    }

    private void reset() {
        lastHealth.clear();
        targets.clear();
        popups.clear();
    }

    @Override
    protected void onEnable() {
        reset();
        lastWorld = mc.world;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @Override
    public void onJoin() {
        reset();
    }

    @Override
    public void onLeave() {
        reset();
        lastWorld = null;
    }

    @Override
    public void onAttack(Entity target) {
        if (target instanceof LivingEntity) targets.put(target.getId(), System.nanoTime());
    }

    // ---------------------------------------------------------------- tracking

    @Override
    public void onTick() {
        if (mc.world != lastWorld) {
            reset();
            lastWorld = mc.world;
        }
        if (mc.world == null || mc.player == null) return;

        long now = System.nanoTime();
        long memNs = memory.get() * 1_000_000_000L;
        targets.values().removeIf(t -> now - t > memNs);

        double maxSq = (double) maxDistance.get() * maxDistance.get();
        seen.clear();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || e == mc.player) continue;
            if (e.squaredDistanceTo(mc.player) > maxSq) continue;
            int id = e.getId();
            seen.add(id);

            float hp = Math.max(0, living.getHealth()) + Math.max(0, living.getAbsorptionAmount());
            Float prev = lastHealth.put(id, hp);
            if (prev == null) continue; // first sample

            float delta = hp - prev;
            if (Math.abs(delta) < 0.05f) continue;
            if (playersOnly.get() && !(e instanceof PlayerEntity)) continue;
            if (onlyMine.get() && !targets.containsKey(id)) continue;
            if (e.isInvisibleTo(mc.player)) continue;

            if (delta < 0) spawn(e, -delta, false, now);
            else if (healing.get()) spawn(e, delta, true, now);
        }
        lastHealth.keySet().retainAll(seen);
    }

    private void spawn(Entity e, float amount, boolean heal, long now) {
        int kind = heal ? 2 : (amount >= bigThreshold.get() ? 1 : 0);
        String text = (heal ? "+" : "-") + format(amount);
        double ox = (random.nextDouble() - 0.5) * e.getWidth() * 0.8;
        double oz = (random.nextDouble() - 0.5) * e.getWidth() * 0.8;
        double y = e.getY() + e.getHeight() * 0.85 + random.nextDouble() * 0.2;
        float dx = drift.get() ? (random.nextFloat() * 2 - 1) * 14f : 0f;
        popups.addLast(new Popup(e.getX() + ox, y, e.getZ() + oz, text, kind, dx, now));
        while (popups.size() > MAX_NUMBERS) popups.removeFirst();
    }

    private String format(float v) {
        if (decimals.get() <= 0) return String.valueOf(Math.max(1, Math.round(v)));
        float r = Math.round(v * 10f) / 10f;
        if (Math.abs(r - Math.round(r)) < 1e-4f) return String.valueOf(Math.round(r));
        return String.format(Locale.ROOT, "%.1f", r);
    }

    // ---------------------------------------------------------------- render

    private void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!isEnabled() || mc.world == null || mc.player == null || mc.options.hudHidden || popups.isEmpty()) return;

        long now = System.nanoTime();
        double life = lifetime.get();
        // drop finished numbers
        popups.removeIf(p -> (now - p.bornNs) / 1e9 >= life);
        if (popups.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        Vec3d cam = HudRender2.cameraPos();
        Matrix3x2fStack m = ctx.getMatrices();

        int normal = normalColor.get();
        int big = bigAccent.get() ? Theme.accent() : bigColor.get();
        int heal = healColor.get();

        for (Popup p : popups) { // oldest first, newest drawn on top
            float age = (float) ((now - p.bornNs) / 1e9);
            float t = (float) (age / life);

            double wy = p.y + riseSpeed.get() * age;
            float[] screen = HudRender2.project(p.x, wy, p.z);
            if (screen == null) continue;

            double dist = Math.sqrt(cam.squaredDistanceTo(p.x, wy, p.z));
            float distScale = (float) MathHelper.clamp(1.0 - dist / 64.0, 0.4, 1.0);

            // pop-in spring: 0 -> overshoot -> 1
            float spring = popIn.get() ? 1f - (float) (Math.exp(-9 * age) * Math.cos(15 * age)) : 1f;
            spring = Math.max(0.01f, spring);
            float s = (float) (size.get() * distScale) * spring * (p.kind == 1 ? bigScale.getFloat() : 1f);

            // fade over the last 40% of the life
            float alpha = t < 0.6f ? 1f : 1f - HudRender2.easeOutCubic((t - 0.6f) / 0.4f);
            if (alpha <= 0.02f) continue;

            int base = p.kind == 2 ? heal : p.kind == 1 ? big : normal;
            int a = (int) ((base >>> 24) * alpha);
            if (a < 5) continue;
            int col = (a << 24) | (base & 0xFFFFFF);

            float sx = screen[0] + p.driftX * HudRender2.easeOutCubic(t);
            float sy = screen[1];

            m.pushMatrix();
            m.translate(sx, sy);
            m.scale(s, s);
            int w = tr.getWidth(p.text);
            int x = -w / 2, y = -tr.fontHeight / 2;
            switch (effect.get()) {
                case NONE -> ctx.drawText(tr, p.text, x, y, col, false);
                case SHADOW -> ctx.drawText(tr, p.text, x, y, col, true);
                case OUTLINE -> {
                    int oc = (Math.min(255, (int) (a * 0.9f)) << 24);
                    ctx.drawText(tr, p.text, x - 1, y, oc, false);
                    ctx.drawText(tr, p.text, x + 1, y, oc, false);
                    ctx.drawText(tr, p.text, x, y - 1, oc, false);
                    ctx.drawText(tr, p.text, x, y + 1, oc, false);
                    ctx.drawText(tr, p.text, x, y, col, false);
                }
            }
            m.popMatrix();
        }
    }
}
