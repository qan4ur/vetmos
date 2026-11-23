package io.github.qan4ur.vetmos;

import io.github.qan4ur.vetmos.command.CommandManager;
import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.gui.ClickGuiScreen;
import io.github.qan4ur.vetmos.gui.HudEditorScreen;
import io.github.qan4ur.vetmos.hud.HudManager;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.Modules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vetmos implements ClientModInitializer {
    public static final String MOD_ID = "vetmos";
    public static final String NAME = "Vetmos";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static KeyBinding openGuiKey;
    private static Screen pendingScreen;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        Modules.registerAll();
        ConfigManager.load();
        for (Module m : ModuleManager.all()) ModuleManager.safe(m, m::onInit);

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vetmos.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.vetmos"));

        HudElementRegistry.addLast(id("hud"), HudManager::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(createClickGui());
            }
            if (client.player != null && client.world != null) ModuleManager.tick();
            ConfigManager.tick();
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && player == MinecraftClient.getInstance().player) ModuleManager.attack(entity);
            return ActionResult.PASS;
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !CommandManager.handle(message));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(ModuleManager::join));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ModuleManager::leave));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOG.info("{} loaded with {} modules", NAME, ModuleManager.all().size());
    }

    /** Opens a screen on the next tick (needed when called from the chat screen). */
    public static void openScreenNextTick(Screen screen) {
        pendingScreen = screen;
    }

    public static Screen createClickGui() {
        return new ClickGuiScreen();
    }

    public static Screen createHudEditor() {
        return new HudEditorScreen(null);
    }
}
