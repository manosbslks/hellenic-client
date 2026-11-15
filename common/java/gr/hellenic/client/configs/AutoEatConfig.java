package gr.hellenic.client.configs;

import gr.hellenic.client.utils.MathUtils;

public class AutoEatConfig extends ModuleConfig implements ValidatableConfig {

    public boolean isHungerLimitEnabled;
    public int hungerLimit;

    public AutoEatConfig() {
        hungerLimit = 10;
    }

    @Override
    public void validate() {
        hungerLimit = MathUtils.clamp(hungerLimit, 0, 100);
    }
}