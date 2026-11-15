package gr.hellenic.client.configs;

import gr.hellenic.client.common.Registries;
import net.minecraft.world.item.Items;

public class BedrockBreakerConfig {

    public boolean replace;
    public String replaceBlockId;
    public boolean allBlocks;

    public BedrockBreakerConfig() {
        replaceBlockId = Registries.ITEMS.getKey(Items.NETHERRACK).toString();
    }
}