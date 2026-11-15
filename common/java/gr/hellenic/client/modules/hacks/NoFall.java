package gr.hellenic.client.modules.hacks;

import gr.hellenic.client.accessors.ServerboundMovePlayerPacketAccessor;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.NoFallConfig;
import gr.hellenic.client.controllers.NetworkPacketsController;
import gr.hellenic.client.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall implements Module {

    public static final NoFall instance = new NoFall();

    private final Minecraft mc = Minecraft.getInstance();

    private NoFall() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
    }

    public boolean isActive() {
        NoFallConfig config = ConfigStore.instance.getConfig().noFallConfig;
        if (!config.enabled) {
            return false;
        }

        if (ConfigStore.instance.getConfig().flyHackConfig.enabled) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        if (mc.player.isFallFlying()) {
            // flying with elytra
            return false;
        }

        return mc.player.getDeltaMovement().y < -0.5;
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (args.packet instanceof ServerboundMovePlayerPacket packet) {
            if (isActive()) {
                ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(true);
            }
        }
    }
}