package gr.hellenic.client.controllers;

import com.mojang.blaze3d.platform.InputConstants;
import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.IKeyBindingRegistry;
import gr.hellenic.client.common.events.RenderGuiEvent;
import gr.hellenic.client.configs.Config;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.ModuleStateProvider;
import gr.hellenic.client.menu.WebModules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-game HUD: "Hellenic Client" watermark (top-left) and the ArrayList of enabled modules (top-right).
 * Toggle with the (unbound by default) "Toggle HUD" key in Controls.
 *
 * Performance: the per-frame render only draws cached sequences. The enabled-module check runs every 10 ticks as plain
 * boolean reads (reflection is resolved once) and the list is rebuilt only when the set of enabled modules changes.
 */
public class HudController {

    public static final HudController instance = new HudController();

    private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hellenicclient", "ui")));
    private static final Style STYLE_BOLD = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hellenicclient", "ui_bold")));
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("hellenicclient", "textures/gui/logo.png");

    private static final int COLOR_A = 0xE879F9;
    private static final int COLOR_B = 0xA855F7;
    private static final int COLOR_C = 0x818CF8;

    private record Entry(FormattedCharSequence text, int width, int color) {}

    /** config field -> module key, resolved once so reflection never runs while playing. */
    private record Source(Field field, String key) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final KeyMapping toggleKey;

    private final List<Source> sources = new ArrayList<>();
    private final Map<String, WebModules.Def> defs = new HashMap<>();
    private final Set<String> activeKeys = new HashSet<>();
    private final Set<String> lastKeys = new HashSet<>();

    private List<Entry> entries = List.of();
    private FormattedCharSequence brand;
    private int brandWidth;
    private boolean enabled = true;
    private int tickCounter;

    private HudController() {
        toggleKey = new KeyMapping(
                "key.hellenicclient.hud",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                KeyBindingsController.instance.category);

        Events.RegisterKeyBindings.add(this::onRegisterKeyBindings);
        Events.AfterHandleKeyBindings.add(this::onHandleKeyBindings);
        Events.ClientTickEnd.add(this::onTick);
        Events.PostRenderGui.add(this::render);
    }

    public boolean isHudEnabled() {
        return enabled;
    }

    public void toggleHud() {
        enabled = !enabled;
        tickCounter = 10; // refresh the list on the next tick when turned back on
    }

    private void onRegisterKeyBindings(IKeyBindingRegistry registry) {
        registry.register(toggleKey);
    }

    private void onHandleKeyBindings() {
        while (toggleKey.consumeClick()) {
            toggleHud();
        }
    }

    private void resolveSources() {
        for (WebModules.Def d : WebModules.ALL) {
            defs.put(d.component(), d);
        }
        for (Field field : Config.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (!ModuleStateProvider.class.isAssignableFrom(type)) {
                continue;
            }
            String key = type.getSimpleName();
            if (key.endsWith("Config")) {
                key = key.substring(0, key.length() - 6);
                if (key.equals("Blocks")) {
                    key = "BlockESP";
                }
                if (key.equals("Entities")) {
                    key = "EntityESP";
                }
            }
            // the cape is cosmetic and on by default, it would just sit in the list forever
            if (defs.containsKey(key) && !key.equals("HellenicCape")) {
                sources.add(new Source(field, key));
            }
        }
    }

    /** Every 10 ticks: boolean checks only; the list is rebuilt just when the enabled set changed. */
    private void onTick() {
        if (!enabled || mc.player == null || ++tickCounter < 10) {
            return;
        }
        tickCounter = 0;
        if (sources.isEmpty()) {
            resolveSources();
        }

        Config config = ConfigStore.instance.getConfig();
        activeKeys.clear();
        for (Source src : sources) {
            try {
                if (((ModuleStateProvider) src.field().get(config)).isEnabled()) {
                    activeKeys.add(src.key());
                }
            } catch (Throwable ignored) {
            }
        }
        if (activeKeys.equals(lastKeys)) {
            return;
        }
        lastKeys.clear();
        lastKeys.addAll(activeKeys);

        Font font = mc.font;
        List<Entry> list = new ArrayList<>(activeKeys.size());
        for (String key : activeKeys) {
            FormattedCharSequence seq = Component.literal(defs.get(key).name()).withStyle(STYLE).getVisualOrderText();
            list.add(new Entry(seq, font.width(seq), 0));
        }
        list.sort(Comparator.comparingInt(Entry::width).reversed());

        // colors depend on the position in the sorted list, so fix them once here instead of every frame
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Entry e = list.get(i);
            list.set(i, new Entry(e.text(), e.width(), 0xFF000000 | gradient(n <= 1 ? 0f : (float) i / (n - 1))));
        }
        entries = list;
    }

    private void render(RenderGuiEvent event) {
        if (!enabled || mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        GuiGraphics g = event.graphics();
        Font font = mc.font;
        int screenW = g.guiWidth();

        // watermark
        if (brand == null) {
            brand = Component.literal("Hellenic Client").withStyle(STYLE_BOLD).getVisualOrderText();
            brandWidth = font.width(brand);
        }
        g.fill(4, 4, 4 + 18 + brandWidth + 8, 4 + 18, 0x90100820);
        g.fill(4, 4, 6, 4 + 18, 0xFF000000 | COLOR_B);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 8, 5, 0f, 0f, 16, 16, 256, 256, 256, 256);
        g.drawString(font, brand, 28, 9, 0xFF000000 | COLOR_A, false);

        // module list
        int rowH = font.lineHeight + 3;
        int y = 4;
        int right = screenW - 4;
        for (Entry e : entries) {
            int left = right - e.width() - 8;
            g.fill(left, y, right, y + rowH, 0x90100820);
            g.fill(right - 2, y, right, y + rowH, e.color());
            g.drawString(font, e.text(), left + 4, y + 2, e.color(), false);
            y += rowH;
        }

    }


    /** t in 0..1 over pink -> violet -> indigo. */
    private static int gradient(float t) {
        int from = t < 0.5f ? COLOR_A : COLOR_B;
        int to = t < 0.5f ? COLOR_B : COLOR_C;
        float k = t < 0.5f ? t * 2f : (t - 0.5f) * 2f;
        int r = (int) (((from >> 16) & 255) * (1 - k) + ((to >> 16) & 255) * k);
        int gr = (int) (((from >> 8) & 255) * (1 - k) + ((to >> 8) & 255) * k);
        int b = (int) ((from & 255) * (1 - k) + (to & 255) * k);
        return (r << 16) | (gr << 8) | b;
    }
}
