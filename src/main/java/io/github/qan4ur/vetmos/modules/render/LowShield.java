package io.github.qan4ur.vetmos.modules.render;

import io.github.qan4ur.vetmos.module.Category;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.setting.BoolSetting;
import io.github.qan4ur.vetmos.setting.DoubleSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;

/** Lowers and scales the shield in first person. See LowShieldHeldItemRendererMixin. */
public class LowShield extends Module {
    private static final String GENERAL = "Основное";

    private final DoubleSetting lower = decimal(GENERAL, "lower", "Опустить", "Насколько опустить щит.", 0.2, 0, 0.8, 2);

    private final DoubleSetting scale = decimal(GENERAL, "scale", "Размер", "Размер щита в руке.", 1.0, 0.3, 1.5, 2);

    private final BoolSetting whileBlocking = bool(GENERAL, "while-blocking", "При блоке",
        "Применять и тогда, когда ты закрываешься щитом.", true);

    public LowShield() {
        super("low-shield", "Низкий щит", "Опускает и уменьшает щит от первого лица.", Category.RENDER);
    }

    /** Called right before a held item is rendered (HeldItemRenderer#renderItem). */
    public static void transform(ItemStack stack, ItemDisplayContext context, MatrixStack matrices) {
        if (context == null || !context.isFirstPerson() || stack == null || !(stack.getItem() instanceof ShieldItem)) return;
        LowShield m = ModuleManager.get(LowShield.class);
        if (m == null || !m.isEnabled()) return;
        if (!m.whileBlocking.get() && mc.player != null && mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof ShieldItem) return;

        matrices.translate(0f, -m.lower.getFloat(), 0f);
        float s = m.scale.getFloat();
        if (s != 1f) matrices.scale(s, s, s);
    }
}
