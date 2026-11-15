package gr.hellenic.client.font;

import java.util.concurrent.CompletableFuture;

public class VanillaFontFactory extends FontFactory {

    protected VanillaFontFactory() {}

    @Override
    public CompletableFuture<FontReference> create(String name) {
        return CompletableFuture.completedFuture(new VanillaFontReference());
    }
}