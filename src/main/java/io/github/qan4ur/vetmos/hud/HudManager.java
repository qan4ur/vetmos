package io.github.qan4ur.vetmos.hud;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

import java.util.*;

/** Positions and draws all {@link HudModule}s. */
public final class HudManager {
    private static final Map<String, HudPosition> POSITIONS = new HashMap<>();
    private static boolean editorOpen;

    private HudManager() {}

    public static boolean isEditorOpen() {
        return editorOpen;
    }

    public static void setEditorOpen(boolean open) {
        editorOpen = open;
    }

    public static List<Module> hudModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (m instanceof HudModule) list.add(m);
        return list;
    }

    public static HudPosition getPosition(Module module) {
        return POSITIONS.computeIfAbsent(module.name, n -> ((HudModule) module).defaultHudPosition().copy());
    }

    public static void setPosition(Module module, HudPosition position) {
        POSITIONS.put(module.name, position);
        ConfigManager.markDirty();
    }

    public static void resetPosition(Module module) {
        POSITIONS.put(module.name, ((HudModule) module).defaultHudPosition().copy());
        ConfigManager.markDirty();
    }

    public static void resetAllPositions() {
        for (Module m : hudModules()) resetPosition(m);
    }

    public static Map<String, HudPosition> positions() {
        return POSITIONS;
    }

    /** Box of the element in GUI units: {left, top, width, height}. */
    public static double[] getBounds(Module module) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudModule hud = (HudModule) module;
        double scale = hud.hudScale().get();
        double w = Math.max(1, hud.getHudWidth() * scale), h = Math.max(1, hud.getHudHeight() * scale);
        HudPosition pos = getPosition(module);
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        return new double[] { pos.left(w, sw), pos.top(h, sh), w, h };
    }

    /** Called by the Fabric HUD layer every frame. */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden && !editorOpen) return;
        float tickDelta = tickCounter.getTickProgress(true);

        for (Module module : ModuleManager.all()) {
            if (!(module instanceof HudModule hud) || !module.isEnabled()) continue;
            if (!editorOpen && !hud.shouldRenderHud()) continue;
            renderOne(context, module, hud, tickDelta);
        }
    }

    private static void renderOne(DrawContext context, Module module, HudModule hud, float tickDelta) {
        double[] b;
        try {
            b = getBounds(module);
        } catch (Throwable t) {
            fail(module, t);
            return;
        }

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        try {
            matrices.translate((float) b[0], (float) b[1]);
            float s = (float) (double) hud.hudScale().get();
            matrices.scale(s, s);
            hud.renderHud(context, tickDelta, editorOpen);
        } catch (Throwable t) {
            fail(module, t);
        } finally {
            matrices.popMatrix();
        }
    }

    private static void fail(Module module, Throwable t) {
        Vetmos.LOG.error("HUD module {} failed to render, disabling it", module.name, t);
        module.setEnabled(false);
        io.github.qan4ur.vetmos.util.ChatUtil.error(module.title + " failed to render and was disabled: " + t);
    }
}
