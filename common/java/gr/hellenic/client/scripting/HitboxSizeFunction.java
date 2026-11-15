package gr.hellenic.client.scripting;

@FunctionalInterface
public interface HitboxSizeFunction {
    boolean shouldApply(int id);
}