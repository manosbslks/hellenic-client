package gr.hellenic.client.mixins.common;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.events.ScreenRenderEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
    private void onAfterRender(GuiGraphics guiGraphics, int x, int y, float partialTicks, CallbackInfo info) {
        Events.AfterScreenRendered.trigger(new ScreenRenderEvent(guiGraphics, x, y));
    }
}