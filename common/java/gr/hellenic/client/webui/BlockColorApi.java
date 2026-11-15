package gr.hellenic.client.webui;

import gr.hellenic.client.common.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class BlockColorApi extends ApiBase {

    @Override
    public String getRoute() {
        return "block-color";
    }

    @Override
    public String get(String id) {
        Block block = Registries.BLOCKS.getValue(Identifier.parse(id));
        int color = Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), null, null, 0);
        return Integer.toString(color);
    }
}