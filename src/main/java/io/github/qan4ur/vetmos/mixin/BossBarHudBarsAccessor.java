package io.github.qan4ur.vetmos.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/** Read access to the active boss bars (used by the movable boss bar HUD). */
@Mixin(BossBarHud.class)
public interface BossBarHudBarsAccessor {
    // final Map<UUID, ClientBossBar> bossBars (package-private; verified with javap, 1.21.7 yarn build.1)
    @Accessor("bossBars")
    Map<UUID, ClientBossBar> vetmos$getBossBars();
}
