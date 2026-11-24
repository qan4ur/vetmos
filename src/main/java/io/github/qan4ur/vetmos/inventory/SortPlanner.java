package io.github.qan4ur.vetmos.inventory;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plans the left clicks (PICKUP) that sort a region of slots, by simulating vanilla click rules on copies of the
 * stacks. Identical items are merged into full stacks, then laid out in order from the first slot; empty slots end
 * up at the end. Only normal clicks are used, exactly like a player would do it.
 */
public final class SortPlanner {
    private SortPlanner() {}

    /**
     * @param stacks     current stacks of the region (index = position in region), not modified
     * @param maxCount   max count a slot of this region accepts for a stack (usually min(item max, 64))
     * @param comparator order of item kinds
     * @return click positions (indexes into the region) to perform in order; empty if already sorted
     */
    public static List<Integer> plan(List<ItemStack> stacks, java.util.function.ToIntFunction<ItemStack> maxCount, Comparator<ItemStack> comparator) {
        int n = stacks.size();
        ItemStack[] s = new ItemStack[n];
        for (int i = 0; i < n; i++) s[i] = stacks.get(i).copy();

        // ---- desired layout: merge identical stacks, full stacks first, then the remainder
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (ItemStack st : s) {
            if (st.isEmpty()) continue;
            int k = indexOfKind(kinds, st);
            if (k < 0) {
                kinds.add(st.copyWithCount(1));
                totals.add(st.getCount());
            } else totals.set(k, totals.get(k) + st.getCount());
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < kinds.size(); i++) order.add(i);
        order.sort((a, b) -> comparator.compare(kinds.get(a), kinds.get(b)));

        List<ItemStack> desired = new ArrayList<>();
        for (int k : order) {
            ItemStack kind = kinds.get(k);
            int max = Math.max(1, maxCount.applyAsInt(kind));
            int left = totals.get(k);
            while (left > 0) {
                int c = Math.min(max, left);
                desired.add(kind.copyWithCount(c));
                left -= c;
            }
        }
        if (desired.size() > n) return List.of(); // can't happen, but never risk a broken plan

        // ---- simulate clicks
        Sim sim = new Sim(s, maxCount);
        for (int p = 0; p < desired.size(); p++) {
            ItemStack target = desired.get(p);
            if (same(sim.s[p], target)) continue;

            if (sim.s[p].isEmpty() || !ItemStack.areItemsAndComponentsEqual(sim.s[p], target)) {
                int q = findKind(sim.s, p + 1, target, target.getCount());
                if (q < 0) return List.of(); // inconsistent, abort safely
                if (sim.s[p].isEmpty()) {
                    sim.click(q);
                    sim.click(p);
                } else {
                    sim.click(q);   // take Q
                    sim.click(p);   // swap: P on cursor
                    sim.click(q);   // put P where Q was
                }
            }
            // now same kind at p; top it up from later slots of the same kind
            int guard = 0;
            while (sim.s[p].getCount() < target.getCount() && guard++ < 64) {
                int q = findKind(sim.s, p + 1, target, -1);
                if (q < 0) return List.of();
                sim.click(q);
                sim.click(p);
                if (!sim.cursor.isEmpty()) sim.click(q);
            }
            if (!same(sim.s[p], target) || !sim.cursor.isEmpty()) return List.of();
        }
        return sim.clicks;
    }

    private static boolean same(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        return a.getCount() == b.getCount() && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static int indexOfKind(List<ItemStack> kinds, ItemStack st) {
        for (int i = 0; i < kinds.size(); i++) if (ItemStack.areItemsAndComponentsEqual(kinds.get(i), st)) return i;
        return -1;
    }

    /** First slot from {@code from} holding the kind; prefers an exact count when {@code preferCount > 0}. */
    private static int findKind(ItemStack[] s, int from, ItemStack kind, int preferCount) {
        int any = -1;
        for (int i = from; i < s.length; i++) {
            if (s[i].isEmpty() || !ItemStack.areItemsAndComponentsEqual(s[i], kind)) continue;
            if (preferCount > 0 && s[i].getCount() == preferCount) return i;
            if (any < 0) any = i;
        }
        return any;
    }

    /** Vanilla left-click (PICKUP, button 0) rules on a copy of the region. */
    private static final class Sim {
        final ItemStack[] s;
        final java.util.function.ToIntFunction<ItemStack> maxCount;
        ItemStack cursor = ItemStack.EMPTY;
        final List<Integer> clicks = new ArrayList<>();

        Sim(ItemStack[] s, java.util.function.ToIntFunction<ItemStack> maxCount) {
            this.s = s;
            this.maxCount = maxCount;
        }

        void click(int i) {
            clicks.add(i);
            ItemStack slot = s[i];
            if (cursor.isEmpty()) {
                cursor = slot;
                s[i] = ItemStack.EMPTY;
            } else if (slot.isEmpty()) {
                s[i] = cursor;
                cursor = ItemStack.EMPTY;
            } else if (ItemStack.areItemsAndComponentsEqual(slot, cursor)) {
                int room = Math.max(0, maxCount.applyAsInt(slot) - slot.getCount());
                int move = Math.min(room, cursor.getCount());
                s[i] = slot.copyWithCount(slot.getCount() + move);
                int left = cursor.getCount() - move;
                cursor = left <= 0 ? ItemStack.EMPTY : cursor.copyWithCount(left);
            } else {
                s[i] = cursor;
                cursor = slot;
            }
        }
    }
}
