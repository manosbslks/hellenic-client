package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoToolConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoToolApi extends ModuleApi<AutoToolConfig> {

    @Override
    protected AutoToolConfig getConfig() {
        return ConfigStore.instance.getConfig().autoTool;
    }
}