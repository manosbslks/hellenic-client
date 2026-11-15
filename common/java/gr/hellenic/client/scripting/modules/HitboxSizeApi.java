package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.HitboxSizeConfig;

public class HitboxSizeApi extends ModuleApi<HitboxSizeConfig> {

    @Override
    protected HitboxSizeConfig getConfig() {
        return ConfigStore.instance.getConfig().hitboxSizeConfig;
    }
}