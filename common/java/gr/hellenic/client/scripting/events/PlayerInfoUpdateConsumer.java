package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.types.PlayerInfoWrapper;

@FunctionalInterface
public interface PlayerInfoUpdateConsumer {
    void accept(PlayerInfoWrapper info, String type);
}