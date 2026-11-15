package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.modules.PlayerMessageSendingEvent;

@FunctionalInterface
public interface PlayerMessageSendingConsumer {
    void consume(PlayerMessageSendingEvent event);
}