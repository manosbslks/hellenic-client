package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.BoatHackConfig;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

@SuppressWarnings("unused")
public class BoatHackApi {

    @MethodDescription("""
            If Boat Fly enabled
            """)
    public boolean isFlyEnabled() {
        return getConfig().fly;
    }

    @MethodDescription("""
            Toggles Boat Fly status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggleFly() {
        BoatHackConfig config = getConfig();
        config.fly = !config.fly;
        ConfigStore.instance.requestWrite();
    }

    private BoatHackConfig getConfig() {
        return ConfigStore.instance.getConfig().boatHackConfig;
    }
}