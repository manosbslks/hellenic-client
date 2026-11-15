package gr.hellenic.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Chat output of the client, always prefixed with the "[Hellenic Client]" tag.
 */
public final class ClientChat {

    public static final int GREY = 0xFF8A94A6;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int PURPLE = 0xFFA855F7;
    public static final int LILAC = 0xFFC4B5FD;

    private ClientChat() {}

    public static MutableComponent prefix() {
        return Component.literal("")
                .append(Component.literal("[").withStyle(Style.EMPTY.withColor(GREY)))
                .append(Component.literal("Hellenic ").withStyle(Style.EMPTY.withColor(WHITE).withBold(true)))
                .append(Component.literal("Client").withStyle(Style.EMPTY.withColor(PURPLE).withBold(true)))
                .append(Component.literal("] ").withStyle(Style.EMPTY.withColor(GREY)));
    }

    public static void send(Component message) {
        Minecraft.getInstance().getChatListener().handleSystemMessage(prefix().append(message), false);
    }

    public static void send(String message, int color) {
        send(Component.literal(message).withStyle(Style.EMPTY.withColor(color)));
    }
}
