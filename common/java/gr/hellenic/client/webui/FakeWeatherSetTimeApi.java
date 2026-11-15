package gr.hellenic.client.webui;

import gr.hellenic.client.modules.visuals.FakeWeather;

public class FakeWeatherSetTimeApi extends ApiBase {

    @Override
    public String getRoute() {
        return "fake-weather-set-time";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        FakeWeather.instance.setTime(request.value);
        return "{ \"ok\": true }";
    }

    public static class Request {
        public int value;
    }
}