package gr.hellenic.client.utils;

import java.util.concurrent.CompletableFuture;

public abstract class EntityInteractionPlan {
    public abstract CompletableFuture<EntityInteractionResult> apply();
}