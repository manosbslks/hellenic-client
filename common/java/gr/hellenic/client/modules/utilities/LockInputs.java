package gr.hellenic.client.modules.utilities;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.events.PlayerTurnByMouseEvent;
import gr.hellenic.client.configs.ConfigStore;

public class LockInputs {

    public static final LockInputs instance = new LockInputs();

    private LockInputs() {
        Events.PlayerTurnByMouse.add(this::onPlayerTurnByMouse);
    }

    private void onPlayerTurnByMouse(PlayerTurnByMouseEvent event) {
        if (ConfigStore.instance.getConfig().lockInputsConfig.mouseInputDisabled) {
            event.cancel();
        }
    }
}