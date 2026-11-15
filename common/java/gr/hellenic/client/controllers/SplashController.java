package gr.hellenic.client.controllers;

import gr.hellenic.client.common.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Short "Hellenic Client" intro shown once, right after the game finished loading. */
public class SplashController {

    public static final SplashController instance = new SplashController();

    private final Minecraft mc = Minecraft.getInstance();
    private boolean shown;

    private SplashController() {
        Events.ClientTickEnd.add(this::onTick);
    }

    private void onTick() {
        if (shown || mc.getOverlay() != null) {
            return;
        }
        shown = true;
        mc.setOverlay(new SplashOverlay());
    }

    private static class SplashOverlay extends Overlay {

        private static final Style BOLD = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hellenicclient", "ui_bold")));
        private static final Identifier LOGO = Identifier.fromNamespaceAndPath("hellenicclient", "textures/gui/logo.png");

        private static final float FADE_IN = 0.55f;
        private static final float HOLD = 1.2f;
        private static final float FADE_OUT = 0.6f;
        private static final float TOTAL = FADE_IN + HOLD + FADE_OUT;

        private final long start = System.nanoTime();

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            float t = (System.nanoTime() - start) / 1_000_000_000f;
            if (t >= TOTAL) {
                mc.setOverlay(null);
                return;
            }

            float alpha;
            if (t < FADE_IN) {
                alpha = ease(t / FADE_IN);
            } else if (t < FADE_IN + HOLD) {
                alpha = 1f;
            } else {
                alpha = 1f - ease((t - FADE_IN - HOLD) / FADE_OUT);
            }

            int w = g.guiWidth();
            int h = g.guiHeight();
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));

            // backdrop
            g.fillGradient(0, 0, w, h, (a << 24) | 0x060610, (a << 24) | 0x1A0B33);

            // logo, slides up slightly while fading in
            float intro = ease(Math.min(1f, t / (FADE_IN + 0.35f)));
            int size = 96;
            int lx = (w - size) / 2;
            int ly = (int) (h / 2f - size / 2f - 14 + (1f - intro) * 10);
            if (a > 4) {
                g.blit(RenderPipelines.GUI_TEXTURED, LOGO, lx, ly, 0f, 0f, size, size, 256, 256, 256, 256, (a << 24) | 0xFFFFFF);
            }

            // title
            Component title = Component.literal("Hellenic Client").withStyle(BOLD);
            int tw = mc.font.width(title);
            float s = 2f;
            g.pose().pushMatrix();
            g.pose().translate((w - tw * s) / 2f, ly + size + 10);
            g.pose().scale(s, s);
            g.drawString(mc.font, title, 0, 0, (a << 24) | 0xE879F9, false);
            g.pose().popMatrix();

            // thin progress line
            int barW = 120;
            int bx = (w - barW) / 2;
            int by = ly + size + 10 + (int) (mc.font.lineHeight * s) + 10;
            g.fill(bx, by, bx + barW, by + 2, ((a / 3) << 24) | 0xA855F7);
            g.fill(bx, by, bx + (int) (barW * Math.min(1f, t / (TOTAL - FADE_OUT))), by + 2, (a << 24) | 0xE879F9);
        }

        private static float ease(float x) {
            x = Math.max(0f, Math.min(1f, x));
            return x * x * (3f - 2f * x);
        }
    }
}
