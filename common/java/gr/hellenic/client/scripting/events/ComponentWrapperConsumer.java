package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.types.ComponentWrapper;

@FunctionalInterface
public interface ComponentWrapperConsumer {
    void accept(ComponentWrapper text);
}
