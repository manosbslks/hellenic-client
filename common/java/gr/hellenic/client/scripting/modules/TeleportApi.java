package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.controllers.TeleportHackController;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import gr.hellenic.client.utils.MathUtils;

@SuppressWarnings("unused")
public class TeleportApi {

    @ApiVisibility(ApiType.ACTION)
    public boolean toCrosshair(double distance, int repeats) {
        distance = MathUtils.clamp(distance, 1, 1000);
        repeats = MathUtils.clamp(repeats, 0, 100);
        return TeleportHackController.instance.teleportToCrosshair(distance, repeats);
    }

    @ApiVisibility(ApiType.ACTION)
    public boolean vertical(double distance, int repeats) {
        distance = MathUtils.absClamp(distance, 1, 1000);
        repeats = MathUtils.clamp(repeats, 0, 100);
        return TeleportHackController.instance.verticalTeleport(distance, repeats);
    }

    @ApiVisibility(ApiType.ACTION)
    public boolean vertical(double fromDistance, double toDistance, boolean findSurface, int repeats) {
        fromDistance = MathUtils.absClamp(fromDistance, -1000, 1000);
        toDistance = MathUtils.absClamp(toDistance, -1000, 1000);
        repeats = MathUtils.clamp(repeats, 0, 100);
        return TeleportHackController.instance.verticalTeleport(fromDistance, toDistance, findSurface, repeats);
    }
}