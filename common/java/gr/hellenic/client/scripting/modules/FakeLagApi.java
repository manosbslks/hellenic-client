package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.hacks.FakeLag;
import gr.hellenic.client.scripting.ApiVisibility;
import gr.hellenic.client.scripting.ApiType;
import com.zergatul.scripting.MethodDescription;

public class FakeLagApi {

    @MethodDescription("""
            Activates Fake Lag
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void enable() {
        if (!isEnabled()) {
            toggle();
        }
    }

    @MethodDescription("""
            Stops Fake Lag
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void disable() {
        if (isEnabled()) {
            toggle();
        }
    }

    @MethodDescription("""
            Sets Fake Lag status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void setEnabled(boolean value) {
        if (value) {
            enable();
        } else {
            disable();
        }
    }

    @MethodDescription("""
            Checks if Fake Lag is active
            """)
    public boolean isEnabled() {
        return FakeLag.instance.isEnabled();
    }

    @MethodDescription("""
            Toggles Fake Lag status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        FakeLag.instance.toggle();
    }
}