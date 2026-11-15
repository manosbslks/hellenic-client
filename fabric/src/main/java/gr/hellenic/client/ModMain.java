package gr.hellenic.client;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.font.SystemFonts;
import gr.hellenic.client.modules.Modules;
import gr.hellenic.client.modules.utilities.Profiles;
import gr.hellenic.client.utils.DebugScreenExtensions;
import gr.hellenic.client.webui.ConfigHttpServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModMain implements ClientModInitializer {

    public static final String MODID = "hellenicclient";
    public static final Logger LOGGER = LogManager.getLogger(ModMain.class);

    @Override
    public void onInitializeClient() {
        FabricEvents.setup();

        SystemFonts.initAsync();
        Profiles.instance.init();
        ConfigHttpServer.instance.start();
        Modules.registerKeyBindings();
        Modules.register();
        Events.RegisterKeyBindings.trigger(KeyBindingHelper::registerKeyBinding);
        DebugScreenExtensions.register();
    }
}