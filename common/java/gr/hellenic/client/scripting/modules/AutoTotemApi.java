package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoTotemConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoTotemApi extends ModuleApi<AutoTotemConfig> {

    @Override
    protected AutoTotemConfig getConfig() {
        return ConfigStore.instance.getConfig().autoTotemConfig;
    }
}