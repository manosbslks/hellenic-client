package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.modules.PacketEvent;

@FunctionalInterface
public interface PacketEventConsumer {
    void accept(PacketEvent event);
}