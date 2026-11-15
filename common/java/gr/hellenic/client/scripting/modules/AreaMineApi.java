package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AreaMineConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AreaMineApi extends ModuleApi<AreaMineConfig> {

    @Override
    protected AreaMineConfig getConfig() {
        return ConfigStore.instance.getConfig().areaMineConfig;
    }
}