package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.chunkoverlays.ExplorationMiniMapChunkOverlay;
import gr.hellenic.client.controllers.ChunkOverlayController;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;

public class ExplorationMiniMapApi {

    @ApiVisibility(ApiType.UPDATE)
    public void addMarker() {
        ChunkOverlayController.instance.ofType(ExplorationMiniMapChunkOverlay.class).addMarker();
    }
}