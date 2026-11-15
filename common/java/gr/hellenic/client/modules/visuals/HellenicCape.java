package gr.hellenic.client.modules.visuals;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.HellenicCapeConfig;
import gr.hellenic.client.modules.Module;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;

/**
 * Puts the chosen Hellenic Client cape on your own player. Purely client side: nobody else sees it.
 * Vanilla elytra falls back to the cape texture when the skin has no elytra texture, so the elytra gets the same design.
 * Called from LocalPlayer.getSkin() (see MixinLocalPlayer); the cape is picked in the menu's Capes tab or the web panel.
 */
public class HellenicCape implements Module {

    public static final HellenicCape instance = new HellenicCape();

    /** One wearable cape; texture at assets/hellenicclient/textures/cape/&lt;id&gt;.png (64x32 cape layout, 1024x512). */
    public record Cape(String id, String name, String note, Identifier texturePath, ClientAsset.Texture asset) {}

    /** Order = order of the cards in the Capes tab. Keep in sync with web/components/visuals/HellenicCape.js. */
    public static final List<Cape> CAPES = List.of(
            cape("hellenic", "Hellenic", "Default"),
            cape("souvlaki", "SouvlakiSMP", "Server"),
            cape("klanovalvides", "Klanovalvides", "Clan"),
            cape("acab", "A.C.A.B", "Black & white"));

    private static Cape cape(String id, String name, String note) {
        Identifier path = Identifier.fromNamespaceAndPath("hellenicclient", "textures/cape/" + id + ".png");
        return new Cape(id, name, note, path, new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("hellenicclient", "cape/" + id), path));
    }

    public static Cape find(String id) {
        for (Cape c : CAPES) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return CAPES.get(0);
    }

    /** getSkin() runs every frame, so the patched skin is rebuilt only when the real skin or the chosen cape changes. */
    private record Patched(PlayerSkin original, Cape cape, PlayerSkin result) {}

    private volatile Patched cache;

    private HellenicCape() {}

    public PlayerSkin apply(PlayerSkin skin) {
        HellenicCapeConfig config = ConfigStore.instance.getConfig().hellenicCapeConfig;
        if (skin == null || !config.enabled) {
            return skin;
        }
        Cape cape = find(config.cape);
        Patched c = cache;
        if (c == null || c.original() != skin || c.cape() != cape) {
            c = new Patched(skin, cape, new PlayerSkin(skin.body(), cape.asset(), skin.elytra(), skin.model(), skin.secure()));
            cache = c;
        }
        return c.result();
    }
}
