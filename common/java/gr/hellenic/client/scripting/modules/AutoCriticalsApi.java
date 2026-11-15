package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoCriticalsConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoCriticalsApi extends ModuleApi<AutoCriticalsConfig> {

    @Override
    protected AutoCriticalsConfig getConfig() {
        return ConfigStore.instance.getConfig().autoCriticalsConfig;
    }
}