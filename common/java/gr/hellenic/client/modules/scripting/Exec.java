package gr.hellenic.client.modules.scripting;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.common.events.SendChatEvent;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.Module;
import gr.hellenic.client.scripting.AsyncRunnable;
import gr.hellenic.client.utils.ClientChat;
import com.zergatul.scripting.DiagnosticMessage;
import com.zergatul.scripting.compiler.CompilationResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class Exec implements Module {

    public static final Exec instance = new Exec();

    private Exec() {
        Events.SendChat.add(this::onSendChat);
    }

    private void onSendChat(SendChatEvent event) {
        if (!ConfigStore.instance.getConfig().execConfig.enabled) {
            return;
        }

        if (event.getMessage().startsWith(".")) {
            try {
                CompilationResult result = ScriptsController.instance.compileKeys(event.getMessage().substring(1));
                if (result.getProgram() != null) {
                    result.<AsyncRunnable>getProgram().run();
                    systemMessage("OK", 0xFF80FF80);
                } else {
                    for (DiagnosticMessage message : result.getDiagnostics()) {
                        systemMessage(message.message, 0xFFFF8080);
                    }
                }
            }
            catch (Throwable e) {
                systemMessage(e.getMessage(), 0xFFFF8080);
            }

            event.cancel();
        }
    }

    private void systemMessage(String message, int color) {
        ClientChat.send(message, color);
    }
}