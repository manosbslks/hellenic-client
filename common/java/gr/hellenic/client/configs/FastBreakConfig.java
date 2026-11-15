package gr.hellenic.client.configs;

import gr.hellenic.client.utils.MathUtils;

public class FastBreakConfig extends ModuleConfig implements ValidatableConfig {
    public double factor;
    public boolean disableDestroyDelay;

    public FastBreakConfig() {
        factor = 1.2;
    }

    public void validate() {
        factor = MathUtils.clamp(factor, 0.5, 10);
    }
}