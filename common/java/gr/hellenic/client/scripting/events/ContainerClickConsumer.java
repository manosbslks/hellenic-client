package gr.hellenic.client.scripting.events;

@FunctionalInterface
public interface ContainerClickConsumer {
    void accept(int slot, int button, String type);
}