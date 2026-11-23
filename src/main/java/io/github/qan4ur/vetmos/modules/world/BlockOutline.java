package io.github.qan4ur.vetmos.modules.world;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.OptionalDouble;

/**
 * Replaces the vanilla block selection outline through Fabric's {@link WorldRenderEvents#BLOCK_OUTLINE}
 * (fired only after vanilla's own visibility checks: F1, spectator, adventure mode, world border...).
 * Returning false cancels the vanilla outline, then the custom one is drawn into the world's vertex consumers.
 */
public class BlockOutline extends Module {
    private static final String GENERAL = "Основное";
    private static final String COLORS = "Цвета";

    // Translucent position/color quads, depth-tested like vanilla or always on top
    private static final RenderPipeline QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("vetmos", "pipeline/block_outline_quads"))
        .withCull(false)
        .withDepthWrite(false)
        .build());
    private static final RenderPipeline QUADS_NO_DEPTH_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("vetmos", "pipeline/block_outline_quads_no_depth"))
        .withCull(false)
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build());
    private static final RenderPipeline LINES_NO_DEPTH_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withLocation(Identifier.of("vetmos", "pipeline/block_outline_lines_no_depth"))
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build());

    private static final RenderLayer QUADS = RenderLayer.of("vetmos_block_outline_quads", 1536, false, true, QUADS_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer QUADS_NO_DEPTH = RenderLayer.of("vetmos_block_outline_quads_no_depth", 1536, false, true, QUADS_NO_DEPTH_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of("vetmos_block_outline_lines_no_depth", 1536, LINES_NO_DEPTH_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty())).build(false));

    // General

    private final DoubleSetting lineWidth = decimal(GENERAL, "line-width", "Толщина линии",
        "Толщина обводки примерно в пикселях. 1 — обычные тонкие линии.", 2.5, 1, 10, 1);

    private final BoolSetting fill = bool(GENERAL, "fill", "Заливка", "Заливать выбранный блок полупрозрачным цветом.", true);

    private final BoolSetting throughWalls = bool(GENERAL, "through-walls", "Сквозь стены",
        "Рисовать обводку поверх всего, а не с проверкой глубины, как в ванилле.", false);

    // Colors

    private final ColorSetting lineColor = color(COLORS, "line-color", "Цвет линии", "Цвет обводки.", ColorSetting.rgb(240, 240, 240));

    private final ColorSetting fillColor = color(COLORS, "fill-color", "Цвет заливки", "Цвет заливки.", ColorSetting.argb(255, 255, 255, 30))
        .visibleWhen(fill::get);

    public BlockOutline() {
        super("block-outline", "Обводка блока", "Свой цвет, толщина и заливка обводки выбранного блока.", Category.WORLD);
    }

    @Override
    public void onInit() {
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) -> {
            if (!isEnabled()) return true;
            boolean[] drawn = {false};
            ModuleManager.safe(this, () -> drawn[0] = render(context, outline));
            return !drawn[0];
        });
    }

    /** Returns true when the custom outline was drawn (vanilla is then cancelled). */
    private boolean render(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outline) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null || mc.world == null) return false;

        BlockPos pos = outline.blockPos();
        BlockState state = outline.blockState();
        Entity camera = outline.entity();
        VoxelShape shape = state.getOutlineShape(mc.world, pos, camera != null ? ShapeContext.of(camera) : ShapeContext.absent());
        if (shape.isEmpty()) return true; // nothing to outline, vanilla would draw nothing either

        MatrixStack matrices = context.matrixStack() != null ? context.matrixStack() : new MatrixStack();
        MatrixStack.Entry entry = matrices.peek();

        // Camera-relative block origin (vertices sent to the world consumers must be relative to the camera)
        double x = pos.getX() - outline.cameraX();
        double y = pos.getY() - outline.cameraY();
        double z = pos.getZ() - outline.cameraZ();

        boolean onTop = throughWalls.get();
        RenderLayer quadLayer = onTop ? QUADS_NO_DEPTH : QUADS;

        // Fill
        if (fill.get()) {
            VertexConsumer quads = consumers.getBuffer(quadLayer);
            int c = fillColor.get();
            double e = 0.002;
            for (Box b : shape.getBoundingBoxes()) {
                box(quads, entry, x + b.minX - e, y + b.minY - e, z + b.minZ - e, x + b.maxX + e, y + b.maxY + e, z + b.maxZ + e, c);
            }
        }

        // Outline
        int color = lineColor.get();
        double width = lineWidth.get();

        if (width <= 1.05) {
            RenderLayer lineLayer = onTop ? LINES_NO_DEPTH : RenderLayer.getLines();
            VertexConsumer lines = consumers.getBuffer(lineLayer);
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                lines.vertex(entry, (float) (x + x1), (float) (y + y1), (float) (z + z1)).color(color).normal(entry, nx, ny, nz);
                lines.vertex(entry, (float) (x + x2), (float) (y + y2), (float) (z + z2)).color(color).normal(entry, nx, ny, nz);
            });
            flush(consumers, lineLayer);
        } else {
            // Core profile can't widen GL lines, so each edge is drawn as a thin box. Scaled by distance so the
            // thickness stays roughly constant on screen (~0.0013 blocks per pixel per block of distance at 70 FOV / 1080p).
            double dx = x + 0.5, dy = y + 0.5, dz = z + 0.5;
            double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy + dz * dz));
            double h = width * 0.5 * 0.0013 * dist;

            VertexConsumer quads = consumers.getBuffer(quadLayer);
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> box(quads, entry,
                x + Math.min(x1, x2) - h, y + Math.min(y1, y2) - h, z + Math.min(z1, z2) - h,
                x + Math.max(x1, x2) + h, y + Math.max(y1, y2) + h, z + Math.max(z1, z2) + h,
                color));
        }

        flush(consumers, quadLayer);
        return true;
    }

    /** Draws the layer right away when the consumers are an immediate provider (they are, in BLOCK_OUTLINE). */
    private static void flush(VertexConsumerProvider consumers, RenderLayer layer) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw(layer);
    }

    /** Six quads of an axis-aligned box (culling is off, so the winding does not matter). */
    private static void box(VertexConsumer vc, MatrixStack.Entry e, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int c) {
        float x1 = (float) minX, y1 = (float) minY, z1 = (float) minZ;
        float x2 = (float) maxX, y2 = (float) maxY, z2 = (float) maxZ;

        quad(vc, e, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // bottom
        quad(vc, e, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // top
        quad(vc, e, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // north
        quad(vc, e, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // south
        quad(vc, e, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // west
        quad(vc, e, c, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // east
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
