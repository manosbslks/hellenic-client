package gr.hellenic.client.controllers;

import com.mojang.blaze3d.platform.InputConstants;
import gr.hellenic.client.ModMain;
import gr.hellenic.client.common.Events;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.KeyBindingsConfig;
import gr.hellenic.client.common.IKeyBindingRegistry;
import gr.hellenic.client.scripting.AsyncRunnable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KeyBindingsController {

    public static final KeyBindingsController instance = new KeyBindingsController();

    public final KeyMapping[] keys;
    public final KeyMapping.Category category;

    private final Minecraft mc = Minecraft.getInstance();

    @SuppressWarnings("unchecked")
    private final Optional<AsyncRunnable>[] actions = new Optional[KeyBindingsConfig.KeysCount];

    @SuppressWarnings("unchecked")
    private final Optional<CompletableFuture<?>>[] futures = new Optional[KeyBindingsConfig.KeysCount];

    private KeyBindingsController() {
        category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ModMain.MODID, "common"));

        keys = new KeyMapping[KeyBindingsConfig.KeysCount];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new KeyMapping("key.hellenicclient.reserved" + i, InputConstants.UNKNOWN.getValue(), category);
        }

        Arrays.setAll(actions, i -> Optional.empty());
        Arrays.setAll(futures, i -> Optional.empty());

        Events.RegisterKeyBindings.add(this::onRegisterKeyBindings);
        Events.AfterHandleKeyBindings.add(this::onHandleKeyBindings);
    }

    public void assign(int index, String name) {
        String[] bindings = ConfigStore.instance.getConfig().keyBindingsConfig.bindings;
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i] != null && bindings[i].equals(name)) {
                actions[i] = Optional.empty();
                bindings[i] = null;
            }
        }

        if (0 <= index && index < KeyBindingsConfig.KeysCount) {
            AsyncRunnable compiled = ScriptsController.instance.get(name);
            if (compiled == null) {
                actions[index] = Optional.empty();
                futures[index] = Optional.empty();
                bindings[index] = null;
            } else {
                actions[index] = Optional.of(compiled);
                futures[index] = Optional.empty();
                bindings[index] = name;
            }
        }
    }

    private void onHandleKeyBindings() {
        if (mc.player == null) {
            return;
        }

        for (int i = 0; i < keys.length; i++) {
            KeyMapping key = keys[i];
            Optional<AsyncRunnable> action = actions[i];
            while (key.consumeClick()) {
                if (action.isPresent()) {
                    Optional<CompletableFuture<?>> future = futures[i];
                    if (future.isEmpty() || future.get().isDone()) {
                        futures[i] = Optional.of(action.get().run());
                    }
                }
            }
        }
    }

    private void onRegisterKeyBindings(IKeyBindingRegistry registry) {
        for (KeyMapping key : keys) {
            registry.register(key);
        }
    }
}