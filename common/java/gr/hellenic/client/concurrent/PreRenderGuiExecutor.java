package gr.hellenic.client.concurrent;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.events.RenderGuiEvent;

public class PreRenderGuiExecutor extends EventExecutor {

    public static final PreRenderGuiExecutor instance = new PreRenderGuiExecutor();

    private PreRenderGuiExecutor() {
        super(5000);
        Events.PreRenderGui.add(this::onPreRenderGui);
    }

    private void onPreRenderGui(RenderGuiEvent event) {
        processQueue();
    }
}