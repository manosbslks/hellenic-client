package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.ScaffoldConfig;

public class ScaffoldApi extends ModuleApi<ScaffoldConfig> {

    @Override
    protected ScaffoldConfig getConfig() {
        return ConfigStore.instance.getConfig().scaffoldConfig;
    }
}