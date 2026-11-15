package gr.hellenic.client.scripting;

import gr.hellenic.client.scripting.types.ItemStackWrapper;

@FunctionalInterface
public interface ItemStackPredicate {
    boolean test(ItemStackWrapper itemStack);
}