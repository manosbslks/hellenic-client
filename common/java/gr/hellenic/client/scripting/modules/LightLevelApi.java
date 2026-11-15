package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.LightLevelConfig;
import gr.hellenic.client.modules.esp.LightLevel;

public class LightLevelApi extends ModuleApi<LightLevelConfig> {

    @Override
    protected LightLevelConfig getConfig() {
        return ConfigStore.instance.getConfig().lightLevelConfig;
    }

    @Override
    protected void onEnableChanged() {
        LightLevel.instance.onChanged();
    }
}