package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Plays a sound only you can hear when you hit an entity (another one for crits). Nothing is sent to the server. */
public class HitSounds extends Module {
    private static final String GENERAL = "Основное";
    private static final String CRITS = "Криты";

    // General

    private final EnumSetting<HitSound> sound = enumeration(GENERAL, "sound", "Звук", "Звук при обычном ударе.", HitSound.ORB);

    private final DoubleSetting volume = decimal(GENERAL, "volume", "Громкость", "Громкость звука.", 0.8, 0, 2, 2);

    private final DoubleSetting pitch = decimal(GENERAL, "pitch", "Тон", "Высота звука.", 1.0, 0.5, 2, 2);

    private final BoolSetting onlyPlayers = bool(GENERAL, "only-players", "Только игроки", "Звук только при ударе по игрокам.", false);

    // Crits

    private final BoolSetting crits = bool(CRITS, "crits", "Свой звук крита", "Отдельный звук при критическом ударе.", true);

    private final EnumSetting<HitSound> critSound = enumeration(CRITS, "crit-sound", "Звук крита", "Звук при критическом ударе.", HitSound.BELL)
        .visibleWhen(crits::get);

    private final DoubleSetting critPitch = decimal(CRITS, "crit-pitch", "Тон крита", "Высота звука крита.", 1.2, 0.5, 2, 2)
        .visibleWhen(crits::get);

    public HitSounds() {
        super("hit-sounds", "Звуки удара", "Свой звук при ударе по цели (слышишь только ты).", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity entity) {
        if (mc.player == null || mc.world == null) return;
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (onlyPlayers.get() && !(entity instanceof PlayerEntity)) return;

        boolean crit = crits.get() && isCrit();
        HitSound s = crit ? critSound.get() : sound.get();
        float p = (crit ? critPitch : pitch).getFloat();
        float v = volume.getFloat();
        if (v <= 0) return;

        mc.getSoundManager().play(PositionedSoundInstance.master(s.event(), p, v));
    }

    /** Same client-side crit check as {@link HitParticles} (vanilla PlayerEntity#attack conditions). */
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

    public enum HitSound {
        ORB("Опыт"),
        BELL("Колокольчик"),
        PLING("Плинк"),
        CHIME("Перезвон"),
        HAT("Щелчок"),
        BASS("Бас"),
        BIT("8-бит"),
        AMETHYST("Аметист"),
        ARROW("Попадание стрелы"),
        CRIT("Крит"),
        STRONG("Сильный удар"),
        GLASS("Стекло"),
        ANVIL("Наковальня");

        private final String title;

        HitSound(String title) {
            this.title = title;
        }

        SoundEvent event() {
            return switch (this) {
                case ORB -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                case BELL -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
                case PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
                case CHIME -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
                case HAT -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
                case BASS -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
                case BIT -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
                case AMETHYST -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
                case ARROW -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                case CRIT -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
                case STRONG -> SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
                case GLASS -> SoundEvents.BLOCK_GLASS_BREAK;
                case ANVIL -> SoundEvents.BLOCK_ANVIL_LAND;
            };
        }

        @Override
        public String toString() {
            return Lang.tr("enum:HitSound." + name(), title);
        }
    }
}
