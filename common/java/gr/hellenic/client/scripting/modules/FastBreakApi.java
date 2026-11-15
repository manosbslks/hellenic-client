package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.FastBreakConfig;

public class FastBreakApi extends ModuleApi<FastBreakConfig> {

    @Override
    protected FastBreakConfig getConfig() {
        return ConfigStore.instance.getConfig().fastBreakConfig;
    }
}