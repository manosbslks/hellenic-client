package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.concurrent.TickEndExecutor;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

public class DelayedApi {

    @MethodDescription("""
            Runs actions after some amount of ticks passed.
            Action may run while no world is loaded
            """)
    @ApiVisibility(ApiType.ACTION)
    public void run(int ticks, Runnable action) {
        if (ticks <= 0) {
            return;
        }

        TickEndExecutor.instance.waitTicks(ticks, action);
    }
}