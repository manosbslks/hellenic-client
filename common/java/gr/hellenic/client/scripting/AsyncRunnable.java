package gr.hellenic.client.scripting;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncRunnable {
    CompletableFuture<?> run();
}