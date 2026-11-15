package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.FullBrightConfig;

public class FullBrightApi extends ModuleApi<FullBrightConfig> {

    @Override
    protected FullBrightConfig getConfig() {
        return ConfigStore.instance.getConfig().fullBrightConfig;
    }
}