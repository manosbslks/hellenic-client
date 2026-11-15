package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.SpearRangeConfig;

public class SpearRangeApi extends ModuleApi<SpearRangeConfig> {


    @Override
    protected SpearRangeConfig getConfig() {
        return ConfigStore.instance.getConfig().spearRangeConfig;
    }
}