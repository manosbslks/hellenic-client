package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.hacks.AirPlace;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

public class AirPlaceApi {

    @MethodDescription("""
            Checks if Air Place is active
            """)
    public boolean isEnabled() {
        return AirPlace.instance.isActive();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void enable() {
        AirPlace.instance.enable();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void disable() {
        AirPlace.instance.disable();
    }

    @MethodDescription("""
            Toggles Air Place status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        AirPlace.instance.toggle();
    }
}