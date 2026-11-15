package gr.hellenic.client.webui;

import gr.hellenic.client.modules.visuals.FakeWeather;

public class FakeWeatherSetRainApi extends ApiBase {

    @Override
    public String getRoute() {
        return "fake-weather-set-rain";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        FakeWeather.instance.setRain(request.value);
        return "{ \"ok\": true }";
    }

    public static class Request {
        public float value;
    }
}
