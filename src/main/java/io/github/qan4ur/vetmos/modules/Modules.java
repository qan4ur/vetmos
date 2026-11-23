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

        // HUD
        ModuleManager.register(new ArmorHud());
        ModuleManager.register(new PotionHud());
        ModuleManager.register(new CooldownsHud());
        ModuleManager.register(new CrosshairHpIndicator());
        ModuleManager.register(new TargetHud());
        ModuleManager.register(new ComboCounter());
        ModuleManager.register(new Keystrokes());
        ModuleManager.register(new LowHpWarning());

        // Рендер
        ModuleManager.register(new CustomCrosshair());
        ModuleManager.register(new SwingAnimations());
        ModuleManager.register(new GlintColor());

        // Клиент
        ModuleManager.register(new io.github.qan4ur.vetmos.modules.client.InterfaceModule());

        // Мир
        ModuleManager.register(new BlockOutline());
        ModuleManager.register(new CustomSkyFog());
        ModuleManager.register(new WorldBorder());
    }
}
