package io.github.qan4ur.vetmos.modules.inventory;

import io.github.qan4ur.vetmos.inventory.SortPlanner;
import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.EnumSetting;
import io.github.qan4ur.vetmos.setting.IntSetting;
import io.github.qan4ur.vetmos.setting.KeySetting;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Inventory sorting like Inventory Profiles Next: press the key (R by default) while an inventory or a container is
 * open — the part under the mouse (container or your inventory) is merged and sorted. Uses normal clicks only.
 */
public class InventorySort extends Module {
    public enum Order {
        CREATIVE("Как в креативе"), NAME("По названию"), ID("По ID предмета");

        private final String title;

        Order(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return Lang.tr("enum:InventorySort.Order." + name(), title);
        }
    }

    public final KeySetting sortKey = key("Основное", "key", "Клавиша сортировки",
        "Нажми в открытом инвентаре или сундуке - отсортируется та часть, над которой курсор. В мире клавиша ничего не делает.", GLFW.GLFW_KEY_R);
    public final EnumSetting<Order> order = enumeration("Основное", "order", "Порядок",
        "Как упорядочивать предметы: как в креативном меню (как в IPN), по названию или по ID.", Order.CREATIVE);
    public final BoolSetting includeHotbar = bool("Основное", "hotbar", "Сортировать хотбар",
        "Сортировать вместе с инвентарём и хотбар (обычно его не трогают).", false);
    public final IntSetting clicksPerTick = integer("Скорость", "clicks-per-tick", "Кликов за тик",
        "Сколько кликов делать за один тик (0 = всё сразу, как IPN). Меньше - медленнее, но аккуратнее для строгих серверов.", 0, 0, 20);
    public final BoolSetting sound = bool("Основное", "sound", "Звук",
        "Тихий щелчок после сортировки.", true);

    private final Deque<int[]> queue = new ArrayDeque<>(); // {syncId, slotId}
    private Map<Item, Integer> creativeIndex;

    public InventorySort() {
        super("inventory-sort", "Сортировка", "Сортировка инвентаря и сундуков на клавишу (по умолчанию R), как в Inventory Profiles Next.",
            Category.INVENTORY, true, Keys.NONE);
    }

    @Override
    public void onInit() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (queue.isEmpty()) return;
            if (client.player == null || client.interactionManager == null || client.player.currentScreenHandler == null) {
                queue.clear();
                return;
            }
            int limit = clicksPerTick.get() <= 0 ? Integer.MAX_VALUE : clicksPerTick.get();
            for (int i = 0; i < limit && !queue.isEmpty(); i++) {
                int[] c = queue.poll();
                if (client.player.currentScreenHandler.syncId != c[0]) {
                    queue.clear();
                    return;
                }
                client.interactionManager.clickSlot(c[0], c[1], 0, SlotActionType.PICKUP, client.player);
            }
        });
    }

    @Override
    public void onLeave() {
        queue.clear();
        creativeIndex = null;
    }

    /** Key / mouse button pressed inside a handled screen. Returns true if it sorted (event consumed). */
    public boolean onScreenInput(HandledScreen<?> screen, int key, Slot hovered) {
        if (!isEnabled() || key == Keys.NONE || key != sortKey.get()) return false;
        if (screen instanceof CreativeInventoryScreen) return false;
        return sort(screen.getScreenHandler(), hovered);
    }

    private boolean sort(ScreenHandler handler, Slot hovered) {
        if (mc.player == null || mc.interactionManager == null) return false;
        if (!queue.isEmpty()) return true; // still sorting
        if (!handler.getCursorStack().isEmpty()) return true; // never sort with an item on the cursor

        PlayerInventory inv = mc.player.getInventory();
        boolean hoverPlayer = hovered != null && hovered.inventory == inv;
        boolean containerSortable = isSortableContainer(handler);

        List<Slot> region = new ArrayList<>();
        if (!hoverPlayer && containerSortable) {
            for (Slot s : handler.slots) if (s.inventory != inv && s.isEnabled()) region.add(s);
        } else {
            for (Slot s : handler.slots) {
                if (s.inventory != inv || !s.isEnabled()) continue;
                int idx = s.getIndex();
                boolean main = idx >= 9 && idx < 36;
                boolean hotbar = idx >= 0 && idx < 9;
                if (main || (hotbar && includeHotbar.get() && (hovered == null || hoverPlayer))) region.add(s);
            }
            // keep the hotbar after the main inventory so items fill the main inventory first
            region.sort(Comparator.comparingInt(s -> s.getIndex() < 9 ? s.getIndex() + 100 : s.getIndex()));
        }
        if (region.size() < 2) return false;

        List<ItemStack> stacks = new ArrayList<>();
        for (Slot s : region) stacks.add(s.getStack());
        Slot first = region.get(0);
        List<Integer> clicks = SortPlanner.plan(stacks, st -> Math.min(st.getMaxCount(), first.getMaxItemCount(st)), comparator());
        if (clicks.isEmpty()) return true;

        for (int i : clicks) queue.add(new int[] { handler.syncId, region.get(i).id });
        if (clicksPerTick.get() <= 0) {
            // instant, like IPN: run now instead of waiting for the tick
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                mc.interactionManager.clickSlot(c[0], c[1], 0, SlotActionType.PICKUP, mc.player);
            }
        }
        if (sound.get()) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, 1.6f);
        return true;
    }

    private static boolean isSortableContainer(ScreenHandler h) {
        return h instanceof GenericContainerScreenHandler || h instanceof ShulkerBoxScreenHandler
            || h instanceof HopperScreenHandler || h instanceof Generic3x3ContainerScreenHandler;
    }

    private Comparator<ItemStack> comparator() {
        Comparator<ItemStack> byName = Comparator.comparing(st -> st.getName().getString().toLowerCase(Locale.ROOT));
        Comparator<ItemStack> byId = Comparator.comparingInt(st -> Registries.ITEM.getRawId(st.getItem()));
        Comparator<ItemStack> tail = byName.thenComparing(st -> st.getComponents().toString());
        return switch (order.get()) {
            case NAME -> byName.thenComparing(byId).thenComparing(tail);
            case ID -> byId.thenComparing(tail);
            case CREATIVE -> {
                Map<Item, Integer> idx = creativeIndex();
                yield Comparator.<ItemStack>comparingInt(st -> idx.getOrDefault(st.getItem(), 100000 + Registries.ITEM.getRawId(st.getItem())))
                    .thenComparing(tail);
            }
        };
    }

    /** Position of each item in the creative search tab (same order IPN uses by default). */
    private Map<Item, Integer> creativeIndex() {
        if (creativeIndex != null) return creativeIndex;
        Map<Item, Integer> map = new HashMap<>();
        try {
            if (mc.world != null) ItemGroups.updateDisplayContext(mc.world.getEnabledFeatures(), false, mc.world.getRegistryManager());
            int i = 0;
            for (ItemStack st : ItemGroups.getSearchGroup().getSearchTabStacks()) map.putIfAbsent(st.getItem(), i++);
        } catch (RuntimeException ignored) {}
        creativeIndex = map;
        return map;
    }
}
