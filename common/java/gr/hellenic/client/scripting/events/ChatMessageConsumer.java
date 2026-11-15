package gr.hellenic.client.scripting.events;

@FunctionalInterface
public interface ChatMessageConsumer {
    void accept(String text);
}