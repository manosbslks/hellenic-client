package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoStunnerConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoStunnerApi extends ModuleApi<AutoStunnerConfig> {

    @Override
    protected AutoStunnerConfig getConfig() {
        return ConfigStore.instance.getConfig().autoStunnerConfig;
    }
}