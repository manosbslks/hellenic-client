package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.KillAuraConfig;

public class KillAuraApi extends ModuleApi<KillAuraConfig> {

    @Override
    protected KillAuraConfig getConfig() {
        return ConfigStore.instance.getConfig().killAuraConfig;
    }
}