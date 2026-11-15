package gr.hellenic.client.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.HellenicCapeConfig;
import gr.hellenic.client.mixins.common.accessors.GuiGraphicsAccessor;
import gr.hellenic.client.modules.visuals.HellenicCape;
import gr.hellenic.client.webui.ApiBase;
import gr.hellenic.client.webui.ApiHandler;
import gr.hellenic.client.webui.ConfigHttpServer;
import gr.hellenic.client.webui.ModulesStatusApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-game Hellenic Client menu: a floating neon window (sidebar with icons + card grid with switches), built on the same
 * module list, descriptions, field labels and API objects as the web control panel.
 *
 * Rendering: the UI is laid out in a virtual space (1 unit = u real pixels) and every rectangle is snapped to whole
 * device pixels, so lines stay 1px crisp and corners are clean at any GUI scale / resolution.
 *
 * Performance: nothing runs while closed. Layout is computed on init/resize/page change; every frame draws only the
 * visible cards; text widths, ellipsized strings, Components and wrapped lines are all cached, so a frame allocates
 * almost nothing and costs a few hundred cheap fills.
 */
public class HellenicMenuScreen extends Screen {

    // ---- palette (dark purple + neon magenta) ----
    private static final int GLOW       = 0x30C026D3;
    private static final int PANEL      = 0xFC0A0610;
    private static final int EDGE       = 0xFF3B1A5E;
    private static final int EDGE_HOVER = 0xFF8A35C8;
    private static final int MAG        = 0xFFE040F0;
    private static final int MAG_DK     = 0xFFA21CAF;
    private static final int GREEN      = 0xFF22C55E;
    private static final int TEXT       = 0xFFF8F0FF;
    private static final int TEXT_SOFT  = 0xFFDCCBF0;
    private static final int TEXT_DIM   = 0xFF9C88B8;
    private static final int TEXT_FAINT = 0xFF5F4D7C;
    private static final int TRACK_OFF  = 0xFF2B2338;

    private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hellenicclient", "ui")));
    private static final Style STYLE_BOLD = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hellenicclient", "ui_bold")));
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("hellenicclient", "textures/gui/logo.png");
    private static final Map<String, Identifier> ICONS = new HashMap<>();

    private static final String[] GROUP_ICONS = {"automation", "esp", "hacks", "visuals", "scripting", "utility"};

    private static final int SIDEBAR_W = 126;
    private static final int TOP_H = 40;

    // remembered between openings
    private static String lastSearch = "";
    private static int lastCategory = 0;
    private static float lastScroll = 0;
    private static Map<String, Meta> metaCache;

    private enum View { HOME, MODULE, CREDITS, SHOTS, ACTIVE, CAPES }

    // ---- capes tab ----
    private static final int CAPE_TOP = 100;
    private int capeCols, capeW, capeImgH, capeRowH;
    private String capeNote;
    private long capeNoteAt;

    // ---- active modules tab ----
    private static final int ACTIVE_TOP = 100;
    private final List<WebModules.Def> active = new ArrayList<>();

    // ---- screenshot manager ----
    private static final int SHOT_LIMIT = 30;
    private static final int SHOT_TOP = 100;
    private static final AtomicInteger SHOT_IDS = new AtomicInteger();

    private static final class Shot {
        File file;
        String name;
        Identifier tex;
        int tw, th;
        boolean failed;
    }

    private final List<Shot> shots = new ArrayList<>();
    private int shotCols, shotW, shotImgH, shotRowH;
    private String shotNote;
    private long shotNoteAt;

    private static final String[] CREDIT_NAMES = {"basilakos1", "kotaidis", "gourounaki12", "gls"};
    private static final String DISCORD_URL = "https://discord.gg/tx3Z54Qmkt";
    private static final int[] CREDIT_COLORS = {0xFFE040F0, 0xFF8B5CF6, 0xFF38BDF8, 0xFF34D399};

    private record Meta(String desc, List<String[]> labels) {}

    private static final class Field {
        String value = "";
        int max = 48;
        boolean focused;

        void type(String s) {
            if (value.length() + s.length() <= max) {
                value += s;
            }
        }

        void backspace() {
            if (!value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }
        }
    }

    private View view = View.HOME;
    private WebModules.Def current;
    private int category = lastCategory; // 0 = all, 1.. = groups

    private final Field search = new Field();
    private final Field editor = new Field();
    private int editingRow = -1;

    private float scroll;
    private float scrollTarget;
    private float contentH;

    private final Map<String, Boolean> statuses = new HashMap<>();
    private final Map<String, Float> switchAnim = new HashMap<>();
    private final long openedAt = System.currentTimeMillis();
    private int activeTotal;

    // ---- caches (a frame must not allocate or re-measure text) ----
    private final Map<String, FormattedCharSequence> seqN = new HashMap<>();
    private final Map<String, FormattedCharSequence> seqB = new HashMap<>();
    private final Map<String, Integer> widthN = new HashMap<>();
    private final Map<String, Integer> widthB = new HashMap<>();
    private final Map<String, String> fitCache = new HashMap<>();

    // ---- window layout (virtual units) ----
    private double gs = 1.0;     // gui scale
    private float u = 1f;        // real pixels per virtual unit
    private float us = 1f;       // u / gs (virtual -> gui coords)
    private int bw = 1;          // border width in real pixels
    private int px, py, pw, ph;
    private int mainX, mainW;
    private int vx, vy, vw, vh;
    private int searchX, searchW;

    private final List<WebModules.Def> shown = new ArrayList<>();
    private int cols, cardW;
    private static final int CARD_H = 40, GAP = 8;
    private final int[] groupCounts = new int[WebModules.GROUPS.length];
    private String clockText = "";
    private long clockSecond = -1;
    private String hello = "";

    private WebModules.Def tipDef;
    private WebModules.Def tipFor;
    private List<FormattedCharSequence> tipLines = List.of();

    // ---- module page ----
    private static final int K_BOOL = 1, K_VALUE = 2, K_NOTE = 3, K_SECTION = 4;

    private static final class Row {
        int kind;
        String label;
        JsonObject owner;
        String key;
        int y;
    }

    private static final class Leaf {
        JsonObject owner;
        String key;
    }

    private ApiBase api;
    private JsonObject json;
    private final List<Row> rows = new ArrayList<>();
    private String pageMessage;
    private List<FormattedCharSequence> descLines = List.of();
    private int rowsTop;

    public HellenicMenuScreen() {
        super(Component.literal("Hellenic Client"));
    }

    // ============================================================ batched drawing
    //
    // 1.21.11's GuiRenderState checks every submitted GUI element against the ones submitted before it (to find the
    // layer it belongs to), so the cost grows with the square of the element count. The menu used to submit several
    // hundred rectangles a frame (a rounded panel is 3-8 of them) and that cost ~30% of the frame while it was open.
    // Now every rectangle of a layer goes into ONE element (QuadBatch), and the layer's text and icons are drawn right
    // after it, so the stacking is the same as before: rectangles below, text/icons on top, next layer above that.

    /** Many colored quads in one GUI element, vertices exactly like GuiGraphics.fill / fillGradient. */
    private record QuadBatch(int[] q, int count, Matrix3x2f pose, ScreenRectangle scissorArea, ScreenRectangle bounds)
            implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int i = 0, o = 0; i < count; i++, o += 6) {
                float x0 = q[o], y0 = q[o + 1], x1 = q[o + 2], y1 = q[o + 3];
                int top = q[o + 4], bottom = q[o + 5];
                vc.addVertexWith2DPose(pose, x0, y0).setColor(top);
                vc.addVertexWith2DPose(pose, x0, y1).setColor(bottom);
                vc.addVertexWith2DPose(pose, x1, y1).setColor(bottom);
                vc.addVertexWith2DPose(pose, x1, y0).setColor(top);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    /** Rectangles + deferred text/blits of one layer; arrays are reused between frames (no per-frame garbage). */
    private static final class Layer {
        int[] q = new int[6 * 512];
        int n;
        int minX, minY, maxX, maxY;

        int ops;
        boolean[] isText = new boolean[256];
        FormattedCharSequence[] seq = new FormattedCharSequence[256];
        Identifier[] tex = new Identifier[256];
        int[] oi = new int[256 * 9];     // x, y, w, h, regionW, regionH, texW, texH, color
        float[] of = new float[256 * 3]; // u, v, text scale

        void clear() {
            n = 0;
            ops = 0;
            minX = minY = Integer.MAX_VALUE;
            maxX = maxY = Integer.MIN_VALUE;
        }

        void rect(int x0, int y0, int x1, int y1, int top, int bottom) {
            if (x0 == x1 || y0 == y1) {
                return;
            }
            if (n * 6 + 6 > q.length) {
                q = Arrays.copyOf(q, q.length * 2);
            }
            int o = n * 6;
            q[o] = x0;
            q[o + 1] = y0;
            q[o + 2] = x1;
            q[o + 3] = y1;
            q[o + 4] = top;
            q[o + 5] = bottom;
            n++;
            minX = Math.min(minX, Math.min(x0, x1));
            minY = Math.min(minY, Math.min(y0, y1));
            maxX = Math.max(maxX, Math.max(x0, x1));
            maxY = Math.max(maxY, Math.max(y0, y1));
        }

        private int nextOp() {
            if (ops == isText.length) {
                int cap = ops * 2;
                isText = Arrays.copyOf(isText, cap);
                seq = Arrays.copyOf(seq, cap);
                tex = Arrays.copyOf(tex, cap);
                oi = Arrays.copyOf(oi, cap * 9);
                of = Arrays.copyOf(of, cap * 3);
            }
            return ops++;
        }

        void text(FormattedCharSequence s, int x, int y, int color, float scale) {
            int i = nextOp();
            isText[i] = true;
            seq[i] = s;
            tex[i] = null;
            oi[i * 9] = x;
            oi[i * 9 + 1] = y;
            oi[i * 9 + 8] = color;
            of[i * 3 + 2] = scale;
        }

        void blit(Identifier id, int x, int y, float u, float v, int w, int h, int regionW, int regionH, int texW, int texH, int color) {
            int i = nextOp();
            isText[i] = false;
            seq[i] = null;
            tex[i] = id;
            int b = i * 9;
            oi[b] = x;
            oi[b + 1] = y;
            oi[b + 2] = w;
            oi[b + 3] = h;
            oi[b + 4] = regionW;
            oi[b + 5] = regionH;
            oi[b + 6] = texW;
            oi[b + 7] = texH;
            oi[b + 8] = color;
            of[i * 3] = u;
            of[i * 3 + 1] = v;
        }
    }

    private final Layer chrome = new Layer();   // window, sidebar, top bar
    private final Layer content = new Layer();  // the scrolled page, clipped to the content area
    private final Layer overlay = new Layer();  // scrollbar + tooltip, above everything
    private Layer cur = chrome;

    /** Submits the layer: its rectangles as one element, then its text and icons in the order they were drawn. */
    private void flush(GuiGraphics g, Layer l, boolean clipToContent) {
        int sx0 = r(mainX), sy0 = r(vy), sx1 = r(mainX + mainW - 1), sy1 = r(vy + vh);
        if (clipToContent) {
            g.enableScissor(sx0, sy0, sx1, sy1);
        }
        if (l.n > 0) {
            Matrix3x2f pose = new Matrix3x2f(g.pose());
            ScreenRectangle scissor = clipToContent
                    ? new ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0).transformAxisAligned(pose) : null;
            ScreenRectangle bounds = new ScreenRectangle(l.minX, l.minY, l.maxX - l.minX, l.maxY - l.minY).transformMaxBounds(pose);
            if (scissor != null) {
                bounds = scissor.intersection(bounds);
            }
            if (bounds != null) {
                ((GuiGraphicsAccessor) g).getGuiRenderState_CU()
                        .submitGuiElement(new QuadBatch(Arrays.copyOf(l.q, l.n * 6), l.n, pose, scissor, bounds));
            }
        }
        for (int i = 0; i < l.ops; i++) {
            int b = i * 9;
            if (l.isText[i]) {
                float s = l.of[i * 3 + 2];
                g.pose().pushMatrix();
                g.pose().translate(l.oi[b], l.oi[b + 1]);
                g.pose().scale(s, s);
                g.drawString(font, l.seq[i], 0, 0, l.oi[b + 8], false);
                g.pose().popMatrix();
            } else {
                g.blit(RenderPipelines.GUI_TEXTURED, l.tex[i], l.oi[b], l.oi[b + 1], l.of[i * 3], l.of[i * 3 + 1],
                        l.oi[b + 2], l.oi[b + 3], l.oi[b + 4], l.oi[b + 5], l.oi[b + 6], l.oi[b + 7], l.oi[b + 8]);
            }
        }
        if (clipToContent) {
            g.disableScissor();
        }
    }

    // ============================================================ pixel / text / icon helpers

    /** virtual units -> whole device pixels */
    private int r(float v) {
        return Math.round(v * u);
    }

    private void fill(GuiGraphics g, int x, int y, int x2, int y2, int color) {
        cur.rect(r(x), r(y), r(x2), r(y2), color, color);
    }

    private void grad(GuiGraphics g, int x, int y, int x2, int y2, int top, int bottom) {
        cur.rect(r(x), r(y), r(x2), r(y2), top, bottom);
    }

    private FormattedCharSequence seq(String s, boolean b) {
        Map<String, FormattedCharSequence> m = b ? seqB : seqN;
        FormattedCharSequence c = m.get(s);
        if (c == null) {
            if (m.size() > 1500) {
                m.clear();
            }
            c = Component.literal(s).withStyle(b ? STYLE_BOLD : STYLE).getVisualOrderText();
            m.put(s, c);
        }
        return c;
    }

    private int tw0(String s, boolean b) {
        Map<String, Integer> m = b ? widthB : widthN;
        Integer w = m.get(s);
        if (w == null) {
            if (m.size() > 3000) {
                m.clear();
            }
            w = font.width(seq(s, b));
            m.put(s, w);
        }
        return w;
    }

    private int tw(String s) {
        return tw0(s, false);
    }

    private int twb(String s) {
        return tw0(s, true);
    }

    private void drawSeq(GuiGraphics g, FormattedCharSequence q, int x, int y, int color, float scale) {
        cur.text(q, r(x), r(y), color, u * scale);
    }

    private void s(GuiGraphics g, String text, int x, int y, int color) {
        drawSeq(g, seq(text, false), x, y, color, 1f);
    }

    private void sb(GuiGraphics g, String text, int x, int y, int color) {
        drawSeq(g, seq(text, true), x, y, color, 1f);
    }

    private void cs(GuiGraphics g, String text, int cx, int y, int color, boolean b) {
        drawSeq(g, seq(text, b), cx - tw0(text, b) / 2, y, color, 1f);
    }

    /** Ellipsizes to maxW; results are cached so this is a map lookup after the first frame. */
    private String fit(String text, int maxW) {
        String key = maxW + "|" + text;
        String cached = fitCache.get(key);
        if (cached != null) {
            return cached;
        }
        String out = text;
        if (tw(text) > maxW) {
            int lo = 1, hi = text.length();
            while (lo < hi) { // binary search for the longest prefix that fits with "..."
                int mid = (lo + hi + 1) >>> 1;
                if (tw(text.substring(0, mid) + "...") <= maxW) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            out = text.substring(0, lo) + "...";
        }
        if (fitCache.size() > 4000) {
            fitCache.clear();
        }
        fitCache.put(key, out);
        return out;
    }

    private void icon(GuiGraphics g, String name, int x, int y, int size, int color) {
        Identifier id = ICONS.get(name);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath("hellenicclient", "textures/gui/icons/" + name + ".png");
            ICONS.put(name, id);
        }
        int s = r(size);
        cur.blit(id, r(x), r(y), 0f, 0f, s, s, 128, 128, 128, 128, color);
    }

    private void logo(GuiGraphics g, int x, int y, int size) {
        int s = r(size);
        cur.blit(LOGO, r(x), r(y), 0f, 0f, s, s, 256, 256, 256, 256, 0xFFFFFFFF);
    }

    /** Filled shape with softly cut (two-step) corners, snapped to device pixels. cut in virtual units. */
    private void rpanel(GuiGraphics g, int x, int y, int w, int h, float cut, int color) {
        int X = r(x), Y = r(y), W = r(x + w) - X, H = r(y + h) - Y;
        int c = Math.max(1, Math.round(cut * u));
        int c2 = c / 2;
        cur.rect(X + c, Y, X + W - c, Y + H, color, color);
        if (c2 > 0) {
            cur.rect(X + c2, Y + c2, X + W - c2, Y + H - c2, color, color);
        }
        cur.rect(X, Y + c, X + W, Y + H - c, color, color);
    }

    /** Rounded border with a vertical gradient fill (6 fills). */
    private void gradPanel(GuiGraphics g, int x, int y, int w, int h, int border, int top, int bottom) {
        float cut = 2.2f;
        rpanel(g, x, y, w, h, cut, border);
        int X = r(x) + bw, Y = r(y) + bw, W = r(x + w) - r(x) - 2 * bw, H = r(y + h) - r(y) - 2 * bw;
        int c = Math.max(1, Math.round(cut * u) - bw), c2 = c / 2;
        // inner shape in three gradient strips, each with its own slice of the gradient so it reads as one fill
        cur.rect(X + c, Y, X + W - c, Y + H, top, bottom);
        if (c2 > 0) {
            cur.rect(X + c2, Y + c2, X + c, Y + H - c2, mix(top, bottom, (float) c2 / H), mix(top, bottom, 1f - (float) c2 / H));
            cur.rect(X + W - c, Y + c2, X + W - c2, Y + H - c2, mix(top, bottom, (float) c2 / H), mix(top, bottom, 1f - (float) c2 / H));
        }
        cur.rect(X, Y + c, X + c2 + 0, Y + H - c, mix(top, bottom, (float) c / H), mix(top, bottom, 1f - (float) c / H));
        cur.rect(X + W - c2, Y + c, X + W, Y + H - c, mix(top, bottom, (float) c / H), mix(top, bottom, 1f - (float) c / H));
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ============================================================ meta

    private static synchronized Map<String, Meta> meta() {
        if (metaCache == null) {
            Map<String, Meta> map = new HashMap<>();
            try (InputStream in = HellenicMenuScreen.class.getResourceAsStream("/assets/hellenicclient/menu/meta.json")) {
                if (in != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                        JsonObject o = e.getValue().getAsJsonObject();
                        List<String[]> labels = new ArrayList<>();
                        for (JsonElement l : o.getAsJsonArray("labels")) {
                            labels.add(new String[]{l.getAsJsonArray().get(0).getAsString(), l.getAsJsonArray().get(1).getAsString()});
                        }
                        map.put(e.getKey(), new Meta(o.get("desc").getAsString(), labels));
                    }
                }
            } catch (Throwable ignored) {
            }
            metaCache = map;
        }
        return metaCache;
    }

    // ============================================================ init / layout

    @Override
    protected void init() {
        // Lay out in a virtual space where 1 unit = u real pixels (u >= 1), snapped to device pixels when drawn.
        gs = minecraft.getWindow().getGuiScale();
        int rw = minecraft.getWindow().getWidth(), rh = minecraft.getWindow().getHeight();
        u = (float) Math.max(1.0, Math.min(rw / 780.0, rh / 460.0));
        us = (float) (u / gs);
        bw = u >= 2.6f ? 2 : 1;
        int sw = Math.round(rw / u), sh = Math.round(rh / u);

        pw = Math.min(sw - 16, 720);
        ph = Math.min(sh - 16, 420);
        px = (sw - pw) / 2;
        py = (sh - ph) / 2;
        mainX = px + SIDEBAR_W;
        mainW = pw - SIDEBAR_W;
        vx = mainX + 14;
        vw = mainW - 14 - 20;
        vy = py + TOP_H;
        vh = ph - TOP_H - 8;
        searchW = Math.min(160, mainW / 3);
        searchX = mainX + mainW - searchW - 14;

        fitCache.clear();
        search.value = lastSearch;
        hello = "Hello, " + minecraft.getUser().getName();
        for (int i = 0; i < groupCounts.length; i++) {
            int n = 0;
            for (WebModules.Def d : WebModules.ALL) {
                if (d.group().equals(WebModules.GROUPS[i][0])) {
                    n++;
                }
            }
            groupCounts[i] = n;
        }

        refreshStatuses();
        rebuildList();
        if (view == View.MODULE && current != null) {
            openModule(current, true);
        } else if (view == View.SHOTS) {
            layoutShots();
        } else if (view == View.ACTIVE) {
            buildActive();
        } else if (view == View.CAPES) {
            layoutCapes();
        } else {
            scroll = scrollTarget = Math.min(lastScroll, maxScroll());
        }
    }

    private void refreshStatuses() {
        try {
            JsonObject o = JsonParser.parseString(new ModulesStatusApi().get()).getAsJsonObject();
            statuses.clear();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                statuses.put(e.getKey(), e.getValue().getAsBoolean());
            }
        } catch (Throwable ignored) {
        }
        activeTotal = 0;
        for (boolean b : statuses.values()) {
            if (b) {
                activeTotal++;
            }
        }
    }

    /** Rebuilds the visible card list (category + search). */
    private void rebuildList() {
        shown.clear();
        String[] words = search.value.trim().toLowerCase(Locale.ROOT).split(" +");
        String group = category == 0 ? null : WebModules.GROUPS[category - 1][0];
        for (WebModules.Def d : WebModules.ALL) {
            if (group != null && !d.group().equals(group)) {
                continue;
            }
            String hay = (d.name() + " " + d.group() + " " + d.path()).toLowerCase(Locale.ROOT).replace('-', ' ');
            boolean ok = true;
            for (String w : words) {
                if (!w.isEmpty() && !hay.contains(w)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                shown.add(d);
            }
        }
        cols = vw >= 380 ? 2 : 1;
        cardW = (vw - GAP * (cols - 1)) / cols;
        int rowsN = (shown.size() + cols - 1) / cols;
        if (view == View.HOME) {
            contentH = rowsN * (CARD_H + GAP) + 4;
        }
        scroll = scrollTarget = 0;
    }

    private float maxScroll() {
        return Math.max(0, contentH - vh);
    }

    private void clampScroll() {
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    // ============================================================ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        scroll += (scrollTarget - scroll) * Math.min(1f, delta * 0.5f);
        if (Math.abs(scrollTarget - scroll) < 0.05f) {
            scroll = scrollTarget;
        }

        // open animation: dim fades in, window eases up into place
        float ot = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
        float ease = 1f - (1f - ot) * (1f - ot) * (1f - ot);
        g.fill(0, 0, width, height, ((int) (0xB0 * ease) << 24));
        mx = Math.round(mx / us);
        my = Math.round(my / us);

        // from here on 1 gui unit == 1 real pixel; all geometry goes through r()
        g.pose().pushMatrix();
        g.pose().scale((float) (1.0 / gs), (float) (1.0 / gs));
        g.pose().translate(0f, (1f - ease) * 14f * u);

        chrome.clear();
        content.clear();
        overlay.clear();
        cur = chrome;

        // window: soft neon glow + border
        for (int i = 3; i >= 1; i--) {
            rpanel(g, px - i * 2, py - i * 2, pw + i * 4, ph + i * 4, 5f, ((0x0A + (3 - i) * 0x08) << 24) | 0xC026D3);
        }
        rpanel(g, px - 1, py - 1, pw + 2, ph + 2, 4f, 0xFF7A2FBF);
        rpanel(g, px, py, pw, ph, 4f, PANEL);
        grad(g, mainX, py + 4, px + pw - 3, py + 80, GLOW, 0x00C026D3);

        tipDef = null;
        renderSidebar(g, mx, my);
        renderTopBar(g, mx, my);

        cur = content;
        if (view == View.HOME) {
            renderCards(g, mx, my);
        } else if (view == View.CREDITS) {
            renderCredits(g, mx, my);
        } else if (view == View.SHOTS) {
            renderShots(g, mx, my);
        } else if (view == View.ACTIVE) {
            renderActive(g, mx, my);
        } else if (view == View.CAPES) {
            renderCapes(g, mx, my);
        } else {
            renderModule(g, mx, my);
        }
        cur = overlay;
        renderScrollbar(g);
        if (tipDef != null) {
            renderTooltip(g, mx, my);
        }

        flush(g, chrome, false);
        flush(g, content, true);
        flush(g, overlay, false);
        cur = chrome;
        g.pose().popMatrix();
    }

    private void renderSidebar(GuiGraphics g, int mx, int my) {
        grad(g, px + 3, py + 4, px + SIDEBAR_W, py + ph - 4, 0xFF150A24, 0xFF0B0614);
        fill(g, px + 1, py + 8, px + 3, py + ph - 8, 0xFF150A24);
        fill(g, px + SIDEBAR_W, py + 6, px + SIDEBAR_W + 1, py + ph - 6, EDGE);

        logo(g, px + 12, py + 12, 28);
        sb(g, "HELLENIC", px + 46, py + 13, TEXT);
        s(g, "Client", px + 46, py + 26, MAG);

        s(g, "MODULES", px + 14, py + 52, TEXT_FAINT);
        int iy = py + 66;
        for (int i = 0; i <= WebModules.GROUPS.length; i++) {
            String name = i == 0 ? "All" : WebModules.GROUPS[i - 1][1];
            String count = Integer.toString(i == 0 ? WebModules.ALL.size() : groupCounts[i - 1]);
            sideItem(g, mx, my, iy, name, count, i == 0 ? "all" : GROUP_ICONS[i - 1], i == category && view == View.HOME);
            iy += 24;
        }

        iy += 6;
        s(g, "GENERAL", px + 14, iy, TEXT_FAINT);
        iy += 14;
        sideItem(g, mx, my, iy, "Web panel", null, "web", false);
        iy += 24;
        // short label: "Active Modules" ran under its count badge
        sideItem(g, mx, my, iy, "Active", Integer.toString(activeTotal), "all", view == View.ACTIVE);
        iy += 24;
        sideItem(g, mx, my, iy, "Capes", null, "cape", view == View.CAPES);
        iy += 24;
        sideItem(g, mx, my, iy, "Screenshots", null, "visuals", view == View.SHOTS);
        iy += 24;
        sideItem(g, mx, my, iy, "Credits", null, "info", view == View.CREDITS);
        iy += 24;
        sideItem(g, mx, my, iy, "Close", null, "close", false);
        iy += 24;

        int cy = py + ph - 48;
        if (cy > iy - 2) {
            gradPanel(g, px + 8, cy, SIDEBAR_W - 16, 40, 0xFF3B1A5E, 0xFF1B0D30, 0xFF120820);
            logo(g, px + 14, cy + 6, 28);
            sb(g, "Hellenic", px + 48, cy + 9, TEXT);
            s(g, fit("by basilakos1", SIDEBAR_W - 16 - 44), px + 48, cy + 22, TEXT_DIM);
        }
    }

    private void sideItem(GuiGraphics g, int mx, int my, int y, String name, String count, String icon, boolean selected) {
        int x = px + 8, w = SIDEBAR_W - 16;
        boolean hover = in(mx, my, x, y, w, 22);
        if (selected) {
            rpanel(g, x - 1, y - 1, w + 2, 24, 3f, 0x30C026D3);
            gradPanel(g, x, y, w, 22, MAG_DK, 0xFF4A1470, 0xFF2D0B4A);
        } else if (hover) {
            gradPanel(g, x, y, w, 22, 0xFF4A2370, 0xFF241238, 0xFF190C28);
        }
        icon(g, icon, x + 8, y + 5, 12, selected ? 0xFFFFFFFF : (hover ? TEXT : TEXT_DIM));
        s(g, name, x + 26, y + 6, selected || hover ? TEXT : TEXT_SOFT);
        if (count != null) {
            int cw = tw(count) + 10;
            int cx = x + w - cw - 6;
            gradPanel(g, cx, y + 4, cw, 14, selected ? 0xFFB033D0 : 0xFF2A1740, selected ? 0xFF6A1F95 : 0xFF1A0E2A, selected ? 0xFF6A1F95 : 0xFF1A0E2A);
            s(g, count, cx + 5, y + 6, selected ? TEXT : TEXT_DIM);
        }
    }

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        if (view == View.HOME) {
            long sec = System.currentTimeMillis() / 1000;
            if (sec != clockSecond) {
                clockSecond = sec;
                LocalTime now = LocalTime.now();
                clockText = String.format("%02d:%02d", now.getHour(), now.getMinute());
            }
            int avail = searchX - (mainX + 14) - 14;
            String act = activeTotal + " active";
            int x0 = mainX + 14;
            sb(g, fit(hello, avail), x0, py + 15, TEXT);
            int x1 = x0 + twb(hello) + 14;
            if (x1 + twb(clockText) <= searchX - 14) {
                sb(g, clockText, x1, py + 15, MAG);
                int x2 = x1 + twb(clockText) + 14;
                if (x2 + twb(act) <= searchX - 14) {
                    sb(g, act, x2, py + 15, GREEN);
                }
            }

            gradPanel(g, searchX, py + 9, searchW, 24, search.focused ? MAG : EDGE, 0xFF0F0818, 0xFF0A0610);
            icon(g, "search", searchX + 8, py + 15, 11, TEXT_DIM);
            if (search.value.isEmpty() && !search.focused) {
                s(g, "Search modules...", searchX + 24, py + 16, TEXT_FAINT);
            } else {
                String fitted = fit(search.value, searchW - 70);
                s(g, fitted, searchX + 24, py + 16, TEXT);
                if (search.focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                    int cx = searchX + 25 + tw(fitted);
                    fill(g, cx, py + 14, cx + 1, py + 28, TEXT);
                }
            }
            if (search.value.isEmpty()) {
                String k = "Ctrl K";
                int kw = tw(k) + 10;
                gradPanel(g, searchX + searchW - kw - 5, py + 14, kw, 15, EDGE, 0xFF1A0E2A, 0xFF1A0E2A);
                s(g, k, searchX + searchW - kw, py + 16, TEXT_DIM);
            }
        } else {
            boolean hover = in(mx, my, mainX + 14, py + 10, 100, 20);
            s(g, "<  All modules", mainX + 14, py + 15, hover ? TEXT : TEXT_DIM);
        }
    }

    // ---- cards ----

    private void renderCards(GuiGraphics g, int mx, int my) {
        if (shown.isEmpty()) {
            cs(g, "No modules found", vx + vw / 2, vy + 40, TEXT_DIM, false);
            return;
        }
        int step = CARD_H + GAP;
        int firstRow = Math.max(0, (int) (scroll / step));
        int lastRow = Math.min((shown.size() + cols - 1) / cols - 1, (int) ((scroll + vh) / step));
        for (int r = firstRow; r <= lastRow; r++) {
            int cy = vy + 4 + r * step - (int) scroll;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= shown.size()) {
                    break;
                }
                drawCard(g, shown.get(idx), vx + c * (cardW + GAP), cy, mx, my);
            }
        }
    }

    private void drawCard(GuiGraphics g, WebModules.Def d, int x, int y, int mx, int my) {
        Boolean st = statuses.get(d.component());
        boolean toggleable = st != null;
        boolean on = toggleable && st;
        boolean hover = in(mx, my, x, y, cardW, CARD_H) && in(mx, my, mainX, vy, mainW, vh);

        // smooth switch animation
        float a = switchAnim.getOrDefault(d.component(), on ? 1f : 0f);
        float target = on ? 1f : 0f;
        if (a != target) {
            a += (target - a) * 0.3f;
            if (Math.abs(target - a) < 0.02f) {
                a = target;
            }
            switchAnim.put(d.component(), a);
        }

        if (on) {
            rpanel(g, x - 2, y - 2, cardW + 4, CARD_H + 4, 4f, 0x18D946EF);
            rpanel(g, x - 1, y - 1, cardW + 2, CARD_H + 2, 3.2f, 0x34D946EF);
        }
        gradPanel(g, x, y, cardW, CARD_H,
                on ? mix(0xFF9A2BC0, MAG, a) : (hover ? EDGE_HOVER : EDGE),
                on ? 0xFF33105A : (hover ? 0xFF251442 : 0xFF1A0D30),
                on ? 0xFF1C0A36 : (hover ? 0xFF180C2C : 0xFF120820));

        int rightPad = toggleable ? 60 : 32;
        String name = fit(d.name(), cardW - 40 - rightPad);
        sb(g, name, x + 14, y + 8, TEXT);

        // (i) info icon next to the name: hover to read the full description
        int ix = x + 14 + twb(name) + 6, iy = y + 8;
        boolean infoHover = in(mx, my, ix - 2, iy - 2, 15, 15) && in(mx, my, mainX, vy, mainW, vh);
        icon(g, "info", ix, iy, 11, infoHover ? MAG : TEXT_FAINT);
        if (infoHover) {
            tipDef = d;
        }

        Meta m = meta().get(d.component());
        String sub = m != null && !m.desc().isEmpty() ? m.desc() : d.group().substring(0, 1).toUpperCase(Locale.ROOT) + d.group().substring(1);
        s(g, fit(sub, cardW - 22 - rightPad), x + 14, y + 22, on ? TEXT_SOFT : TEXT_DIM);

        if (toggleable) {
            drawSwitch(g, x + cardW - 44, y + (CARD_H - 16) / 2, a);
        } else {
            sb(g, ">", x + cardW - 20, y + 14, hover ? TEXT : TEXT_FAINT);
        }
    }

    private void drawSwitch(GuiGraphics g, int sx, int sy, float a) {
        int sw = 32, sh = 16;
        if (a > 0.5f) {
            rpanel(g, sx - 2, sy - 2, sw + 4, sh + 4, 4f, 0x40E040F0);
        }
        rpanel(g, sx, sy, sw, sh, 4f, mix(0xFF3A3050, MAG, a));
        rpanel(g, sx + 1, sy + 1, sw - 2, sh - 2, 3.4f, mix(TRACK_OFF, 0xFFB01CC4, a));
        int kx = sx + 3 + Math.round((sw - sh) * a);
        rpanel(g, kx, sy + 3, sh - 6, sh - 6, 3f, 0xFFFFFFFF);
    }

    private void renderTooltip(GuiGraphics g, int mx, int my) {
        WebModules.Def d = tipDef;
        Meta m = meta().get(d.component());
        if (m == null || m.desc().isEmpty()) {
            return;
        }
        if (tipFor != d) {
            tipFor = d;
            tipLines = font.split(Component.literal(m.desc()).withStyle(STYLE), 230);
        }
        int lh = 12, w = twb(d.name());
        for (FormattedCharSequence l : tipLines) {
            w = Math.max(w, font.width(l));
        }
        int bw2 = w + 22, bh = 12 + 16 + tipLines.size() * lh + 8;
        int x = mx + 12, y = my + 14;
        if (x + bw2 > px + pw) {
            x = mx - bw2 - 8;
        }
        if (y + bh > py + ph) {
            y = py + ph - bh - 4;
        }
        gradPanel(g, x, y, bw2, bh, MAG_DK, 0xFF140A22, 0xFF0F0818);
        sb(g, d.name(), x + 11, y + 9, TEXT);
        for (int i = 0; i < tipLines.size(); i++) {
            drawSeq(g, tipLines.get(i), x + 11, y + 26 + i * lh, TEXT_DIM, 1f);
        }
    }

    // ---- module page ----

    private void renderModule(GuiGraphics g, int mx, int my) {
        int ox = vx, oy = vy - (int) scroll;

        String kicker = current.group().toUpperCase(Locale.ROOT);
        int kw = twb(kicker) + 28;
        rpanel(g, ox, oy + 6, kw, 18, 3f, 0xFF2A1040);
        fill(g, ox + 9, oy + 13, ox + 14, oy + 18, MAG);
        sb(g, kicker, ox + 20, oy + 10, MAG);

        drawSeq(g, seq(current.name(), true), ox, oy + 32, TEXT, 1.9f);

        String web = "Open in web panel";
        int bw2 = tw(web) + 36, bx = ox + vw - bw2, by = oy + 36;
        boolean webHover = in(mx, my, bx, by, bw2, 22);
        gradPanel(g, bx, by, bw2, 22, webHover ? MAG : EDGE, webHover ? 0xFF3A1760 : 0xFF1B0D30, 0xFF120820);
        icon(g, "web", bx + 9, by + 5, 12, webHover ? TEXT : TEXT_DIM);
        s(g, web, bx + 26, by + 7, TEXT);

        for (int i = 0; i < descLines.size(); i++) {
            drawSeq(g, descLines.get(i), ox, oy + 72 + i * 13, TEXT_DIM, 1f);
        }
        if (pageMessage != null) {
            s(g, pageMessage, ox, oy + rowsTop, TEXT_DIM);
        }

        float top = scroll - 40, bottom = scroll + vh + 10;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.y + 30 < top || r.y > bottom) {
                continue;
            }
            int ry = oy + r.y;
            switch (r.kind) {
                case K_SECTION -> {
                    sb(g, r.label.toUpperCase(Locale.ROOT), ox + 2, ry + 12, MAG);
                    fill(g, ox, ry + 28, ox + vw, ry + 29, EDGE);
                }
                case K_BOOL -> {
                    boolean on = r.owner.get(r.key).getAsBoolean();
                    boolean hover = in(mx, my, ox, ry, vw, 30) && in(mx, my, mainX, vy, mainW, vh);
                    gradPanel(g, ox, ry, vw, 30, on ? 0xFF9A2BC0 : (hover ? EDGE_HOVER : EDGE),
                            on ? 0xFF33105A : (hover ? 0xFF251442 : 0xFF1A0D30), on ? 0xFF1C0A36 : 0xFF120820);
                    s(g, fit(r.label, vw - 120), ox + 14, ry + 9, on ? TEXT : TEXT_SOFT);
                    String stt = on ? "ON" : "OFF";
                    sb(g, stt, ox + vw - 44 - 10 - twb(stt), ry + 9, on ? GREEN : TEXT_FAINT);
                    drawSwitch(g, ox + vw - 44, ry + 7, on ? 1f : 0f);
                }
                case K_VALUE -> {
                    boolean hover = in(mx, my, ox, ry, vw, 30) && in(mx, my, mainX, vy, mainW, vh);
                    gradPanel(g, ox, ry, vw, 30, hover ? EDGE_HOVER : EDGE, hover ? 0xFF251442 : 0xFF1A0D30, 0xFF120820);
                    s(g, fit(r.label, vw - 190), ox + 14, ry + 9, TEXT_SOFT);
                    int fw = 140, fx = ox + vw - fw - 8;
                    if (i == editingRow) {
                        gradPanel(g, fx, ry + 4, fw, 22, MAG, 0xFF0F0818, 0xFF0A0610);
                        String fitted = fit(editor.value, fw - 18);
                        s(g, fitted, fx + 8, ry + 10, TEXT);
                        if ((System.currentTimeMillis() / 500) % 2 == 0) {
                            int cx = fx + 9 + tw(fitted);
                            fill(g, cx, ry + 8, cx + 1, ry + 22, TEXT);
                        }
                    } else {
                        JsonElement v = r.owner.get(r.key);
                        String val = v == null || v.isJsonNull() ? "" : v.getAsString();
                        gradPanel(g, fx, ry + 4, fw, 22, EDGE, 0xFF0F0818, 0xFF0A0610);
                        s(g, fit(val, fw - 18), fx + 8, ry + 10, TEXT);
                    }
                }
                default -> s(g, r.label + "   (edit in the web panel)", ox + 14, ry + 9, TEXT_FAINT);
            }
        }
    }

    private void renderScrollbar(GuiGraphics g) {
        float max = maxScroll();
        if (max <= 0) {
            return;
        }
        int trackX = px + pw - 10;
        int thumbH = Math.max(24, (int) (vh * (vh / (vh + max))));
        int thumbY = vy + (int) ((vh - thumbH) * (scroll / max));
        rpanel(g, trackX, vy, 3, vh, 1f, 0xFF140A22);
        rpanel(g, trackX, thumbY, 3, thumbH, 1f, MAG);
    }

    // ============================================================ module page logic

    private void openModule(WebModules.Def def, boolean keepScroll) {
        current = def;
        view = View.MODULE;
        editingRow = -1;
        editor.focused = false;
        search.focused = false;
        rows.clear();
        pageMessage = null;
        api = null;
        json = null;

        if (!def.dangerous()) {
            ApiHandler handler = ApiHandler.current;
            if (handler == null) {
                handler = new ApiHandler();
            }
            String route = def.api().isEmpty() ? def.path() : def.api();
            if (!route.isEmpty()) {
                api = handler.find(route);
            }
        }
        if (api == null) {
            pageMessage = def.dangerous()
                    ? "This action is only available in the web panel."
                    : "This module is configured in the web panel - use the button above.";
        } else {
            loadJson();
        }
        layoutModule();
        if (!keepScroll) {
            scroll = scrollTarget = 0;
        }
    }

    private void loadJson() {
        try {
            JsonElement e = JsonParser.parseString(api.get());
            if (e.isJsonObject()) {
                json = e.getAsJsonObject();
            } else {
                json = null;
                pageMessage = "This module is configured in the web panel - use the button above.";
            }
        } catch (Throwable t) {
            json = null;
            pageMessage = "This module is configured in the web panel - use the button above.";
        }
    }

    private void layoutModule() {
        rows.clear();
        Meta m = meta().get(current.component());
        descLines = m != null && !m.desc().isEmpty() ? font.split(Component.literal(m.desc()).withStyle(STYLE), vw) : List.of();
        rowsTop = 72 + descLines.size() * 13 + 14;
        int y = rowsTop;

        if (json != null) {
            y = buildRows(m, y);
        }
        if (rows.isEmpty() && pageMessage == null) {
            pageMessage = "No simple settings here - use the web panel.";
        }
        if (pageMessage != null) {
            y += 24;
        }
        contentH = y + 20;
        clampScroll();
    }

    private int buildRows(Meta m, int y) {
        Map<String, Leaf> leaves = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        collectLeaves(json, "", 0, leaves, notes);

        Set<String> used = new HashSet<>();
        if (m != null) {
            for (String[] lab : m.labels()) {
                String path = lab[0];
                if (lab[1].isEmpty()) {
                    // hidden field: set somewhere else (e.g. the worn cape, picked in the Capes tab)
                    used.add(path);
                    continue;
                }
                Leaf leaf = leaves.get(path);
                if (leaf != null) {
                    y = addLeafRow(leaf, lab[1], y);
                    used.add(path);
                } else {
                    for (String n : notes) {
                        if (n.equals(path) || n.startsWith(path + ".")) {
                            y = addRow(K_NOTE, lab[1], null, null, y);
                            used.add(n);
                            break;
                        }
                    }
                }
            }
        }

        boolean header = false;
        for (Map.Entry<String, Leaf> e : leaves.entrySet()) {
            if (used.contains(e.getKey())) {
                continue;
            }
            Leaf leaf = e.getValue();
            JsonElement v = leaf.owner.get(leaf.key);
            if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
                    && (v.getAsString().length() > 48 || v.getAsString().indexOf('\n') >= 0)) {
                continue;
            }
            if (!header) {
                y = addRow(K_SECTION, "More settings", null, null, y);
                header = true;
            }
            y = addLeafRow(leaf, prettify(leaf.key), y);
        }
        return y;
    }

    private int addLeafRow(Leaf leaf, String label, int y) {
        JsonElement v = leaf.owner.get(leaf.key);
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
            return addRow(K_BOOL, label, leaf.owner, leaf.key, y);
        }
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
                && (v.getAsString().length() > 48 || v.getAsString().indexOf('\n') >= 0)) {
            return addRow(K_NOTE, label, null, null, y);
        }
        return addRow(K_VALUE, label, leaf.owner, leaf.key, y);
    }

    private void collectLeaves(JsonObject o, String prefix, int depth, Map<String, Leaf> out, List<String> notes) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = e.getKey();
            JsonElement v = e.getValue();
            String path = prefix + key;
            if (v.isJsonPrimitive() || v.isJsonNull()) {
                Leaf l = new Leaf();
                l.owner = o;
                l.key = key;
                out.put(path, l);
            } else if (v.isJsonObject() && depth < 3) {
                collectLeaves(v.getAsJsonObject(), path + ".", depth + 1, out, notes);
            } else {
                notes.add(path);
            }
        }
    }

    private int addRow(int kind, String label, JsonObject owner, String key, int y) {
        Row r = new Row();
        r.kind = kind;
        r.label = label;
        r.owner = owner;
        r.key = key;
        r.y = y;
        rows.add(r);
        return y + 36;
    }

    private static String prettify(String id) {
        StringBuilder sb = new StringBuilder(id.length() + 4);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                if (Character.isUpperCase(c) && !Character.isUpperCase(id.charAt(i - 1))) {
                    sb.append(' ');
                }
                sb.append(c == '_' ? ' ' : c);
            }
        }
        return sb.toString();
    }

    /** Sends the edited JSON through the same API the web panel uses, then reloads what the game actually stored. */
    private void commit() {
        if (api == null || json == null) {
            return;
        }
        try {
            api.post(json.toString());
        } catch (Throwable ignored) {
        }
        loadJson();
        float keep = scrollTarget;
        layoutModule();
        scrollTarget = Math.min(keep, maxScroll());
        refreshStatuses();
    }

    private void finishEdit(boolean apply) {
        if (editingRow < 0) {
            return;
        }
        Row r = rows.get(editingRow);
        editingRow = -1;
        editor.focused = false;
        if (!apply) {
            return;
        }
        String text = editor.value.trim();
        JsonElement old = r.owner.get(r.key);
        try {
            if (old != null && old.isJsonPrimitive() && old.getAsJsonPrimitive().isNumber()) {
                if (text.matches("-?[0-9]+")) {
                    r.owner.addProperty(r.key, Long.parseLong(text));
                } else {
                    r.owner.addProperty(r.key, Double.parseDouble(text.replace(',', '.')));
                }
            } else {
                r.owner.addProperty(r.key, text);
            }
        } catch (NumberFormatException e) {
            return;
        }
        commit();
    }

    private void startEdit(int rowIndex) {
        Row r = rows.get(rowIndex);
        editingRow = rowIndex;
        JsonElement v = r.owner.get(r.key);
        editor.value = v == null || v.isJsonNull() ? "" : v.getAsString();
        editor.focused = true;
        search.focused = false;
    }

    // ============================================================ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() / us, my = event.y() / us;
        int button = event.button();

        if (!in(mx, my, px, py, pw, ph)) {
            return false;
        }

        if (mx < px + SIDEBAR_W) {
            if (button == 0) {
                clickSidebar(mx, my);
            }
            return true;
        }

        if (my < vy) {
            if (view == View.HOME) {
                if (in(mx, my, searchX, py + 9, searchW, 24)) {
                    search.focused = true;
                    return true;
                }
                search.focused = false;
            } else {
                if (button == 0 && in(mx, my, mainX + 14, py + 10, 100, 20)) {
                    goHome();
                    return true;
                }
                if (editingRow >= 0) {
                    finishEdit(true);
                }
            }
            return true;
        }

        search.focused = false;
        double cy = my - vy + scroll;
        double cx = mx - vx;

        if (editingRow >= 0) {
            Row r = rows.get(editingRow);
            if (in(cx, cy, vw - 148, r.y + 4, 140, 22)) {
                return true;
            }
            finishEdit(true);
        }
        if (view == View.CREDITS) {
            return clickCredits(button, cx, cy);
        }
        if (view == View.SHOTS) {
            return clickShots(button, cx, cy);
        }
        if (view == View.ACTIVE) {
            return clickActive(button, cx, cy);
        }
        if (view == View.CAPES) {
            return clickCapes(button, cx, cy);
        }
        return view == View.HOME ? clickCards(button, cx, cy) : clickModule(button, cx, cy);
    }

    private void clickSidebar(double mx, double my) {
        int iy = py + 66;
        for (int i = 0; i <= WebModules.GROUPS.length; i++) {
            if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
                if (view != View.HOME) {
                    goHome();
                }
                category = lastCategory = i;
                search.focused = false;
                rebuildList();
                return;
            }
            iy += 24;
        }
        iy += 6 + 14;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            openWeb("");
            return;
        }
        iy += 24;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            openActive();
            return;
        }
        iy += 24;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            openCapes();
            return;
        }
        iy += 24;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            openShots();
            return;
        }
        iy += 24;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            openCredits();
            return;
        }
        iy += 24;
        if (in(mx, my, px + 8, iy, SIDEBAR_W - 16, 22)) {
            onClose();
        }
    }

    private void openCredits() {
        finishEdit(false);
        view = View.CREDITS;
        current = null;
        api = null;
        json = null;
        rows.clear();
        pageMessage = null;
        descLines = List.of();
        search.focused = false;
        contentH = 300;
        scroll = scrollTarget = 0;
    }

    private void renderCredits(GuiGraphics g, int mx, int my) {
        int ox = vx, oy = vy - (int) scroll;

        String kicker = "TEAM";
        int kw = twb(kicker) + 28;
        rpanel(g, ox, oy + 6, kw, 18, 3f, 0xFF2A1040);
        fill(g, ox + 9, oy + 13, ox + 14, oy + 18, MAG);
        sb(g, kicker, ox + 20, oy + 10, MAG);
        drawSeq(g, seq("Credits", true), ox, oy + 32, TEXT, 1.9f);
        s(g, "The people behind Hellenic Client.", ox, oy + 72, TEXT_DIM);

        int cw = (vw - GAP) / 2;
        for (int i = 0; i < CREDIT_NAMES.length; i++) {
            int x = ox + (i % 2) * (cw + GAP), y = oy + 100 + (i / 2) * 50;
            boolean hover = in(mx, my, x, y, cw, 42) && in(mx, my, mainX, vy, mainW, vh);
            gradPanel(g, x, y, cw, 42, hover ? EDGE_HOVER : EDGE, hover ? 0xFF251442 : 0xFF1A0D30, 0xFF120820);
            int col = CREDIT_COLORS[i % CREDIT_COLORS.length];
            gradPanel(g, x + 8, y + 7, 28, 28, col, mix(col, 0xFF000000, 0.15f), mix(col, 0xFF000000, 0.45f));
            cs(g, CREDIT_NAMES[i].substring(0, 1).toUpperCase(Locale.ROOT), x + 22, y + 15, 0xFFFFFFFF, true);
            sb(g, fit(CREDIT_NAMES[i], cw - 56), x + 46, y + 15, TEXT);
        }

        // Discord card
        int dy = oy + 212;
        boolean dh = in(mx, my, ox, dy, vw, 64) && in(mx, my, mainX, vy, mainW, vh);
        gradPanel(g, ox, dy, vw, 64, dh ? MAG : MAG_DK, dh ? 0xFF3A1760 : 0xFF2A1048, 0xFF150A2A);
        sb(g, "Join our Discord", ox + 16, dy + 13, TEXT);
        s(g, "Support, updates and suggestions.", ox + 16, dy + 28, TEXT_DIM);
        s(g, "discord.gg/tx3Z54Qmkt", ox + 16, dy + 42, MAG);
        String btn = "Open Discord";
        int bw2 = tw(btn) + 28, bx = ox + vw - bw2 - 14, by = dy + 21;
        gradPanel(g, bx, by, bw2, 22, dh ? 0xFFFFFFFF : MAG, MAG_DK, 0xFF7A1590);
        cs(g, btn, bx + bw2 / 2, by + 6, 0xFFFFFFFF, true);
    }

    private boolean clickCredits(int button, double x, double y) {
        if (button == 0 && in(x, y, 0, 212, vw, 64)) {
            Util.getPlatform().openUri(DISCORD_URL);
            return true;
        }
        return false;
    }

    // ============================================================ active modules tab

    private void openActive() {
        finishEdit(false);
        view = View.ACTIVE;
        current = null;
        api = null;
        json = null;
        rows.clear();
        pageMessage = null;
        descLines = List.of();
        search.focused = false;
        refreshStatuses();
        buildActive();
        scroll = scrollTarget = 0;
    }

    private void buildActive() {
        active.clear();
        for (WebModules.Def d : WebModules.ALL) {
            if (Boolean.TRUE.equals(statuses.get(d.component()))) {
                active.add(d);
            }
        }
        cols = vw >= 380 ? 2 : 1;
        cardW = (vw - GAP * (cols - 1)) / cols;
        int rowsN = (active.size() + cols - 1) / cols;
        contentH = ACTIVE_TOP + Math.max(rowsN * (CARD_H + GAP), 40) + 20;
        clampScroll();
    }

    private void renderActive(GuiGraphics g, int mx, int my) {
        int ox = vx, oy = vy - (int) scroll;

        String kicker = "OVERVIEW";
        int kw = twb(kicker) + 28;
        rpanel(g, ox, oy + 6, kw, 18, 3f, 0xFF2A1040);
        fill(g, ox + 9, oy + 13, ox + 14, oy + 18, MAG);
        sb(g, kicker, ox + 20, oy + 10, MAG);
        drawSeq(g, seq("Active Modules", true), ox, oy + 32, TEXT, 1.9f);

        s(g, active.isEmpty() ? "No modules are enabled right now."
                : active.size() + (active.size() == 1 ? " module is enabled." : " modules are enabled.")
                + "  Right click a card to turn it off.", ox, oy + 72, TEXT_DIM);

        // HUD on/off button
        boolean hudOn = gr.hellenic.client.controllers.HudController.instance.isHudEnabled();
        String label = "Toggle HUD";
        int bw2 = tw(label) + 32 + 36, bx = ox + vw - bw2, by = oy + 36;
        boolean hh = in(mx, my, bx, by, bw2, 24) && in(mx, my, mainX, vy, mainW, vh);
        gradPanel(g, bx, by, bw2, 24, hudOn ? MAG : (hh ? EDGE_HOVER : EDGE), hudOn ? 0xFF33105A : 0xFF1B0D30, 0xFF120820);
        s(g, label, bx + 12, by + 8, hudOn ? TEXT : TEXT_SOFT);
        drawSwitch(g, bx + bw2 - 32 - 10, by + 4, hudOn ? 1f : 0f);

        int step = CARD_H + GAP;
        int firstRow = Math.max(0, (int) ((scroll - ACTIVE_TOP) / step));
        int lastRow = Math.min((active.size() + cols - 1) / cols - 1, (int) ((scroll + vh - ACTIVE_TOP) / step));
        for (int row = firstRow; row <= lastRow; row++) {
            int cy = oy + ACTIVE_TOP + row * step;
            for (int c = 0; c < cols; c++) {
                int idx = row * cols + c;
                if (idx >= active.size()) {
                    break;
                }
                drawCard(g, active.get(idx), ox + c * (cardW + GAP), cy, mx, my);
            }
        }
    }

    private boolean clickActive(int button, double x, double y) {
        String label = "Toggle HUD";
        int bw2 = tw(label) + 32 + 36;
        if (button == 0 && in(x, y, vw - bw2, 36, bw2, 24)) {
            gr.hellenic.client.controllers.HudController.instance.toggleHud();
            return true;
        }
        if (y < ACTIVE_TOP) {
            return false;
        }
        int step = CARD_H + GAP;
        int row = (int) ((y - ACTIVE_TOP) / step);
        double offY = (y - ACTIVE_TOP) - row * step;
        if (offY >= CARD_H) {
            return false;
        }
        for (int c = 0; c < cols; c++) {
            double cx0 = c * (cardW + GAP);
            if (x >= cx0 && x < cx0 + cardW) {
                int idx = row * cols + c;
                if (idx >= active.size()) {
                    return false;
                }
                WebModules.Def d = active.get(idx);
                boolean onSwitch = x >= cx0 + cardW - 56;
                if (button == 1 || (button == 0 && onSwitch)) {
                    quickToggle(d);
                    buildActive();
                } else if (button == 0) {
                    activate(d);
                }
                return true;
            }
        }
        return false;
    }

    // ============================================================ capes tab

    private void openCapes() {
        finishEdit(false);
        view = View.CAPES;
        current = null;
        api = null;
        json = null;
        rows.clear();
        pageMessage = null;
        descLines = List.of();
        search.focused = false;
        capeNote = null;
        scroll = scrollTarget = 0;
        layoutCapes();
    }

    /** Card grid: "No cape" first, then every cape; each card shows the cape's outer side (10:16). */
    private void layoutCapes() {
        capeCols = vw >= 520 ? 5 : vw >= 400 ? 4 : 3;
        capeW = (vw - GAP * (capeCols - 1)) / capeCols;
        capeImgH = Math.round((capeW - 28) * 1.6f);
        capeRowH = capeImgH + 52 + GAP;
        int items = HellenicCape.CAPES.size() + 1;
        int rowsN = (items + capeCols - 1) / capeCols;
        contentH = CAPE_TOP + rowsN * capeRowH + 12;
        clampScroll();
    }

    private void renderCapes(GuiGraphics g, int mx, int my) {
        int ox = vx, oy = vy - (int) scroll;

        String kicker = "COSMETICS";
        int kw = twb(kicker) + 28;
        rpanel(g, ox, oy + 6, kw, 18, 3f, 0xFF2A1040);
        fill(g, ox + 9, oy + 13, ox + 14, oy + 18, MAG);
        sb(g, kicker, ox + 20, oy + 10, MAG);
        drawSeq(g, seq("Capes", true), ox, oy + 32, TEXT, 1.9f);

        boolean noteOn = capeNote != null && System.currentTimeMillis() - capeNoteAt < 2500;
        s(g, noteOn ? capeNote : "Click a cape to wear it. Only you can see it, press F5 to look.", ox, oy + 72, noteOn ? GREEN : TEXT_DIM);

        HellenicCapeConfig cfg = ConfigStore.instance.getConfig().hellenicCapeConfig;
        String worn = cfg.enabled ? HellenicCape.find(cfg.cape).id() : null;

        int items = HellenicCape.CAPES.size() + 1;
        int cardH = capeImgH + 52;
        int firstRow = Math.max(0, (int) ((scroll - CAPE_TOP) / capeRowH));
        int lastRow = Math.min((items + capeCols - 1) / capeCols - 1, (int) ((scroll + vh - CAPE_TOP) / capeRowH));
        for (int row = firstRow; row <= lastRow; row++) {
            for (int c = 0; c < capeCols; c++) {
                int idx = row * capeCols + c;
                if (idx >= items) {
                    break;
                }
                HellenicCape.Cape cape = idx == 0 ? null : HellenicCape.CAPES.get(idx - 1);
                boolean on = cape == null ? worn == null : cape.id().equals(worn);
                int x = ox + c * (capeW + GAP), y = oy + CAPE_TOP + row * capeRowH;
                boolean hover = in(mx, my, x, y, capeW, cardH) && in(mx, my, mainX, vy, mainW, vh);

                if (on) {
                    rpanel(g, x - 2, y - 2, capeW + 4, cardH + 4, 4f, 0x18D946EF);
                    rpanel(g, x - 1, y - 1, capeW + 2, cardH + 2, 3.2f, 0x34D946EF);
                }
                gradPanel(g, x, y, capeW, cardH,
                        on ? MAG : (hover ? EDGE_HOVER : EDGE),
                        on ? 0xFF33105A : (hover ? 0xFF251442 : 0xFF1A0D30),
                        on ? 0xFF1C0A36 : 0xFF120820);

                int iw = capeW - 28, ix = x + 14, iy = y + 10;
                if (cape != null) {
                    rpanel(g, ix - 1, iy - 1, iw + 2, capeImgH + 2, 2f, 0xFF07040C);
                    int X = r(ix), Y = r(iy);
                    // outer side of the cape = texture area (16,16)-(176,272) of the 1024x512 texture
                    cur.blit(cape.texturePath(), X, Y, 16f, 16f,
                            r(ix + iw) - X, r(iy + capeImgH) - Y, 160, 256, 1024, 512, 0xFFFFFFFF);
                } else {
                    gradPanel(g, ix, iy, iw, capeImgH, on ? MAG_DK : EDGE, 0xFF140A22, 0xFF0D0717);
                    icon(g, "close", ix + iw / 2 - 9, iy + capeImgH / 2 - 9, 18, on ? MAG : TEXT_FAINT);
                }

                String name = cape == null ? "No cape" : cape.name();
                sb(g, fit(name, capeW - 18), x + 9, y + capeImgH + 18, TEXT);
                String sub = on ? "Equipped" : (cape == null ? "Hide the cape" : cape.note());
                s(g, fit(sub, capeW - 18), x + 9, y + capeImgH + 31, on ? GREEN : TEXT_DIM);
            }
        }
    }

    private boolean clickCapes(int button, double x, double y) {
        if (button != 0 || y < CAPE_TOP || x < 0) {
            return false;
        }
        int row = (int) ((y - CAPE_TOP) / capeRowH);
        double offY = (y - CAPE_TOP) - row * capeRowH;
        if (offY >= capeImgH + 52) {
            return false;
        }
        for (int c = 0; c < capeCols; c++) {
            double cx0 = c * (capeW + GAP);
            if (x >= cx0 && x < cx0 + capeW) {
                int idx = row * capeCols + c;
                if (idx > HellenicCape.CAPES.size()) {
                    return false;
                }
                HellenicCapeConfig cfg = ConfigStore.instance.getConfig().hellenicCapeConfig;
                if (idx == 0) {
                    cfg.enabled = false;
                    capeNote = "Cape hidden";
                } else {
                    HellenicCape.Cape cape = HellenicCape.CAPES.get(idx - 1);
                    cfg.cape = cape.id();
                    cfg.enabled = true;
                    capeNote = "Wearing " + cape.name() + " - press F5 to see it";
                }
                capeNoteAt = System.currentTimeMillis();
                ConfigStore.instance.requestWrite();
                refreshStatuses();
                return true;
            }
        }
        return false;
    }

    // ============================================================ screenshot manager

    private File shotDir() {
        return new File(minecraft.gameDirectory, "screenshots");
    }

    private void openShots() {
        finishEdit(false);
        view = View.SHOTS;
        current = null;
        api = null;
        json = null;
        rows.clear();
        pageMessage = null;
        descLines = List.of();
        search.focused = false;
        shotNote = null;

        releaseShots();
        shots.clear();
        File[] files = shotDir().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (int i = 0; i < files.length && i < SHOT_LIMIT; i++) {
                Shot sh = new Shot();
                sh.file = files[i];
                String n = files[i].getName();
                sh.name = n.substring(0, n.length() - 4);
                shots.add(sh);
            }
        }
        scroll = scrollTarget = 0;
        layoutShots();
    }

    private void layoutShots() {
        shotCols = vw >= 420 ? 3 : 2;
        shotW = (vw - GAP * (shotCols - 1)) / shotCols;
        shotImgH = shotW * 9 / 16;
        shotRowH = shotImgH + 24 + GAP;
        int rowsN = (shots.size() + shotCols - 1) / shotCols;
        contentH = SHOT_TOP + Math.max(rowsN * shotRowH, 60) + 20;
        clampScroll();
    }

    /** Frees the GPU textures of the thumbnails. */
    private void releaseShots() {
        if (minecraft == null) {
            return;
        }
        for (Shot sh : shots) {
            if (sh.tex != null) {
                minecraft.getTextureManager().release(sh.tex);
                sh.tex = null;
            }
        }
    }

    /** Reads one screenshot from disk and uploads a 320px-wide thumbnail (called at most once per frame). */
    private void loadThumb(Shot sh) {
        try (InputStream in = Files.newInputStream(sh.file.toPath()); NativeImage src = NativeImage.read(in)) {
            int w = Math.min(320, src.getWidth());
            int h = Math.max(1, src.getHeight() * w / src.getWidth());
            NativeImage dst = new NativeImage(w, h, false);
            src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), dst);
            Identifier id = Identifier.fromNamespaceAndPath("hellenicclient", "shots/" + SHOT_IDS.incrementAndGet());
            minecraft.getTextureManager().register(id, new DynamicTexture(() -> "hellenic screenshot", dst));
            sh.tex = id;
            sh.tw = w;
            sh.th = h;
        } catch (Throwable t) {
            sh.failed = true;
        }
    }

    private void renderShots(GuiGraphics g, int mx, int my) {
        int ox = vx, oy = vy - (int) scroll;

        String kicker = "GALLERY";
        int kw = twb(kicker) + 28;
        rpanel(g, ox, oy + 6, kw, 18, 3f, 0xFF2A1040);
        fill(g, ox + 9, oy + 13, ox + 14, oy + 18, MAG);
        sb(g, kicker, ox + 20, oy + 10, MAG);
        drawSeq(g, seq("Screenshots", true), ox, oy + 32, TEXT, 1.9f);

        boolean noteOn = shotNote != null && System.currentTimeMillis() - shotNoteAt < 2500;
        s(g, noteOn ? shotNote : "Click to open  -  Right click to copy the image", ox, oy + 72, noteOn ? GREEN : TEXT_DIM);

        String folder = "Open folder";
        int bw2 = tw(folder) + 28, bx = ox + vw - bw2, by = oy + 36;
        boolean fh = in(mx, my, bx, by, bw2, 22) && in(mx, my, mainX, vy, mainW, vh);
        gradPanel(g, bx, by, bw2, 22, fh ? MAG : EDGE, fh ? 0xFF3A1760 : 0xFF1B0D30, 0xFF120820);
        cs(g, folder, bx + bw2 / 2, by + 7, TEXT, false);

        if (shots.isEmpty()) {
            s(g, "No screenshots yet - press F2 in game to take one.", ox, oy + SHOT_TOP + 8, TEXT_DIM);
            return;
        }

        boolean loaded = false;
        int firstRow = Math.max(0, (int) ((scroll - SHOT_TOP) / shotRowH));
        int lastRow = Math.min((shots.size() + shotCols - 1) / shotCols - 1, (int) ((scroll + vh - SHOT_TOP) / shotRowH));
        for (int row = firstRow; row <= lastRow; row++) {
            for (int c = 0; c < shotCols; c++) {
                int idx = row * shotCols + c;
                if (idx >= shots.size()) {
                    break;
                }
                Shot sh = shots.get(idx);
                int x = ox + c * (shotW + GAP), y = oy + SHOT_TOP + row * shotRowH;
                boolean hover = in(mx, my, x, y, shotW, shotImgH + 22) && in(mx, my, mainX, vy, mainW, vh);

                if (sh.tex == null && !sh.failed && !loaded) {
                    loaded = true;
                    loadThumb(sh);
                }

                gradPanel(g, x, y, shotW, shotImgH + 22, hover ? MAG : EDGE, hover ? 0xFF251442 : 0xFF1A0D30, 0xFF120820);
                int ix = r(x) + bw + 1, iy = r(y) + bw + 1;
                int iw = r(x + shotW) - r(x) - 2 * (bw + 1), ih = r(y + shotImgH) - r(y) - bw - 1;
                if (sh.tex != null) {
                    cur.blit(sh.tex, ix, iy, 0f, 0f, iw, ih, sh.tw, sh.th, sh.tw, sh.th, 0xFFFFFFFF);
                } else {
                    cur.rect(ix, iy, ix + iw, iy + ih, 0xFF0F0818, 0xFF0F0818);
                    cs(g, sh.failed ? "Can't load" : "Loading...", x + shotW / 2, y + shotImgH / 2 - 4, TEXT_FAINT, false);
                }
                s(g, fit(sh.name, shotW - 14), x + 7, y + shotImgH + 6, hover ? TEXT : TEXT_SOFT);
            }
        }
    }

    private boolean clickShots(int button, double x, double y) {
        String folder = "Open folder";
        int bw2 = tw(folder) + 28;
        if (button == 0 && in(x, y, vw - bw2, 36, bw2, 22)) {
            File dir = shotDir();
            if (dir.isDirectory()) {
                Util.getPlatform().openFile(dir);
            }
            return true;
        }
        if (y < SHOT_TOP || x < 0) {
            return false;
        }
        int row = (int) ((y - SHOT_TOP) / shotRowH);
        double offY = (y - SHOT_TOP) - row * shotRowH;
        if (offY >= shotImgH + 22) {
            return false;
        }
        for (int c = 0; c < shotCols; c++) {
            double cx0 = c * (shotW + GAP);
            if (x >= cx0 && x < cx0 + shotW) {
                int idx = row * shotCols + c;
                if (idx >= shots.size()) {
                    return false;
                }
                Shot sh = shots.get(idx);
                if (button == 0) {
                    Util.getPlatform().openFile(sh.file);
                } else if (button == 1) {
                    copyImage(sh.file);
                }
                return true;
            }
        }
        return false;
    }

    private void setShotNote(String text) {
        shotNote = text;
        shotNoteAt = System.currentTimeMillis();
    }

    /**
     * Puts the picture itself on the clipboard (paste into Discord etc.) by asking Windows PowerShell to do it - AWT's
     * clipboard is unreliable inside Minecraft. The path goes through an environment variable, never into a command
     * string. Falls back to copying the file path if that fails.
     */
    private void copyImage(File file) {
        setShotNote("Copying image...");
        String path = file.getAbsolutePath();
        Thread t = new Thread(() -> {
            boolean ok = false;
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-STA", "-Command",
                        "Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing; "
                                + "$img = [System.Drawing.Image]::FromFile($env:HC_SHOT_PATH); "
                                + "[System.Windows.Forms.Clipboard]::SetImage($img); $img.Dispose()");
                pb.environment().put("HC_SHOT_PATH", path);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.getInputStream().readAllBytes();
                ok = p.waitFor() == 0;
            } catch (Throwable ignored) {
            }
            if (ok) {
                setShotNote("Image copied - paste it with Ctrl+V");
            } else {
                minecraft.execute(() -> minecraft.keyboardHandler.setClipboard(path));
                setShotNote("Couldn't copy the image - path copied instead");
            }
        }, "hellenic-copy-image");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void removed() {
        releaseShots();
        super.removed();
    }

    private boolean clickCards(int button, double x, double y) {
        int step = CARD_H + GAP;
        int r = (int) ((y - 4) / step);
        double offY = (y - 4) - r * step;
        if (offY >= CARD_H || y < 4) {
            return false;
        }
        for (int c = 0; c < cols; c++) {
            double cx0 = c * (cardW + GAP);
            if (x >= cx0 && x < cx0 + cardW) {
                int idx = r * cols + c;
                if (idx < 0 || idx >= shown.size()) {
                    return false;
                }
                WebModules.Def d = shown.get(idx);
                boolean toggleable = statuses.containsKey(d.component());
                boolean onSwitch = toggleable && x >= cx0 + cardW - 56;
                if (button == 1 || (button == 0 && onSwitch)) {
                    quickToggle(d);
                } else if (button == 0) {
                    activate(d);
                }
                return true;
            }
        }
        return false;
    }

    private void activate(WebModules.Def d) {
        if (!d.link().isEmpty()) {
            if (d.link().startsWith("local:")) {
                openWeb(d.link().substring(6));
            } else {
                Util.getPlatform().openUri(d.link().substring(9));
            }
            return;
        }
        openModule(d, false);
    }

    /** Flips the module's "enabled" switch through the same API the web panel uses. */
    private void quickToggle(WebModules.Def d) {
        if (d.dangerous() || !d.link().isEmpty()) {
            return;
        }
        ApiHandler handler = ApiHandler.current;
        if (handler == null) {
            return;
        }
        ApiBase a = handler.find(d.api().isEmpty() ? d.path() : d.api());
        if (a == null) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(a.get()).getAsJsonObject();
            JsonElement en = o.get("enabled");
            if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) {
                o.addProperty("enabled", !en.getAsBoolean());
                a.post(o.toString());
                refreshStatuses();
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean clickModule(int button, double x, double y) {
        if (button != 0) {
            return false;
        }
        int bw2 = tw("Open in web panel") + 36;
        if (in(x, y, vw - bw2, 36, bw2, 22)) {
            openWeb(current.path().isEmpty() ? "" : "#/" + current.path());
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.kind == K_SECTION || r.kind == K_NOTE) {
                continue;
            }
            if (in(x, y, 0, r.y, vw, 30)) {
                if (r.kind == K_BOOL) {
                    r.owner.addProperty(r.key, !r.owner.get(r.key).getAsBoolean());
                    commit();
                } else {
                    startEdit(i);
                }
                return true;
            }
        }
        return false;
    }

    private void goHome() {
        finishEdit(false);
        view = View.HOME;
        current = null;
        api = null;
        json = null;
        rows.clear();
        pageMessage = null;
        descLines = List.of();
        refreshStatuses();
        rebuildList();
        scroll = scrollTarget = Math.min(lastScroll, maxScroll());
    }

    private void openWeb(String suffix) {
        int port = ConfigHttpServer.instance.getPort();
        if (port > 0) {
            String path = suffix.isEmpty() || suffix.startsWith("/") ? suffix : "/" + suffix;
            Util.getPlatform().openUri("http://localhost:" + port + (path.isEmpty() ? "/" : path));
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scrollTarget -= (float) scrollY * 34f;
        clampScroll();
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        Field f = activeField();
        if (f != null) {
            String s = event.codepointAsString();
            if (!s.isEmpty() && s.charAt(0) >= 32) {
                f.type(s);
                searchChanged(f);
            }
            return true;
        }
        return false;
    }

    private void searchChanged(Field f) {
        if (f == search) {
            lastSearch = search.value;
            rebuildList();
        }
    }

    private Field activeField() {
        if (editingRow >= 0 && editor.focused) {
            return editor;
        }
        if (view == View.HOME && search.focused) {
            return search;
        }
        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        Field f = activeField();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingRow >= 0) {
                finishEdit(false);
            } else if (f != null) {
                f.focused = false;
            } else if (view != View.HOME) {
                goHome();
            } else {
                onClose();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_K && event.hasControlDown() && view == View.HOME) {
            search.focused = true;
            return true;
        }
        if (f != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                f.backspace();
                searchChanged(f);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (f == editor) {
                    finishEdit(true);
                } else {
                    f.focused = false;
                }
            } else if (event.isPaste()) {
                String clip = minecraft.keyboardHandler.getClipboard();
                if (clip != null) {
                    f.type(clip.replace('\n', ' ').replace('\r', ' '));
                    searchChanged(f);
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (view == View.HOME) {
            lastScroll = scrollTarget;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        // own dim background is drawn in render(); skipping vanilla blur keeps the menu cheap
    }
}
