package io.github.qan4ur.vetmos.modules.hud;

import io.github.qan4ur.vetmos.hud.HudManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Drawing / animation helpers shared by the Vetmos HUD modules (armor, effects, cooldowns, totem pops),
 * plus a small world-to-screen projection captured from the last rendered world frame.
 */
public final class HudRender2 {
    private HudRender2() {}

    // ---------------------------------------------------------------- panel

    /** Dark panel with 1px rounded corners, a faint accent glow and an accent top line. Colors are ARGB. */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int bg, int accent, float ease) {
        int bgc = withAlpha(bg, (int) ((bg >>> 24) * ease));
        int glow = withAlpha(accent, (int) (40 * ease));
        ctx.fill(x - 1, y + 1, x, y + h - 1, glow);
        ctx.fill(x + w, y + 1, x + w + 1, y + h - 1, glow);
        ctx.fill(x + 1, y + h, x + w - 1, y + h + 1, glow);

        ctx.fill(x + 1, y, x + w - 1, y + h, bgc);
        ctx.fill(x, y + 1, x + 1, y + h - 1, bgc);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, bgc);

        int ac = withAlpha(accent, (int) ((accent >>> 24) * ease));
        int half = w / 2;
        ctx.fillGradient(x + 1, y, x + half, y + 1, withAlpha(ac, (int) (60 * ease)), ac);
        ctx.fill(x + half, y, x + w - 1, y + 1, ac);
    }

    /** 1px outline. */
    public static void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** True when the HUD editor is open or there is no world (draw previews). */
    public static boolean isEditor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return HudManager.isEditorOpen() || mc.player == null || mc.world == null;
    }

    // ---------------------------------------------------------------- animation

    /** Frame-rate independent exponential approach. */
    public static float approach(float current, float target, double frameTime, float speed) {
        float k = 1 - (float) Math.exp(-speed * Math.max(frameTime, 0));
        return current + (target - current) * k;
    }

    public static float easeOutCubic(float t) {
        t = MathHelper.clamp(t, 0, 1);
        float f = 1 - t;
        return 1 - f * f * f;
    }

    public static float easeOutBack(float t) {
        t = MathHelper.clamp(t, 0, 1);
        float c1 = 1.70158f, c3 = c1 + 1;
        float f = t - 1;
        return 1 + c3 * f * f * f + c1 * f * f;
    }

    /** 0..1 sine pulse, {@code speed} pulses per second. */
    public static float pulse(double speed) {
        double time = System.nanoTime() / 1e9;
        return (float) (0.5 + 0.5 * Math.sin(time * Math.PI * 2 * speed));
    }

    // ---------------------------------------------------------------- colors

    public static int withAlpha(int argb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    /** Multiplies the color's own alpha by f. */
    public static int fade(int argb, float f) {
        return withAlpha(argb, (int) ((argb >>> 24) * MathHelper.clamp(f, 0, 1)));
    }

    public static int lerpColor(int a, int b, float t) {
        t = MathHelper.clamp(t, 0, 1);
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // ---------------------------------------------------------------- world -> screen

    private static boolean projectionHooked;
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static boolean projectionValid;
    private static final Vector4f TMP = new Vector4f();

    /**
     * Starts capturing the camera matrices at the end of every world frame. Call once (e.g. from {@code onInit}).
     * The HUD is drawn after the world in the same frame, so {@link #project} matches what is on screen.
     */
    public static void initProjection() {
        if (projectionHooked) return;
        projectionHooked = true;
        WorldRenderEvents.LAST.register(context -> {
            VIEW.set(context.positionMatrix());
            PROJECTION.set(context.projectionMatrix());
            PROJECTION.mul(VIEW, VIEW_PROJECTION);
            cameraPos = context.camera().getPos();
            projectionValid = true;
        });
    }

    public static Vec3d cameraPos() {
        return cameraPos;
    }

    /**
     * Projects a world position to GUI coordinates. Returns {x, y} in scaled GUI units, or null when the point is
     * behind the camera or no world frame has been rendered yet.
     */
    public static float[] project(double x, double y, double z) {
        if (!projectionValid) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        TMP.set((float) (x - cameraPos.x), (float) (y - cameraPos.y), (float) (z - cameraPos.z), 1f);
        VIEW_PROJECTION.transform(TMP);
        if (TMP.w <= 0.05f) return null;
        float ndcX = TMP.x / TMP.w, ndcY = TMP.y / TMP.w;
        if (ndcX < -1.5f || ndcX > 1.5f || ndcY < -1.5f || ndcY > 1.5f) return null;
        float sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        return new float[] { (ndcX * 0.5f + 0.5f) * sw, (0.5f - ndcY * 0.5f) * sh };
    }
}
