package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ArmorOverlayConfig;
import gr.hellenic.client.configs.ConfigStore;

public class ArmorOverlayApi extends ModuleApi<ArmorOverlayConfig> {

    @Override
    protected ArmorOverlayConfig getConfig() {
        return ConfigStore.instance.getConfig().armorOverlayConfig;
    }
}