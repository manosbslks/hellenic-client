package gr.hellenic.client.scripting.events;

@FunctionalInterface
public interface EntityIdConsumer {
    void accept(int id);
}