package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoHotbarConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoHotbarApi extends ModuleApi<AutoHotbarConfig> {

    @Override
    protected AutoHotbarConfig getConfig() {
        return ConfigStore.instance.getConfig().autoHotbarConfig;
    }
}