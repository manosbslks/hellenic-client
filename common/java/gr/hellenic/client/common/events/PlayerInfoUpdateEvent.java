package gr.hellenic.client.common.events;

import net.minecraft.client.multiplayer.PlayerInfo;

public record PlayerInfoUpdateEvent(PlayerInfo info, PlayerInfoUpdateType type) {}
