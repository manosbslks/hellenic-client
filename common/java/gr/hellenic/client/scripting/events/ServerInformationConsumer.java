package gr.hellenic.client.scripting.events;

@FunctionalInterface
public interface ServerInformationConsumer {
    void accept(ServerInformation info);
}