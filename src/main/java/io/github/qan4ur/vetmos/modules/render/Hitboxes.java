package io.github.qan4ur.vetmos.modules.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.ColorSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom F3+B entity hitboxes. While the module is on, the vanilla hitbox drawing is cancelled by
 * {@code HitboxesEntityRenderDispatcherMixin} and the boxes are drawn here in {@link WorldRenderEvents#AFTER_ENTITIES}
 * with their own colours, thickness, fill, look-direction and eye-height lines.
 */
public class Hitboxes extends Module {
    private static final String G = "Основное", F = "Фильтры", C = "Цвета", L = "Линии";

    private static final RenderPipeline QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("vetmos", "pipeline/hitboxes_quads"))
        .withCull(false)
        .withDepthWrite(false)
        .build());
    private static final RenderLayer QUADS = RenderLayer.of("vetmos_hitboxes_quads", 1536, false, true, QUADS_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder().build(false));

    // General
    private final BoolSetting alwaysShow = bool(G, "always-show", "Всегда показывать",
        "Показывать хитбоксы без F3+B.", false);
    private final DoubleSetting lineWidth = decimal(G, "line-width", "Толщина линии",
        "Толщина линий примерно в пикселях. 1 — обычные тонкие линии.", 1.5, 1, 10, 1);
    private final BoolSetting fill = bool(G, "fill", "Заливка", "Заливать хитбокс полупрозрачным цветом.", false);
    private final IntSetting fillAlpha = integer(G, "fill-alpha", "Прозрачность заливки",
        "Непрозрачность заливки (0–255).", 40, 0, 255).visibleWhen(fill::get);

    // Filters
    private final BoolSetting onlyPlayers = bool(F, "only-players", "Только игроки", "Показывать хитбоксы только у игроков.", false);
    private final BoolSetting hideItems = bool(F, "hide-items", "Скрыть предметы", "Не показывать хитбоксы выброшенных предметов.", false)
        .visibleWhen(() -> !onlyPlayers.get());
    private final BoolSetting hideProjectiles = bool(F, "hide-projectiles", "Скрыть снаряды",
        "Не показывать хитбоксы стрел, зелий, жемчуга и других снарядов.", false).visibleWhen(() -> !onlyPlayers.get());

    // Colours
    private final BoolSetting useAccent = bool(C, "use-accent", "Акцент интерфейса", "Красить хитбоксы в цвет акцента интерфейса.", true);
    private final ColorSetting color = color(C, "color", "Цвет", "Цвет хитбоксов (когда акцент выключен).", Theme.WHITE)
        .visibleWhen(() -> !useAccent.get());
    private final BoolSetting separate = bool(C, "separate-colors", "Раздельные цвета",
        "Свой цвет для игроков, мобов и предметов.", false);
    private final ColorSetting playerColor = color(C, "player-color", "Игроки", "Цвет хитбоксов игроков.", Theme.WHITE)
        .visibleWhen(separate::get);
    private final ColorSetting mobColor = color(C, "mob-color", "Мобы", "Цвет хитбоксов мобов и других существ.", Theme.GREY)
        .visibleWhen(separate::get);
    private final ColorSetting itemColor = color(C, "item-color", "Предметы", "Цвет хитбоксов выброшенных предметов.", Theme.MID)
        .visibleWhen(separate::get);

    // Lines
    private final BoolSetting lookLine = bool(L, "look-direction", "Направление взгляда",
        "Линия, показывающая, куда смотрит сущность (синяя линия F3+B).", true);
    private final ColorSetting lookColor = color(L, "look-color", "Цвет взгляда", "Цвет линии направления взгляда.", ColorSetting.rgb(60, 110, 255))
        .visibleWhen(lookLine::get);
    private final BoolSetting eyeLine = bool(L, "eye-height", "Высота глаз", "Рамка на высоте глаз сущности.", false);
    private final ColorSetting eyeColor = color(L, "eye-color", "Цвет высоты глаз", "Цвет рамки на высоте глаз.", ColorSetting.rgb(255, 80, 80))
        .visibleWhen(eyeLine::get);

    private final List<Entity> visible = new ArrayList<>();

    public Hitboxes() {
        super("hitboxes", "Хитбоксы", "Свой цвет, толщина и заливка хитбоксов F3+B.", Category.RENDER);
    }

    /** Called by the mixin: true cancels the vanilla hitbox drawing. */
    public static boolean replacesVanilla() {
        try {
            return ModuleManager.isEnabled(Hitboxes.class);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void onInit() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled()) return;
            ModuleManager.safe(this, () -> render(context));
        });
    }

    // ---------------------------------------------------------------- render

    private void render(WorldRenderContext context) {
        if (mc.world == null || mc.player == null) return;
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        if (!dispatcher.shouldRenderHitboxes() && !alwaysShow.get()) return;
        if (mc.hasReducedDebugInfo()) return; // same rule as vanilla F3+B
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        float tickDelta = context.tickCounter().getTickProgress(true);
        Vec3d cam = context.camera().getPos();
        Frustum frustum = context.frustum();
        Entity cameraEntity = mc.getCameraEntity();
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();

        visible.clear();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EnderDragonPart) continue; // drawn with the dragon
            if (e == cameraEntity && firstPerson) continue;
            if (e.isInvisibleTo(mc.player)) continue;
            if (!passesFilters(e)) continue;
            if (frustum != null && !dispatcher.shouldRender(e, frustum, cam.x, cam.y, cam.z)) continue;
            visible.add(e);
        }
        if (visible.isEmpty()) return;

        MatrixStack matrices = context.matrixStack() != null ? context.matrixStack() : new MatrixStack();
        MatrixStack.Entry entry = matrices.peek();
        boolean thick = lineWidth.get() > 1.05;

        // Pass 1: translucent quads (fill + thick lines)
        if (fill.get() || thick) {
            VertexConsumer quads = consumers.getBuffer(QUADS);
            for (Entity e : visible) drawEntity(e, tickDelta, cam, entry, quads, null);
            flush(consumers, QUADS);
        }
        // Pass 2: thin GL lines
        if (!thick) {
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            for (Entity e : visible) drawEntity(e, tickDelta, cam, entry, null, lines);
            flush(consumers, RenderLayer.getLines());
        }
        visible.clear();
    }

    private boolean passesFilters(Entity e) {
        if (onlyPlayers.get()) return e instanceof PlayerEntity;
        if (hideItems.get() && e instanceof ItemEntity) return false;
        if (hideProjectiles.get() && e instanceof ProjectileEntity) return false;
        return true;
    }

    private int colorFor(Entity e) {
        if (separate.get()) {
            if (e instanceof PlayerEntity) return playerColor.get();
            if (e instanceof ItemEntity) return itemColor.get();
            if (e instanceof LivingEntity) return mobColor.get();
        }
        return useAccent.get() ? Theme.accent() : color.get();
    }

    /**
     * Draws one entity. With {@code quads} set: fill and (when thick) the edges / lines as thin boxes and ribbons.
     * With {@code lines} set: the edges / lines as 1px GL lines.
     */
    private void drawEntity(Entity e, float tickDelta, Vec3d cam, MatrixStack.Entry entry, VertexConsumer quads, VertexConsumer lines) {
        Vec3d lerp = e.getLerpedPos(tickDelta);
        Vec3d shift = lerp.subtract(e.getPos()).subtract(cam); // world -> camera-relative, interpolated
        Box box = e.getBoundingBox().offset(shift);
        int c = colorFor(e) | 0xFF000000;

        double h = halfWidth(box.getCenter());
        drawBox(box, c, h, entry, quads, lines);

        if (e instanceof EnderDragonEntity dragon) {
            for (EnderDragonPart part : dragon.getBodyParts()) {
                Vec3d ps = part.getLerpedPos(tickDelta).subtract(part.getPos()).subtract(cam);
                drawBox(part.getBoundingBox().offset(ps), c, h, entry, quads, lines);
            }
        }

        if (eyeLine.get() && e instanceof LivingEntity) {
            double y = lerp.y - cam.y + e.getStandingEyeHeight();
            int ec = eyeColor.get() | 0xFF000000;
            double x1 = box.minX, x2 = box.maxX, z1 = box.minZ, z2 = box.maxZ;
            segment(x1, y, z1, x2, y, z1, ec, h, entry, quads, lines);
            segment(x1, y, z2, x2, y, z2, ec, h, entry, quads, lines);
            segment(x1, y, z1, x1, y, z2, ec, h, entry, quads, lines);
            segment(x2, y, z1, x2, y, z2, ec, h, entry, quads, lines);
        }

        if (lookLine.get()) {
            Vec3d dir = e.getRotationVec(tickDelta).multiply(2.0);
            float x = (float) (lerp.x - cam.x), y = (float) (lerp.y - cam.y + e.getStandingEyeHeight()), z = (float) (lerp.z - cam.z);
            int lc = lookColor.get() | 0xFF000000;
            if (lines != null) {
                float len = (float) dir.length();
                float nx = len > 0 ? (float) dir.x / len : 0, ny = len > 0 ? (float) dir.y / len : 1, nz = len > 0 ? (float) dir.z / len : 0;
                lines.vertex(entry, x, y, z).color(lc).normal(entry, nx, ny, nz);
                lines.vertex(entry, x + (float) dir.x, y + (float) dir.y, z + (float) dir.z).color(lc).normal(entry, nx, ny, nz);
            } else if (quads != null && h > 0) {
                ribbon(quads, entry, x, y, z, (float) dir.x, (float) dir.y, (float) dir.z, (float) h, lc);
            }
        }
    }

    /** Half line thickness in blocks for a point at this camera-relative position, 0 for thin GL lines. */
    private double halfWidth(Vec3d rel) {
        double width = lineWidth.get();
        if (width <= 1.05) return 0;
        double dist = Math.max(1, rel.length());
        return width * 0.5 * 0.0013 * dist;
    }

    private void drawBox(Box b, int c, double h, MatrixStack.Entry entry, VertexConsumer quads, VertexConsumer lines) {
        if (quads != null && fill.get()) {
            int fc = (Math.max(0, Math.min(255, fillAlpha.get())) << 24) | (c & 0xFFFFFF);
            double ex = 0.002;
            box(quads, entry, b.minX - ex, b.minY - ex, b.minZ - ex, b.maxX + ex, b.maxY + ex, b.maxZ + ex, fc);
        }
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        // bottom
        segment(x1, y1, z1, x2, y1, z1, c, h, entry, quads, lines);
        segment(x1, y1, z2, x2, y1, z2, c, h, entry, quads, lines);
        segment(x1, y1, z1, x1, y1, z2, c, h, entry, quads, lines);
        segment(x2, y1, z1, x2, y1, z2, c, h, entry, quads, lines);
        // top
        segment(x1, y2, z1, x2, y2, z1, c, h, entry, quads, lines);
        segment(x1, y2, z2, x2, y2, z2, c, h, entry, quads, lines);
        segment(x1, y2, z1, x1, y2, z2, c, h, entry, quads, lines);
        segment(x2, y2, z1, x2, y2, z2, c, h, entry, quads, lines);
        // verticals
        segment(x1, y1, z1, x1, y2, z1, c, h, entry, quads, lines);
        segment(x2, y1, z1, x2, y2, z1, c, h, entry, quads, lines);
        segment(x1, y1, z2, x1, y2, z2, c, h, entry, quads, lines);
        segment(x2, y1, z2, x2, y2, z2, c, h, entry, quads, lines);
    }

    /** Axis-aligned edge: a GL line, or (thick) a thin box around it. */
    private static void segment(double x1, double y1, double z1, double x2, double y2, double z2, int c, double h,
                                MatrixStack.Entry entry, VertexConsumer quads, VertexConsumer lines) {
        if (lines != null) {
            float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len <= 0) return;
            nx /= len;
            ny /= len;
            nz /= len;
            lines.vertex(entry, (float) x1, (float) y1, (float) z1).color(c).normal(entry, nx, ny, nz);
            lines.vertex(entry, (float) x2, (float) y2, (float) z2).color(c).normal(entry, nx, ny, nz);
        } else if (quads != null && h > 0) {
            box(quads, entry, Math.min(x1, x2) - h, Math.min(y1, y2) - h, Math.min(z1, z2) - h,
                Math.max(x1, x2) + h, Math.max(y1, y2) + h, Math.max(z1, z2) + h, c);
        }
    }

    /** Camera-facing flat strip from p along d with half width h (for the non axis-aligned look line). */
    private static void ribbon(VertexConsumer vc, MatrixStack.Entry e, float px, float py, float pz, float dx, float dy, float dz, float h, int c) {
        // camera is at the origin: perpendicular = normalize(d x (-mid))
        float mx = px + dx * 0.5f, my = py + dy * 0.5f, mz = pz + dz * 0.5f;
        float cx = dy * -mz - dz * -my, cy = dz * -mx - dx * -mz, cz = dx * -my - dy * -mx;
        float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (len < 1e-6f) return;
        cx = cx / len * h;
        cy = cy / len * h;
        cz = cz / len * h;
        quad(vc, e, c, px + cx, py + cy, pz + cz, px + dx + cx, py + dy + cy, pz + dz + cz,
            px + dx - cx, py + dy - cy, pz + dz - cz, px - cx, py - cy, pz - cz);
    }

    private static void flush(VertexConsumerProvider consumers, RenderLayer layer) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw(layer);
    }

    /** Six quads of an axis-aligned box (culling is off, so the winding does not matter). */
    private static void box(VertexConsumer vc, MatrixStack.Entry e, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int c) {
        float x1 = (float) minX, y1 = (float) minY, z1 = (float) minZ;
        float x2 = (float) maxX, y2 = (float) maxY, z2 = (float) maxZ;
        quad(vc, e, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(vc, e, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(vc, e, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(vc, e, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(vc, e, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(vc, e, c, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.vertex(e, ax, ay, az).color(c);
        vc.vertex(e, bx, by, bz).color(c);
        vc.vertex(e, cx, cy, cz).color(c);
        vc.vertex(e, dx, dy, dz).color(c);
    }
}
