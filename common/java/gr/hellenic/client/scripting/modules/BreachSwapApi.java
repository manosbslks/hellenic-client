package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.BreachSwapConfig;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.modules.automation.BreachSwap;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;

public class BreachSwapApi extends ModuleApi<BreachSwapConfig> {

    @Override
    protected BreachSwapConfig getConfig() {
        return ConfigStore.instance.getConfig().breachSwapConfig;
    }
}