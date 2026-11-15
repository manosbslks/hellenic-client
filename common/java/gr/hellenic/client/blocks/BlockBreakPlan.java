package gr.hellenic.client.blocks;

import java.util.concurrent.CompletableFuture;

public abstract class BlockBreakPlan {
    public abstract CompletableFuture<Void> apply();
}