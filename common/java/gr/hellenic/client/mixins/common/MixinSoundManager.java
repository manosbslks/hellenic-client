package gr.hellenic.client.mixins.common;

import gr.hellenic.client.modules.hacks.ElytraFly;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Inject(at = @At("HEAD"), method = "play", cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> info) {
        if (sound instanceof ElytraOnPlayerSoundInstance) {
            if (!ElytraFly.instance.shouldPlaySound()) {
                info.cancel();
            }
        }
    }
}