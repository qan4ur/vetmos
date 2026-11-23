package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

/** Spawns client-only particles on the entity you hit. Nothing is sent to the server. */
public class HitParticles extends Module {
    private static final String GENERAL = "Основное";
    private static final String CRITS = "Криты";
    private static final String COLORS = "Цвета";

    // General

    private final EnumSetting<Kind> kind = enumeration(GENERAL, "particle", "Частица", "Тип частиц.", Kind.DUST);

    private final BoolSetting normalHits = bool(GENERAL, "normal-hits", "Обычные удары", "Частицы при обычных (не критических) ударах.", true);

    private final IntSetting amount = integer(GENERAL, "amount", "Количество", "Частиц за обычный удар.", 12, 1, 200)
        .visibleWhen(normalHits::get);

    private final DoubleSetting spread = decimal(GENERAL, "spread", "Разброс", "Насколько далеко от хитбокса цели появляются частицы.", 0.6, 0, 3);

    private final DoubleSetting speed = decimal(GENERAL, "speed", "Скорость", "Начальная скорость частиц.", 0.15, 0, 2);

    private final DoubleSetting size = decimal(GENERAL, "size", "Размер", "Размер частиц пыли.", 1.0, 0.1, 4)
        .visibleWhen(() -> kind.get() == Kind.DUST || (this.crits.get() && this.critKind.get() == Kind.DUST));

    // Crits

    private final BoolSetting crits = bool(CRITS, "crits", "Криты", "Частицы при критических ударах.", true);

    private final IntSetting critAmount = integer(CRITS, "crit-amount", "Количество при крите", "Частиц за критический удар.", 25, 1, 300)
        .visibleWhen(crits::get);

    private final EnumSetting<Kind> critKind = enumeration(CRITS, "crit-particle", "Частица крита", "Тип частиц при критическом ударе.", Kind.DUST)
        .visibleWhen(crits::get);

    // Colors

    private final ColorSetting color = color(COLORS, "color", "Цвет", "Цвет пыли при обычных ударах.", ColorSetting.rgb(200, 200, 200));

    private final ColorSetting critColor = color(COLORS, "crit-color", "Цвет крита", "Цвет пыли при критических ударах.", ColorSetting.rgb(255, 255, 255));

    private final Random random = Random.create();

    public HitParticles() {
        super("hit-particles", "Частицы удара", "Свои частицы на цели при ударе (только у тебя на экране).", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity entity) {
        if (mc.player == null || mc.world == null) return;
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;

        boolean crit = isCrit();
        if (crit && !crits.get()) return;
        if (!crit && !normalHits.get()) return;

        Kind k = crit ? critKind.get() : kind.get();
        int count = crit ? critAmount.get() : amount.get();
        ParticleEffect effect = k.effect((crit ? critColor : color).get() & 0xFFFFFF, size.getFloat());

        double sp = spread.get();
        double halfW = target.getWidth() / 2.0 + sp * 0.5;
        double v = speed.get();

        for (int i = 0; i < count; i++) {
            double x = target.getX() + (random.nextDouble() * 2 - 1) * halfW;
            double y = target.getBodyY(random.nextDouble()) + (random.nextDouble() * 2 - 1) * sp * 0.5;
            double z = target.getZ() + (random.nextDouble() * 2 - 1) * halfW;

            double vx = (random.nextDouble() * 2 - 1) * v;
            double vy = random.nextDouble() * v;
            double vz = (random.nextDouble() * 2 - 1) * v;

            mc.world.addParticleClient(effect, x, y, z, vx, vy, vz);
        }
    }

    /**
     * Vanilla crit conditions (PlayerEntity#attack), evaluated before the attack resets the cooldown:
     * charged > 0.9, falling, airborne, not climbing, not in water, not blind, not riding, not sprinting.
     */
    private boolean isCrit() {
        return mc.player.getAttackCooldownProgress(0.5f) > 0.9f
            && mc.player.fallDistance > 0
            && !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !mc.player.hasVehicle()
            && !mc.player.isSprinting();
    }

    public enum Kind {
        DUST("Пыль"),
        CRIT("Крит"),
        ENCHANTED_HIT("Зачарованный удар"),
        HEART("Сердце"),
        FLAME("Пламя"),
        SOUL_FIRE("Огонь душ");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        ParticleEffect effect(int rgb, float scale) {
            return switch (this) {
                case DUST -> new DustParticleEffect(rgb, scale);
                case CRIT -> ParticleTypes.CRIT;
                case ENCHANTED_HIT -> ParticleTypes.ENCHANTED_HIT;
                case HEART -> ParticleTypes.HEART;
                case FLAME -> ParticleTypes.FLAME;
                case SOUL_FIRE -> ParticleTypes.SOUL_FIRE_FLAME;
            };
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:Kind." + name(), title);
        }
    }
}
