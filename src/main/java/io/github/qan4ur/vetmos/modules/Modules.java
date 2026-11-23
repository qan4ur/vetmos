package io.github.qan4ur.vetmos.modules;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.combat.*;
import io.github.qan4ur.vetmos.modules.hud.*;
import io.github.qan4ur.vetmos.modules.render.*;
import io.github.qan4ur.vetmos.modules.world.*;

/** Registers every module. Order does not matter (the GUI sorts by title). */
public final class Modules {
    private Modules() {}

    public static void registerAll() {
        // Бой
        ModuleManager.register(new HitColor());
        ModuleManager.register(new HitParticles());
        ModuleManager.register(new KillEffect());
        ModuleManager.register(new TotemPopCounter());
        ModuleManager.register(new HitSounds());
        ModuleManager.register(new DamageNumbers());

        // HUD
        ModuleManager.register(new ArmorHud());
        ModuleManager.register(new PotionHud());
        ModuleManager.register(new CooldownsHud());
        ModuleManager.register(new CrosshairHpIndicator());
        ModuleManager.register(new TargetHud());
        ModuleManager.register(new ComboCounter());
        ModuleManager.register(new Keystrokes());
        ModuleManager.register(new LowHpWarning());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.vanilla.ScoreboardHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.vanilla.BossBarHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.vanilla.TitlesHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.vanilla.ActionBarHud());
        ModuleManager.register(new ItemCounter());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.FpsHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.PingHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.CoordsHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.DirectionHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.SpeedHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.ClockHud());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.hud.info.BiomeHud());

        // Рендер
        ModuleManager.register(new CustomCrosshair());
        ModuleManager.register(new SwingAnimations());
        ModuleManager.register(new GlintColor());
        ModuleManager.register(new NoHurtCam());
        ModuleManager.register(new LowFire());
        ModuleManager.register(new LowShield());
        ModuleManager.register(new Zoom());
        ModuleManager.register(new Hitboxes());

        // Клиент
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.client.InterfaceModule());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.client.ChatTweaks());
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.client.MacrosModule());

        // Мир
        ModuleManager.register(new BlockOutline());
        ModuleManager.register(new CustomSkyFog());
        ModuleManager.register(new WorldBorder());
    }
}
