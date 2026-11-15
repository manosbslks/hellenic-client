package gr.hellenic.client.configs;

import gr.hellenic.client.utils.MathUtils;

public class PerformanceConfig implements ValidatableConfig, ModuleStateProvider {

    public boolean limitBackgroundWindowFps;
    public int backgroundWindowFps;

    public PerformanceConfig() {
        backgroundWindowFps = 20;
    }

    @Override
    public void validate() {
        backgroundWindowFps = MathUtils.clamp(backgroundWindowFps, 1, 120);
    }

    @Override
    public boolean isEnabled() {
        return limitBackgroundWindowFps;
    }
}