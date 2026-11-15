package gr.hellenic.client.wrappers;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.ReachConfig;

public class PickRange {

    public static double get() {
        ReachConfig config = ConfigStore.instance.getConfig().reachConfig;
        if (config.overrideReachDistance) {
            return config.reachDistance;
        } else {
            return 4.5;
        }
    }
}