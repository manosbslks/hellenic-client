package gr.hellenic.client.schematics;

import net.minecraft.world.level.block.state.BlockState;

public record PaletteEntry(String raw, BlockState state) {}