package io.github.qan4ur.vetmos.modules.hud.info;

import io.github.qan4ur.vetmos.hud.HudPosition;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.List;

/** Latency to the server as shown in the player list. */
public class PingHud extends InfoHudModule {
    private final BoolSetting colorize = bool(G, "color-by-quality", "Цвет по качеству", "Зелёный — хороший пинг, красный — плохой.", true);
    private final BoolSetting hideSingleplayer = bool(G, "hide-singleplayer", "Скрывать в одиночной игре", "Не показывать пинг в одиночном мире.", true);

    public PingHud() {
        super("ping-hud", "Пинг", "Показывает задержку до сервера из списка игроков.");
    }

    @Override
    public HudPosition defaultHudPosition() {
        return new HudPosition(HudPosition.AnchorX.LEFT, HudPosition.AnchorY.TOP, 4, 23);
    }

    @Override
    protected boolean hasContent() {
        return !(hideSingleplayer.get() && mc.isInSingleplayer()) && entry() != null;
    }

    private PlayerListEntry entry() {
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net == null || mc.player == null) return null;
        return net.getPlayerListEntry(mc.player.getUuid());
    }

    @Override
    protected void collect(List<Line> out, boolean preview) {
        int ping;
        if (preview) {
            ping = 42;
        } else {
            PlayerListEntry e = entry();
            ping = e != null ? e.getLatency() : 0;
        }
        int color = 0;
        if (colorize.get()) color = ping <= 0 ? 0 : ping < 60 ? Quality.GOOD : ping < 130 ? Quality.MEDIUM : ping < 250 ? Quality.POOR : Quality.BAD;
        out.add(new Line(label("ping", "Пинг"), ping + " " + label("ms", "мс"), color));
    }
}
