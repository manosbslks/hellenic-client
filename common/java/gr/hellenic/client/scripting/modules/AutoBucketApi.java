package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AutoBucketConfig;
import gr.hellenic.client.configs.ConfigStore;

public class AutoBucketApi extends ModuleApi<AutoBucketConfig> {

    @Override
    protected AutoBucketConfig getConfig() {
        return ConfigStore.instance.getConfig().autoBucketConfig;
    }
}