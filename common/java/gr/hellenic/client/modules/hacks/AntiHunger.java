package gr.hellenic.client.modules.hacks;

import gr.hellenic.client.accessors.ServerboundMovePlayerPacketAccessor;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.NetworkPacketsController;
import gr.hellenic.client.modules.Module;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class AntiHunger implements Module {

    public static final AntiHunger instance = new AntiHunger();

    private AntiHunger() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (args.packet instanceof ServerboundMovePlayerPacket packet) {
            if (!ConfigStore.instance.getConfig().antiHungerConfig.enabled) {
                return;
            }
            if (NoFall.instance.isActive()) {
                return;
            }
            if (ConfigStore.instance.getConfig().flyHackConfig.enabled) {
                return;
            }
            ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(false);
        }
    }
}