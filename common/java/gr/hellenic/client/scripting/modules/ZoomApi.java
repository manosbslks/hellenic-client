package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.visuals.Zoom;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;

public class ZoomApi {

    @ApiVisibility(ApiType.UPDATE)
    public void start(double fov, double seconds) {
        if (fov < 0.01 || fov > 150) {
            return;
        }
        if (seconds > 10) {
            return;
        }
        Zoom.instance.startZooming(fov, seconds);
    }

    @ApiVisibility(ApiType.UPDATE)
    public void stop() {
        Zoom.instance.stopZooming();
    }
}