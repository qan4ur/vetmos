package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.*;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a purely client-side effect when an entity you recently hit dies.
 * Death is detected from the health the server already syncs (isDead) and the vanilla death status (EntityStatus 3).
 */
public class KillEffect extends Module {
    private static final String GENERAL = "Основное";
    private static final String LIGHTNING = "Молния";
    private static final String PARTICLES = "Частицы";
    private static final String SOUND = "Звук";

    // General

    private final BoolSetting playersOnly = bool(GENERAL, "players-only", "Только игроки",
        "Срабатывать только на игроков (иначе на любых живых существ).", true);

    private final DoubleSetting attackWindow = decimal(GENERAL, "attack-window", "Окно атаки",
        "Сколько секунд после твоего удара смерть считается твоим убийством.", 5, 0.5, 30, 1);

    // Lightning

    private final BoolSetting lightning = bool(LIGHTNING, "lightning", "Молния",
        "Бьёт косметической молнией в жертву (видно только тебе).", true);

    // Particles

    private final BoolSetting particles = bool(PARTICLES, "particles", "Частицы", "Кольца частиц пыли вокруг жертвы.", true);

    private final ColorSetting particleColor = color(PARTICLES, "particle-color", "Цвет частиц", "Цвет пыли.", ColorSetting.rgb(240, 240, 240))
        .visibleWhen(particles::get);

    private final IntSetting particleCount = integer(PARTICLES, "particles-per-ring", "Частиц в кольце", "Сколько частиц в каждом кольце.", 32, 4, 200)
        .visibleWhen(particles::get);

    private final IntSetting rings = integer(PARTICLES, "rings", "Колец", "Количество колец по высоте жертвы.", 4, 1, 16)
        .visibleWhen(particles::get);

    private final DoubleSetting radius = decimal(PARTICLES, "radius", "Радиус", "Начальный радиус колец.", 0.6, 0, 4)
        .visibleWhen(particles::get);

    private final DoubleSetting burstSpeed = decimal(PARTICLES, "burst-speed", "Скорость разлёта", "Скорость разлёта частиц наружу.", 1.5, 0, 6)
        .visibleWhen(particles::get);

    private final DoubleSetting particleSize = decimal(PARTICLES, "particle-size", "Размер частиц", "Размер частиц пыли.", 1.5, 0.1, 4)
        .visibleWhen(particles::get);

    // Sound

    private final BoolSetting sound = bool(SOUND, "sound", "Звук", "Звук, который слышишь только ты.", true);

    private final EnumSetting<KillSound> soundType = enumeration(SOUND, "sound-type", "Звук убийства", "Какой звук проигрывать.", KillSound.LEVEL_UP)
        .visibleWhen(sound::get);

    private final DoubleSetting volume = decimal(SOUND, "volume", "Громкость", "Громкость звука.", 1, 0, 2)
        .visibleWhen(sound::get);

    private final DoubleSetting pitch = decimal(SOUND, "pitch", "Тон", "Высота звука.", 1, 0.5, 2)
        .visibleWhen(sound::get);

    /** entity id -> time (ms) of my last hit on it. Only touched on the client thread. */
    private final Int2LongMap attacked = new Int2LongOpenHashMap();
    private final List<LightningEntity> bolts = new ArrayList<>();
    private final List<Integer> boltAges = new ArrayList<>();
    /** Negative ids so local bolts never collide with server entity ids. */
    private int nextBoltId = -1_000_000;

    public KillEffect() {
        super("kill-effect", "Эффект убийства", "Молния, частицы и звук, когда умирает тот, кого ты ударил.", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        attacked.clear();
        clearBolts();
    }

    @Override
    public void onLeave() {
        attacked.clear();
        bolts.clear();
        boltAges.clear();
    }

    @Override
    public void onAttack(Entity e) {
        if (mc.player == null || e == mc.player || !isValidTarget(e)) return;
        attacked.put(e.getId(), System.currentTimeMillis());
    }

    /** Main thread (see ClientPlayNetworkHandlerMixin). */
    @Override
    public void onEntityStatus(Entity entity, byte status) {
        if (status == 3 && entity instanceof LivingEntity living) tryTrigger(living);
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        long window = (long) (attackWindow.get() * 1000);

        ObjectIterator<Int2LongMap.Entry> it = attacked.int2LongEntrySet().iterator();
        List<LivingEntity> dead = null;
        while (it.hasNext()) {
            Int2LongMap.Entry entry = it.next();
            if (now - entry.getLongValue() > window) {
                it.remove();
                continue;
            }
            if (mc.world.getEntityById(entry.getIntKey()) instanceof LivingEntity living && (living.isDead() || living.getHealth() <= 0)) {
                if (dead == null) dead = new ArrayList<>();
                dead.add(living);
            }
        }
        if (dead != null) dead.forEach(this::tryTrigger);

        tickBolts();
    }

    /** Fires once: the id is removed from the map, so a second signal for the same death does nothing. */
    private void tryTrigger(LivingEntity victim) {
        if (mc.world == null || !attacked.containsKey(victim.getId())) return;
        long hitTime = attacked.remove(victim.getId());
        if (System.currentTimeMillis() - hitTime > (long) (attackWindow.get() * 1000)) return;

        if (lightning.get()) spawnLightning(victim);
        if (particles.get()) spawnBurst(victim);
        if (sound.get()) {
            mc.world.playSoundClient(soundType.get().event, SoundCategory.PLAYERS, volume.getFloat(), pitch.getFloat());
        }
    }

    private boolean isValidTarget(Entity e) {
        if (playersOnly.get()) return e instanceof PlayerEntity;
        return e instanceof LivingEntity;
    }

    // Lightning

    private void spawnLightning(LivingEntity victim) {
        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        bolt.setId(nextBoltId--);
        bolt.setCosmetic(true);
        bolt.setPosition(victim.getX(), victim.getY(), victim.getZ());
        mc.world.addEntity(bolt);
        bolts.add(bolt);
        boltAges.add(0);
    }

    /** Vanilla client-side LightningEntity#tick discards itself after its flashes; this is a safety net. */
    private void tickBolts() {
        for (int i = bolts.size() - 1; i >= 0; i--) {
            LightningEntity bolt = bolts.get(i);
            int age = boltAges.get(i) + 1;
            if (bolt.isRemoved()) {
                bolts.remove(i);
                boltAges.remove(i);
            } else if (age > 60) {
                mc.world.removeEntity(bolt.getId(), Entity.RemovalReason.DISCARDED);
                bolts.remove(i);
                boltAges.remove(i);
            } else {
                boltAges.set(i, age);
            }
        }
    }

    private void clearBolts() {
        if (mc.world != null) {
            for (LightningEntity bolt : bolts) {
                if (!bolt.isRemoved()) mc.world.removeEntity(bolt.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        bolts.clear();
        boltAges.clear();
    }

    // Particles

    private void spawnBurst(LivingEntity victim) {
        DustParticleEffect dust = new DustParticleEffect(particleColor.get() & 0xFFFFFF, particleSize.getFloat());
        int perRing = particleCount.get();
        int ringCount = rings.get();
        double r = radius.get();
        double v = burstSpeed.get();
        double height = victim.getHeight();

        for (int ring = 0; ring < ringCount; ring++) {
            double y = victim.getY() + (ringCount == 1 ? height / 2 : height * ring / (ringCount - 1));
            double offset = ring * Math.PI / perRing; // stagger rings
            for (int i = 0; i < perRing; i++) {
                double angle = offset + i * (Math.PI * 2 / perRing);
                double cos = Math.cos(angle), sin = Math.sin(angle);
                mc.world.addParticleClient(dust,
                    victim.getX() + cos * r, y, victim.getZ() + sin * r,
                    cos * v, 0.05, sin * v);
            }
        }
    }

    public enum KillSound {
        THUNDER("Гром", SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER),
        LEVEL_UP("Новый уровень", SoundEvents.ENTITY_PLAYER_LEVELUP),
        ORB("Сфера опыта", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
        ANVIL("Наковальня", SoundEvents.BLOCK_ANVIL_LAND),
        BELL("Колокол", SoundEvents.BLOCK_BELL_USE),
        TOTEM("Тотем", SoundEvents.ITEM_TOTEM_USE),
        WITHER("Появление визера", SoundEvents.ENTITY_WITHER_SPAWN),
        DRAGON("Рык дракона", SoundEvents.ENTITY_ENDER_DRAGON_GROWL),
        BLAZE("Смерть ифрита", SoundEvents.ENTITY_BLAZE_DEATH);

        private final String title;
        final SoundEvent event;

        KillSound(String title, SoundEvent event) {
            this.title = title;
            this.event = event;
        }

        @Override
        public String toString() {
            return io.github.qan4ur.vetmos.util.Lang.tr("enum:KillSound." + name(), title);
        }
    }
}
