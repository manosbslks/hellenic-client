package gr.hellenic.client.scripting;

@FunctionalInterface
public interface KillAuraFunction {
    boolean shouldAttack(int id);
}