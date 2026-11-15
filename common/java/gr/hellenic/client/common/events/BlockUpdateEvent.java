package gr.hellenic.client.common.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public record BlockUpdateEvent(LevelChunk chunk, BlockPos pos, BlockState state) {}