package gr.hellenic.client.webui;

import gr.hellenic.client.utils.EntityUtils;

public class EntityInfoApi extends ApiBase {

    @Override
    public String getRoute() {
        return "entity-info";
    }

    @Override
    public String get() {
        return gson.toJson(EntityUtils.getEntityClasses());
    }
}