package gr.hellenic.client.configs;

/** Hellenic Client cape on your own player (client side only). On by default. */
public class HellenicCapeConfig extends ModuleConfig {

    /** Id of the worn cape, see HellenicCape.CAPES; unknown ids fall back to the first cape. */
    public String cape = "hellenic";

    public HellenicCapeConfig() {
        enabled = true;
    }
}
