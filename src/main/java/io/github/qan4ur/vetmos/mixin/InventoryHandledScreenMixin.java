package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.inventory.InventorySort;
import io.github.qan4ur.vetmos.modules.inventory.ShiftDrag;
import io.github.qan4ur.vetmos.util.Keys;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/** Sort key (InventorySort) and Shift + drag quick move (ShiftDrag) in inventory screens. */
@Mixin(HandledScreen.class)
public abstract class InventoryHandledScreenMixin extends Screen {
    @Shadow protected Slot focusedSlot;

    @Shadow protected abstract Slot getSlotAt(double x, double y);

    @Shadow protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Unique private final Set<Integer> vetmos$dragged = new HashSet<>();
    @Unique private boolean vetmos$dragging;

    protected InventoryHandledScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private boolean vetmos$typing() {
        return getFocused() instanceof TextFieldWidget;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vetmos$sortKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (vetmos$typing()) return;
        InventorySort sort = ModuleManager.get(InventorySort.class);
        if (sort != null && sort.onScreenInput((HandledScreen<?>) (Object) this, keyCode, focusedSlot)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vetmos$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // sort bound to a mouse button (e.g. middle click)
        InventorySort sort = ModuleManager.get(InventorySort.class);
        if (sort != null && button != 0 && sort.onScreenInput((HandledScreen<?>) (Object) this, Keys.mouse(button), getSlotAt(mouseX, mouseY))) {
            cir.setReturnValue(true);
            return;
        }
        // remember the first slot of a shift-drag (vanilla quick-moves it itself)
        vetmos$dragged.clear();
        vetmos$dragging = false;
        if (button == 0 && Screen.hasShiftDown() && vetmos$shiftDragOn()) {
            Slot slot = getSlotAt(mouseX, mouseY);
            vetmos$dragging = true;
            if (slot != null) vetmos$dragged.add(slot.id);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void vetmos$mouseDragged(double mouseX, double mouseY, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!vetmos$dragging || button != 0 || !Screen.hasShiftDown() || !vetmos$shiftDragOn()) return;
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (client == null || client.player == null || !self.getScreenHandler().getCursorStack().isEmpty()) return;
        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot == null || !slot.hasStack() || !vetmos$dragged.add(slot.id)) return;
        onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void vetmos$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) {
            vetmos$dragging = false;
            vetmos$dragged.clear();
        }
    }

    @Unique
    private boolean vetmos$shiftDragOn() {
        if ((Object) this instanceof CreativeInventoryScreen) return false;
        ShiftDrag m = ModuleManager.get(ShiftDrag.class);
        return m != null && m.isEnabled();
    }
}
