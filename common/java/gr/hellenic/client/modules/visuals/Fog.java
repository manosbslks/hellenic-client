package gr.hellenic.client.modules.visuals;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.mixins.common.accessors.FogRendererAccessor;
import gr.hellenic.client.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogRenderer;

public class Fog implements Module {

    public static final Fog instance = new Fog();

    private Fog() {
        Events.ClientTickStart.add(this::onClientTickStart);
    }

    public void onClientTickStart() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        if (ConfigStore.instance.getConfig().fogConfig.enabled) {
            if (FogRendererAccessor.isFogEnabled_CU()) {
                FogRenderer.toggleFog();
            }
        } else {
            if (!FogRendererAccessor.isFogEnabled_CU()) {
                FogRenderer.toggleFog();
            }
        }
    }
}