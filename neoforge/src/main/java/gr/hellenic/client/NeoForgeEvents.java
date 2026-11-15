package gr.hellenic.client;

import com.mojang.datafixers.util.Either;
import gr.hellenic.client.common.events.GatherTooltipComponentsEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.List;

import static gr.hellenic.client.common.Events.*;

@SuppressWarnings("unused")
public class NeoForgeEvents {

    @SubscribeEvent
    public void onPreRenderTooltip(RenderTooltipEvent.GatherComponents event) {
        List<Component> list = new ArrayList<>();
        GatherTooltipComponents.trigger(new GatherTooltipComponentsEvent(event.getItemStack(), list));
        list.forEach(c -> event.getTooltipElements().add(Either.left(c)));
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            RawChunkLoaded.trigger(event.getChunk());
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RawChunkUnloaded.trigger(event.getChunk());
        }
    }
}