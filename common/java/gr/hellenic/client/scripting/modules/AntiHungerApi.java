package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AntiHungerConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AntiHungerApi extends ModuleApi<AntiHungerConfig> {

    @Override
    protected AntiHungerConfig getConfig() {
        return ConfigStore.instance.getConfig().antiHungerConfig;
    }
}