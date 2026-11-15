package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoAttackConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoAttackApi extends ModuleApi<AutoAttackConfig> {

    @Override
    protected AutoAttackConfig getConfig() {
        return ConfigStore.instance.getConfig().autoAttackConfig;
    }
}
