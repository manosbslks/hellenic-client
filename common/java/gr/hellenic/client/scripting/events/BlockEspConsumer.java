package gr.hellenic.client.scripting.events;

import gr.hellenic.client.scripting.modules.BlockEspEvent;
import gr.hellenic.client.scripting.types.BlockPosWrapper;

@FunctionalInterface
public interface BlockEspConsumer {
    void accept(BlockPosWrapper pos, BlockEspEvent event);
}