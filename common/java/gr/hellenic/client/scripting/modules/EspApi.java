package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.esp.EspGlobal;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

public class EspApi {

    @MethodDescription("""
            Checks if ESPs rendering is enabled
            """)
    public boolean isEnabled() {
        return EspGlobal.enabled;
    }

    @MethodDescription("""
            Enables/disables rendering of all ESP modules
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        EspGlobal.enabled = !EspGlobal.enabled;
    }
}