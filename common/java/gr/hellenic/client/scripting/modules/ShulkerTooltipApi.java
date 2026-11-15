package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.ShulkerTooltipConfig;

public class ShulkerTooltipApi extends ModuleApi<ShulkerTooltipConfig> {

    @Override
    protected ShulkerTooltipConfig getConfig() {
        return ConfigStore.instance.getConfig().shulkerTooltipConfig;
    }
}