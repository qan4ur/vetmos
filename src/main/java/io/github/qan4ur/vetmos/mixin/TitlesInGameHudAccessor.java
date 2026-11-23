package io.github.qan4ur.vetmos.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to the title / subtitle / action bar state (used by the movable titles and action bar HUD). */
@Mixin(InGameHud.class)
public interface TitlesInGameHudAccessor {
    // All private fields of InGameHud (verified with javap, 1.21.7 yarn build.1)
    @Accessor("title")
    Text vetmos$titlesTitle();

    @Accessor("subtitle")
    Text vetmos$titlesSubtitle();

    @Accessor("titleRemainTicks")
    int vetmos$titlesRemainTicks();

    @Accessor("titleFadeInTicks")
    int vetmos$titlesFadeInTicks();

    @Accessor("titleStayTicks")
    int vetmos$titlesStayTicks();

    @Accessor("titleFadeOutTicks")
    int vetmos$titlesFadeOutTicks();

    @Accessor("overlayMessage")
    Text vetmos$titlesOverlayMessage();

    @Accessor("overlayRemaining")
    int vetmos$titlesOverlayRemaining();

    @Accessor("overlayTinted")
    boolean vetmos$titlesOverlayTinted();
}
