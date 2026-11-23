package io.github.qan4ur.vetmos.test;

import io.github.qan4ur.vetmos.gui.ClickGuiScreen;
import io.github.qan4ur.vetmos.gui.HudEditorScreen;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.render.CustomCrosshair;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Smoke test: every module enabled at once must not crash, and the screenshots show what is drawn. */
public class VetmosSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            sp.getClientWorld().waitForChunksRender();
            sp.getServer().runCommand("time set noon");
            sp.getServer().runCommand("gamemode survival @a");
            sp.getServer().runCommand("item replace entity @a armor.chest with minecraft:diamond_chestplate[enchantments={protection:4}]");
            sp.getServer().runCommand("item replace entity @a armor.head with minecraft:netherite_helmet");
            sp.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword[enchantments={sharpness:5}]");
            sp.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:totem_of_undying");
            sp.getServer().runCommand("effect give @a minecraft:strength 600 1");
            sp.getServer().runCommand("effect give @a minecraft:speed 600 1");
            sp.getServer().runCommand("effect give @a minecraft:absorption 600 1");
            sp.getServer().runCommand("summon minecraft:zombie ~ ~ ~3 {NoAI:1b}");
            context.waitTicks(20);

            context.runOnClient(client -> {
                for (Module m : ModuleManager.all()) m.setEnabled(true);
            });
            context.waitTicks(40);
            context.takeScreenshot("vetmos-1-all-modules");

            sp.getServer().runCommand("damage @p 13 minecraft:generic");
            context.waitTicks(30);
            context.takeScreenshot("vetmos-2-low-hp");

            context.setScreen(ClickGuiScreen::new);
            context.waitTicks(30);
            context.takeScreenshot("vetmos-3-clickgui");

            context.setScreen(() -> new ClickGuiScreen(null, ModuleManager.get(CustomCrosshair.class)));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-4-clickgui-crosshair");

            context.setScreen(() -> new HudEditorScreen(null));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-5-hud-editor");

            context.setScreen(() -> null);
            context.waitTicks(10);

            int disabled = context.computeOnClient(client -> {
                int n = 0;
                for (Module m : ModuleManager.all()) if (!m.isEnabled()) n++;
                return n;
            });
            if (disabled > 0) throw new AssertionError(disabled + " module(s) crashed and were disabled, see log");
        }
    }
}
