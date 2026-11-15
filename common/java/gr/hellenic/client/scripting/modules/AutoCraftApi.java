package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoCraftConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoCraftApi extends ModuleApi<AutoCraftConfig> {

    @Override
    protected AutoCraftConfig getConfig() {
        return ConfigStore.instance.getConfig().autoCraftConfig;
    }
}