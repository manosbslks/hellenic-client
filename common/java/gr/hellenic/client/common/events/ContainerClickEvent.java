package gr.hellenic.client.common.events;

import net.minecraft.world.inventory.ClickType;

public record ContainerClickEvent(int slot, int button, ClickType type) {}