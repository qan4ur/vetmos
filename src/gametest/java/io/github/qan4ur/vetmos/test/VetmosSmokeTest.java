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
    private static float fov(net.minecraft.client.MinecraftClient client) {
        try {
            java.lang.reflect.Method m = net.minecraft.client.render.GameRenderer.class.getDeclaredMethod("getFov", net.minecraft.client.render.Camera.class, float.class, boolean.class);
            m.setAccessible(true);
            return (float) m.invoke(client.gameRenderer, client.gameRenderer.getCamera(), 1f, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> io.github.qan4ur.vetmos.config.ConfigManager.setWelcomeShown(true));
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
            sp.getServer().runCommand("scoreboard objectives add vt dummy \"Vetmos PvP\"");
            sp.getServer().runCommand("scoreboard objectives setdisplay sidebar vt");
            sp.getServer().runCommand("scoreboard players set Player0 vt 12");
            sp.getServer().runCommand("scoreboard players set Kills vt 5");
            sp.getServer().runCommand("bossbar add vt \"Ender Dragon\"");
            sp.getServer().runCommand("bossbar set vt players @a");
            sp.getServer().runCommand("bossbar set vt value 60");
            context.waitTicks(20);

            context.runOnClient(client -> {
                for (Module m : ModuleManager.all()) m.setEnabled(true);
            });
            context.waitTicks(40);
            sp.getServer().runCommand("title @a times 0 200 20");
            sp.getServer().runCommand("title @a title \"Vetmos\"");
            sp.getServer().runCommand("title @a subtitle \"1.2.0\"");
            context.waitTicks(10);
            context.takeScreenshot("vetmos-1-all-modules");

            // Zoom: hold C (the default zoom key) — must work even though vanilla also binds C (save toolbar)
            context.getInput().holdKey(net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_C));
            context.waitTicks(20);
            float zoomFov = context.computeOnClient(client -> fov(client));
            context.takeScreenshot("vetmos-1b-zoom");
            context.getInput().releaseKey(net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_C));
            context.waitTicks(20);
            float normalFov = context.computeOnClient(client -> fov(client));
            if (!(zoomFov < normalFov * 0.6f)) throw new AssertionError("Zoom did not zoom: fov " + zoomFov + " vs " + normalFov);

            // Clickable coordinates in chat
            String linked = context.computeOnClient(client -> {
                net.minecraft.text.Text t = io.github.qan4ur.vetmos.util.ChatCoords.link(net.minecraft.text.Text.literal("база на -515 64 -515 жду"));
                String[] found = { null };
                t.visit((style, str) -> {
                    if (style.getClickEvent() instanceof net.minecraft.text.ClickEvent.CopyToClipboard c) found[0] = c.value();
                    return java.util.Optional.empty();
                }, net.minecraft.text.Style.EMPTY);
                return found[0];
            });
            if (!"-515 64 -515".equals(linked)) throw new AssertionError("Coords not linked: " + linked);
            sp.getServer().runCommand("say Корды базы: -515 64 -515");
            context.waitTicks(5);
            context.setScreen(() -> new net.minecraft.client.gui.screen.ChatScreen(""));
            context.waitTicks(10);
            context.takeScreenshot("vetmos-1c-chat-coords");
            context.setScreen(() -> null);

            sortTest(context, sp);

            sp.getServer().runCommand("damage @p 13 minecraft:generic");
            context.waitTicks(30);
            context.takeScreenshot("vetmos-2-low-hp");

            context.setScreen(ClickGuiScreen::new);
            context.waitTicks(30);
            context.takeScreenshot("vetmos-3-clickgui");

            context.setScreen(() -> new ClickGuiScreen(null, ModuleManager.get(CustomCrosshair.class)));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-4-clickgui-crosshair");

            context.setScreen(() -> new io.github.qan4ur.vetmos.gui.MoveElementScreen(null, ModuleManager.get(io.github.qan4ur.vetmos.modules.hud.vanilla.ScoreboardHud.class)));
            context.waitTicks(20);
            context.takeScreenshot("vetmos-4b-move-element");

            context.setScreen(() -> new HudEditorScreen(null));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-5-hud-editor");

            context.runOnClient(client -> {
                io.github.qan4ur.vetmos.macro.MacroManager.add(io.github.qan4ur.vetmos.util.Keys.parse("g"), "/spawn", 3.0);
                io.github.qan4ur.vetmos.macro.MacroManager.add(io.github.qan4ur.vetmos.util.Keys.parse("h"), "gg", 10.0);
            });
            context.setScreen(() -> new ClickGuiScreen(null, ModuleManager.get(io.github.qan4ur.vetmos.modules.client.MacrosModule.class)));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-5b-macros");

            // English UI with a purple gradient accent
            context.runOnClient(client -> {
                io.github.qan4ur.vetmos.modules.client.InterfaceModule ui = ModuleManager.get(io.github.qan4ur.vetmos.modules.client.InterfaceModule.class);
                ui.language.set(io.github.qan4ur.vetmos.modules.client.InterfaceModule.Language.EN);
                ui.accent.set(0xFF8B5CF6);
                ui.gradient.set(true);
                ui.accent2.set(0xFF38BDF8);
            });
            context.setScreen(() -> new ClickGuiScreen(null, ModuleManager.get(io.github.qan4ur.vetmos.modules.hud.TargetHud.class)));
            context.waitTicks(30);
            context.takeScreenshot("vetmos-6-english-purple-settings");

            context.runOnClient(client -> {
                io.github.qan4ur.vetmos.modules.client.InterfaceModule ui = ModuleManager.get(io.github.qan4ur.vetmos.modules.client.InterfaceModule.class);
                ui.language.set(io.github.qan4ur.vetmos.modules.client.InterfaceModule.Language.RU);
                ui.resetSettings();
                io.github.qan4ur.vetmos.config.ConfigManager.setWelcomeShown(false);
            });
            context.setScreen(ClickGuiScreen::new);
            context.waitTicks(30);
            context.takeScreenshot("vetmos-7-welcome");

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

    private static int[] screenOrigin(net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen) {
        try {
            java.lang.reflect.Field fx = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("x");
            java.lang.reflect.Field fy = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("y");
            fx.setAccessible(true);
            fy.setAccessible(true);
            return new int[] { fx.getInt(screen), fy.getInt(screen) };
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Inventory sorting: chest and player inventory are merged + ordered, nothing is lost (checked on the server). */
    private static void sortTest(ClientGameTestContext context, TestSingleplayerContext sp) {
        sp.getServer().runCommand("clear @a");
        sp.getServer().runCommand("execute at @p run setblock ~2 ~ ~ minecraft:chest{Items:["
            + "{Slot:0b,id:\"minecraft:diamond\",count:1},"
            + "{Slot:3b,id:\"minecraft:dirt\",count:5},"
            + "{Slot:5b,id:\"minecraft:stone\",count:30},"
            + "{Slot:9b,id:\"minecraft:dirt\",count:20},"
            + "{Slot:14b,id:\"minecraft:stone\",count:40},"
            + "{Slot:20b,id:\"minecraft:ender_pearl\",count:3},"
            + "{Slot:26b,id:\"minecraft:ender_pearl\",count:10}]}");
        sp.getServer().runCommand("give @p minecraft:cobblestone 10");
        sp.getServer().runCommand("item replace entity @p container.20 with minecraft:cobblestone 7");
        sp.getServer().runCommand("item replace entity @p container.30 with minecraft:oak_planks 12");
        context.waitTicks(5);

        sp.getServer().runOnServer(server -> {
            net.minecraft.server.network.ServerPlayerEntity p = server.getPlayerManager().getPlayerList().get(0);
            net.minecraft.util.math.BlockPos pos = p.getBlockPos().add(2, 0, 0);
            if (p.getWorld().getBlockEntity(pos) instanceof net.minecraft.block.entity.ChestBlockEntity chest) p.openHandledScreen(chest);
        });
        context.waitForScreen(net.minecraft.client.gui.screen.ingame.GenericContainerScreen.class);
        context.waitTicks(5);
        context.takeScreenshot("vetmos-8a-chest-before");

        // sort the chest (hovered = a chest slot), then the player inventory (hovered = a player slot)
        context.runOnClient(client -> {
            var screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
            var sort = ModuleManager.get(io.github.qan4ur.vetmos.modules.inventory.InventorySort.class);
            net.minecraft.screen.slot.Slot chestSlot = screen.getScreenHandler().slots.get(0);
            if (!sort.onScreenInput(screen, org.lwjgl.glfw.GLFW.GLFW_KEY_R, chestSlot)) throw new AssertionError("sort did not run");
        });
        context.waitTicks(5);
        context.runOnClient(client -> {
            var screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
            var sort = ModuleManager.get(io.github.qan4ur.vetmos.modules.inventory.InventorySort.class);
            net.minecraft.screen.slot.Slot playerSlot = null;
            for (var sl : screen.getScreenHandler().slots) if (sl.inventory == client.player.getInventory() && sl.getIndex() == 20) playerSlot = sl;
            sort.onScreenInput(screen, org.lwjgl.glfw.GLFW.GLFW_KEY_R, playerSlot);
        });
        context.waitTicks(10);
        context.takeScreenshot("vetmos-8b-chest-sorted");

        String result = sp.getServer().computeOnServer(server -> {
            net.minecraft.server.network.ServerPlayerEntity p = server.getPlayerManager().getPlayerList().get(0);
            var chest = (net.minecraft.block.entity.ChestBlockEntity) p.getWorld().getBlockEntity(p.getBlockPos().add(2, 0, 0));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chest.size(); i++) {
                var st = chest.getStack(i);
                if (!st.isEmpty()) sb.append(i).append('=').append(net.minecraft.registry.Registries.ITEM.getId(st.getItem()).getPath()).append('x').append(st.getCount()).append(' ');
            }
            sb.append("| inv:");
            for (int i = 9; i < 36; i++) {
                var st = p.getInventory().getStack(i);
                if (!st.isEmpty()) sb.append(' ').append(i).append('=').append(net.minecraft.registry.Registries.ITEM.getId(st.getItem()).getPath()).append('x').append(st.getCount());
            }
            sb.append(" | hotbar0=").append(p.getInventory().getStack(0).getCount());
            return sb.toString();
        });
        io.github.qan4ur.vetmos.Vetmos.LOG.info("SORT RESULT: {}", result);
        String chestPart = result.substring(0, result.indexOf('|')).trim();
        String[] cells = chestPart.split(" ");
        // merged: stone 70 -> 64+6, dirt 25, pearls 13, diamond 1  => 5 stacks in slots 0..4, totals preserved
        if (cells.length != 5) throw new AssertionError("chest not merged/sorted: " + result);
        for (int i = 0; i < cells.length; i++) if (!cells[i].startsWith(i + "=")) throw new AssertionError("gap in sorted chest: " + result);
        if (!chestPart.contains("stonex64") || !chestPart.contains("stonex6") || !chestPart.contains("dirtx25")
            || !chestPart.contains("ender_pearlx13") || !chestPart.contains("diamondx1")) throw new AssertionError("chest totals wrong: " + result);
        String invPart = result.substring(result.indexOf("inv:"));
        if (!invPart.contains("9=") || !invPart.contains("cobblestonex7") || !invPart.contains("oak_planksx12"))
            throw new AssertionError("player inventory not sorted: " + result);
        if (!result.endsWith("hotbar0=10")) throw new AssertionError("hotbar touched: " + result);

        // shift-drag: hold Shift + LMB and sweep over chest slots 0..4 -> all moved to the player
        context.getInput().holdShift();
        context.runOnClient(client -> {
            var screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
            int[] xy = screenOrigin(screen);
            var slots = screen.getScreenHandler().slots;
            double sx = xy[0] + slots.get(0).x + 8, sy = xy[1] + slots.get(0).y + 8;
            screen.mouseClicked(sx, sy, 0);
            for (int i = 1; i <= 4; i++) {
                double tx = xy[0] + slots.get(i).x + 8, ty = xy[1] + slots.get(i).y + 8;
                screen.mouseDragged(tx, ty, 0, tx - sx, ty - sy);
                sx = tx; sy = ty;
            }
            screen.mouseReleased(sx, sy, 0);
        });
        context.getInput().releaseShift();
        context.waitTicks(10);
        String after = sp.getServer().computeOnServer(server -> {
            net.minecraft.server.network.ServerPlayerEntity p = server.getPlayerManager().getPlayerList().get(0);
            var chest = (net.minecraft.block.entity.ChestBlockEntity) p.getWorld().getBlockEntity(p.getBlockPos().add(2, 0, 0));
            int left = 0;
            for (int i = 0; i < chest.size(); i++) left += chest.getStack(i).getCount();
            return String.valueOf(left);
        });
        io.github.qan4ur.vetmos.Vetmos.LOG.info("SHIFT DRAG chest items left: {}", after);
        if (!"0".equals(after)) throw new AssertionError("shift-drag did not move everything, left in chest: " + after);
        context.setScreen(() -> null);
        context.waitTicks(5);
    }
}
