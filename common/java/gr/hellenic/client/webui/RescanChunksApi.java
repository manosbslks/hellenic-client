package gr.hellenic.client.webui;

import gr.hellenic.client.modules.esp.BlockFinder;

public class RescanChunksApi extends ApiBase {

    @Override
    public String getRoute() {
        return "rescan-chunks";
    }

    @Override
    public String post(String body) {
        BlockFinder.instance.rescan();
        return "{}";
    }
}