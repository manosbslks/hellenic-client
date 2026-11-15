package gr.hellenic.client.font;

public abstract class FontReference {
    public abstract FontBackend createFontBackend(FontRenderParameters parameters);
}