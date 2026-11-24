package io.github.qan4ur.vetmos.modules.inventory;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.util.Keys;

/**
 * Fast item moving: hold Shift and LMB and sweep the mouse over slots — every slot you pass is shift-clicked
 * (moved to the other inventory), like in Inventory Profiles Next.
 */
public class ShiftDrag extends Module {
    public ShiftDrag() {
        super("shift-drag", "Быстрое перекладывание", "Зажми Shift и ЛКМ и проведи мышкой по слотам - все предметы, над которыми прошёл курсор, переложатся в другой инвентарь.",
            Category.INVENTORY, true, Keys.NONE);
    }
}
