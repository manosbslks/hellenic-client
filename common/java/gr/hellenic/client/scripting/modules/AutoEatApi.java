package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoEatConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoEatApi extends ModuleApi<AutoEatConfig> {

    @Override
    protected AutoEatConfig getConfig() {
        return ConfigStore.instance.getConfig().autoEatConfig;
    }
}