package gr.hellenic.client.scripting.events;

@FunctionalInterface
public interface BlockPosConsumer {
    void accept(int x, int y, int z);
}