package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.modules.EntityEspEvent;

@FunctionalInterface
public interface EntityEspConsumer {
    void accept(int id, EntityEspEvent event);
}