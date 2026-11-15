package gr.hellenic.client.controllers;

import gr.hellenic.client.modules.esp.FreeCam;
import gr.hellenic.client.concurrent.ProfilerSingleThreadExecutor;

import java.text.DecimalFormat;
import java.util.List;

public class DebugScreenController {

    public static final DebugScreenController instance = new DebugScreenController();

    private final DecimalFormat format = new DecimalFormat("0.00");

    private DebugScreenController() {

    }

    public void onGetGameInformation(List<String> list) {
        ProfilerSingleThreadExecutor executor = BlockEventsProcessor.instance.getExecutor();

        list.add("");
        list.add("Hellenic Client");
        list.add(String.format("BlockEvents thread: queue size=%s; successful=%d; failed=%d; rejected=%d; busy=%s;",
                executor.getQueueSize(),
                executor.getSuccessful(),
                executor.getFailed(),
                executor.getRejected(),
                format.format(executor.getBusyPercentage()) + "%"));

    }
}