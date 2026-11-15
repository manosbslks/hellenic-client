package gr.hellenic.client.utils;

import gr.hellenic.client.mixins.common.accessors.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public class KeyUtils {

    public static void click(KeyMapping mapping) {
        KeyMapping.click(((KeyMappingAccessor) mapping).getKey_CU());
    }
}