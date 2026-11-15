package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.scripting.Debugging;

public class DebugApi {
    public void write(String message) {
        Debugging.instance.addMessage(message);
    }
}