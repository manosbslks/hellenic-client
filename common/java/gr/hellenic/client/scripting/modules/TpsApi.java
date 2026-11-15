package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.controllers.TpsCounterController;

public class TpsApi {

    public double get() {
        return TpsCounterController.instance.getTps();
    }
}