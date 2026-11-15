package gr.hellenic.client.utils;

import gr.hellenic.client.mixins.common.accessors.MinecraftAccessor;
import net.minecraft.client.Minecraft;

public class GlobalTicks {
    public static long get() {
        return ((MinecraftAccessor) Minecraft.getInstance()).getClientTickCount_CU();
    }
}