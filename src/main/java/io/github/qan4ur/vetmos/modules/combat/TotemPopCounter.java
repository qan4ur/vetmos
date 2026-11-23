package io.github.qan4ur.vetmos.modules.combat;

import io.github.qan4ur.vetmos.Vetmos;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.modules.hud.HudRender2;
import io.github.qan4ur.vetmos.setting.*;
import io.github.qan4ur.vetmos.util.ChatUtil;
import io.github.qan4ur.vetmos.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static io.github.qan4ur.vetmos.modules.hud.HudRender2.*;

/** Counts totem pops per player, prints them to chat and shows an animated badge above heads. */
public class TotemPopCounter extends Module {
    private static final byte STATUS_TOTEM = 35, STATUS_DEATH = 3;

    private static final String G = "Основное", R = "Рендер", C = "Цвета";

    private final BoolSetting ignoreSelf = bool(G, "ignore-self", "Не считать себя", "Не считать собственные попы.", false);
    private final BoolSetting chat = bool(G, "chat-message", "Сообщение в чат", "Писать в чат, когда кто-то попнул тотем.", true);
    private final BoolSetting deathMessage = bool(G, "death-message", "Сообщение о смерти", "Писать в чат, когда игрок умер после попов.", true)
        .visibleWhen(chat::get);

    private final BoolSetting render = bool(R, "above-head", "Над головой", "Показывать число попов над головами игроков.", true);
    private final BoolSetting totemIcon = bool(R, "totem-icon", "Иконка тотема", "Маленькая иконка тотема рядом с числом.", true)
        .visibleWhen(render::get);
    private final BoolSetting floatingText = bool(R, "float-up", "Всплывающее -1", "При каждом попе «-1» всплывает и тает.", true)
        .visibleWhen(render::get);
    private final DoubleSetting scale = decimal(R, "scale", "Размер", "Размер значка.", 1.2, 0.3, 4)
        .visibleWhen(render::get);
    private final DoubleSetting height = decimal(R, "height", "Высота", "Высота над головой (блоки). Поднимите, если перекрывает ник.", 0.95, 0, 3)
        .visibleWhen(render::get);
    private final IntSetting range = integer(R, "range", "Дальность", "Максимальная дистанция отрисовки значков.", 64, 1, 256)
        .visibleWhen(render::get);

    private final ColorSetting bgColor = color(C, "background", "Фон", "Фон значка.", Theme.PANEL);
    private final ColorSetting accentColor = color(C, "accent", "Акцент", "Цвет числа и линии снизу.", Theme.SCARLET);
    private final ColorSetting glowColor = color(C, "pop-flash", "Вспышка", "Цвет вспышки сразу после попа.", Theme.GLOW);

    private static final class PopState {
        int pops;
        long lastPopNs;
        boolean listed;
        final ArrayDeque<Long> floaters = new ArrayDeque<>();
    }

    private static final double BOUNCE_TIME = 0.6, FLOAT_TIME = 0.9;
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    private final Map<UUID, PopState> pops = new HashMap<>();
    private final List<PlayerEntity> drawList = new ArrayList<>();
    private ClientWorld lastWorld;

    public TotemPopCounter() {
        super("totem-pop-counter", "Счётчик тотемов", "Считает попы тотемов у игроков и показывает их над головой.", Category.COMBAT);
    }

    @Override
    public void onInit() {
        HudRender2.initProjection();
        HudElementRegistry.addLast(Vetmos.id("totem-pop-badges"), this::renderBadges);
    }

    @Override
    protected void onEnable() {
        pops.clear();
        lastWorld = mc.world;
    }

    @Override
    protected void onDisable() {
        pops.clear();
    }

    @Override
    public void onJoin() {
        pops.clear();
    }

    @Override
    public void onLeave() {
        pops.clear();
        lastWorld = null;
    }

    /** Pop count of a player, 0 if none. */
    public int getPops(PlayerEntity player) {
        PopState s = pops.get(player.getUuid());
        return s == null ? 0 : s.pops;
    }

    // ---------------------------------------------------------------- counting

    @Override
    public void onEntityStatus(Entity entity, byte status) {
        if (!(entity instanceof PlayerEntity player) || mc.player == null) return;
        if (status == STATUS_TOTEM) onPop(player);
        else if (status == STATUS_DEATH) onDeath(player);
    }

    private void onPop(PlayerEntity player) {
        if (player == mc.player && ignoreSelf.get()) return;

        PopState state = pops.computeIfAbsent(player.getUuid(), k -> new PopState());
        state.pops++;
        state.lastPopNs = System.nanoTime();
        state.floaters.addLast(state.lastPopNs);
        while (state.floaters.size() > 6) state.floaters.removeFirst();

        if (chat.get()) {
            ChatUtil.send(Text.empty()
                .append(Text.literal("Игрок ").withColor(0xFFAAAA))
                .append(nameText(player))
                .append(Text.literal(" попнул ").withColor(0xFFAAAA))
                .append(count(state.pops))
                .append(Text.literal(" " + totems(state.pops)).withColor(0xFFAAAA)));
        }
    }

    private void onDeath(PlayerEntity player) {
        PopState state = pops.remove(player.getUuid());
        if (state == null || state.pops <= 0) return;
        if (chat.get() && deathMessage.get()) {
            ChatUtil.send(Text.empty()
                .append(Text.literal("Игрок ").withColor(0xFFAAAA))
                .append(nameText(player))
                .append(Text.literal(" умер, попнув ").withColor(0xFFAAAA))
                .append(count(state.pops))
                .append(Text.literal(" " + totems(state.pops)).withColor(0xFFAAAA)));
        }
    }

    @Override
    public void onTick() {
        if (mc.world != lastWorld) {
            // dimension change / new world
            pops.clear();
            lastWorld = mc.world;
        }
        if (mc.world == null || pops.isEmpty()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            PopState state = pops.get(player.getUuid());
            if (state == null) continue;
            if (player.isDead() || player.getHealth() <= 0 || player.deathTime > 0) onDeath(player);
        }

        // forget players that left the server (they were in the tab list and are gone now)
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net != null) {
            pops.entrySet().removeIf(e -> {
                boolean inList = net.getPlayerListEntry(e.getKey()) != null;
                if (inList) e.getValue().listed = true;
                return e.getValue().listed && !inList;
            });
        }
    }

    private Text nameText(PlayerEntity player) {
        return Text.literal(player.getName().getString()).withColor(accentColor.get() & 0xFFFFFF);
    }

    private MutableText count(int n) {
        return Text.literal(String.valueOf(n)).withColor(accentColor.get() & 0xFFFFFF).formatted(Formatting.BOLD);
    }

    /** Russian plural: 1 тотем, 3 тотема, 5 тотемов. */
    private static String totems(int n) {
        int mod100 = n % 100, mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return "тотемов";
        if (mod10 == 1) return "тотем";
        if (mod10 >= 2 && mod10 <= 4) return "тотема";
        return "тотемов";
    }

    // ---------------------------------------------------------------- badges

    private void renderBadges(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!isEnabled() || !render.get() || mc.world == null || mc.player == null || mc.options.hudHidden || pops.isEmpty()) return;

        float tickDelta = tickCounter.getTickProgress(true);
        long now = System.nanoTime();
        TextRenderer tr = mc.textRenderer;
        Vec3d cam = HudRender2.cameraPos();
        double maxSq = range.get() * range.get();

        drawList.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            PopState state = pops.get(player.getUuid());
            if (state == null || state.pops <= 0) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (player.squaredDistanceTo(cam) > maxSq) continue;
            if (player.isInvisibleTo(mc.player)) continue;
            drawList.add(player);
        }
        if (drawList.isEmpty()) return;
        // far first, so closer badges end up on top
        drawList.sort(Comparator.comparingDouble((PlayerEntity p) -> p.squaredDistanceTo(cam)).reversed());

        Matrix3x2fStack m = ctx.getMatrices();
        for (PlayerEntity player : drawList) {
            Vec3d pos = player.getLerpedPos(tickDelta).add(0, player.getHeight() + height.get(), 0);
            float[] screen = HudRender2.project(pos.x, pos.y, pos.z);
            if (screen == null) continue;

            // shrink a little with distance so far badges don't cover the screen
            double dist = Math.sqrt(pos.squaredDistanceTo(cam));
            float s = (float) (scale.get() * MathHelper.clamp(1.0 - dist / 96.0, 0.45, 1.0));

            m.pushMatrix();
            m.translate(screen[0], screen[1]);
            m.scale(s, s);
            drawBadge(ctx, tr, pops.get(player.getUuid()), accentColor.get(), now);
            m.popMatrix();
        }
    }

    private void drawBadge(DrawContext ctx, TextRenderer tr, PopState state, int accent, long now) {
        Matrix3x2fStack m = ctx.getMatrices();
        double since = (now - state.lastPopNs) / 1e9;

        String text = "-" + state.pops;
        int tw = tr.getWidth(text);
        int iconW = totemIcon.get() ? 12 : 0;
        int w = 4 + iconW + tw + 4;
        int h = 14;
        int x = -w / 2, y = -h;

        // bounce: overshoot then settle (damped spring)
        float bounce = 1f;
        float flash = 0f;
        if (since < BOUNCE_TIME) {
            float t = (float) since;
            bounce = 1f + 0.55f * (float) (Math.exp(-7 * t) * Math.cos(16 * t));
            flash = 1f - (float) (since / BOUNCE_TIME);
        }

        m.pushMatrix();
        m.translate(0, y + h / 2f);
        float b = Math.max(0.05f, bounce);
        m.scale(b, b);
        m.translate(0, -(y + h / 2f));

        // flash halo
        if (flash > 0) {
            int g = glowColor.get();
            int gc = withAlpha(g, (int) ((g >>> 24) * flash));
            int grow = Math.round(3 * flash);
            ctx.fill(x - grow, y - grow + 1, x + w + grow, y + h + grow - 1, gc);
            ctx.fill(x - grow + 1, y - grow, x + w + grow - 1, y + h + grow, gc);
        }

        // badge body (rounded)
        int bg = bgColor.get();
        ctx.fill(x + 1, y, x + w - 1, y + h, bg);
        ctx.fill(x, y + 1, x + 1, y + h - 1, bg);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, accent | 0xFF000000);

        int tx = x + 4;
        if (iconW > 0) {
            m.pushMatrix();
            m.translate(tx, y + 1);
            m.scale(0.75f, 0.75f);
            ctx.drawItem(TOTEM, 0, 0);
            m.popMatrix();
            tx += iconW;
        }

        int textColor = lerpColor(accent | 0xFF000000, 0xFFFFFFFF, flash);
        ctx.drawText(tr, text, tx, y + 3, textColor, true);
        m.popMatrix();

        // floating "-1"s
        if (floatingText.get()) {
            Iterator<Long> it = state.floaters.iterator();
            while (it.hasNext()) {
                double t = (now - it.next()) / 1e9 / FLOAT_TIME;
                if (t >= 1) {
                    it.remove();
                    continue;
                }
                float e = easeOutCubic((float) t);
                float alpha = 1f - (float) (t * t);
                float rise = 6 + 16 * e;
                float drift = (float) Math.sin(t * Math.PI * 1.5) * 4;

                m.pushMatrix();
                m.translate(w / 2f + 2 + drift, y - rise);
                float sc = 0.9f + 0.3f * (1 - e);
                m.scale(sc, sc);
                ctx.drawText(tr, "-1", 0, 0, withAlpha(accent, (int) (255 * MathHelper.clamp(alpha, 0, 1))), true);
                m.popMatrix();
            }
        }
    }
}
