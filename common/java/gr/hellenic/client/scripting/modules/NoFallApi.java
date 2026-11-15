package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.NoFallConfig;

public class NoFallApi extends ModuleApi<NoFallConfig> {

    @Override
    protected NoFallConfig getConfig() {
        return ConfigStore.instance.getConfig().noFallConfig;
    }
}