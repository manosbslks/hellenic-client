package gr.hellenic.client.controllers;

import com.mojang.blaze3d.platform.InputConstants;
import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.IKeyBindingRegistry;
import gr.hellenic.client.menu.HellenicMenuScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Opens the in-game Hellenic Client menu (default key: Right Shift, rebindable in Controls). */
public class MenuController {

    public static final MenuController instance = new MenuController();

    private final Minecraft mc = Minecraft.getInstance();
    private final KeyMapping openKey;

    private MenuController() {
        openKey = new KeyMapping(
                "key.hellenicclient.menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT,
                KeyBindingsController.instance.category);

        Events.RegisterKeyBindings.add(this::onRegisterKeyBindings);
        Events.AfterHandleKeyBindings.add(this::onHandleKeyBindings);
    }

    private void onHandleKeyBindings() {
        while (openKey.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new HellenicMenuScreen());
            }
        }
    }

    private void onRegisterKeyBindings(IKeyBindingRegistry registry) {
        registry.register(openKey);
    }
}
