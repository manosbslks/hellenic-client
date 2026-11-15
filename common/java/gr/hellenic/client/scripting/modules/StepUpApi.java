package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.StepUpConfig;

public class StepUpApi extends ModuleApi<StepUpConfig> {

    @Override
    protected StepUpConfig getConfig() {
        return ConfigStore.instance.getConfig().stepUp;
    }
}
