package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.modules.hacks.Blink;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

@SuppressWarnings("unused")
public class BlinkApi {

    @MethodDescription("""
            Checks if Blink is activated
            """)
    public boolean isEnabled() {
        return Blink.instance.isEnabled();
    }

    @MethodDescription("""
            Stops sending movement packets to server, making it look like you got network lag.
            You can continue moving, but this movements are client-side only.
            All movement packets are stored in buffer. You can either send all these packets to server by using blink.apply() or discard them by using blick.disable().
            """)
    @ApiVisibility(ApiType.ACTION)
    public void enable() {
        Blink.instance.enable();
    }

    @MethodDescription("""
            Discard all movement packets stored in buffer and returns your character to position where you activated blink.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void disable() {
        Blink.instance.disable();
    }

    @MethodDescription("""
            When blink is enabled, sends all movements packet to the server causing you to "teleport" to your current position.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void apply() {
        Blink.instance.apply();
    }

    @MethodDescription("""
            How far you are from position where you activated blink.
            """)
    public double getDistance() {
        return Blink.instance.getDistance();
    }

    @MethodDescription("""
            How many packets are stored in buffer since you activated blink.
            """)
    public int getPackets() {
        return Blink.instance.getPackets();
    }
}