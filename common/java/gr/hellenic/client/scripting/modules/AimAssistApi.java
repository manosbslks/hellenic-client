package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.configs.AimAssistConfig;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.modules.automation.AimAssist;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
public class AimAssistApi {

    public boolean isBowAssistEnabled() {
        return getConfig().bowAssist;
    }

    public void toggleBowAssist() {
        AimAssistConfig config = getConfig();
        config.bowAssist = !config.bowAssist;
        ConfigStore.instance.requestWrite();
    }

    public boolean hasBowAssistTarget() {
        return AimAssist.instance.getBowAssistTarget() != null;
    }

    public int getBowAssistEntityId() {
        Entity target = AimAssist.instance.getBowAssistTarget();
        return target != null ? target.getId() : Integer.MIN_VALUE;
    }

    public boolean isTargetLockEnabled() {
        return AimAssist.instance.isTargetLockEnabled();
    }

    public void enableTargetLock() {
        AimAssist.instance.enableTargetLock();
    }

    public void disableTargetLock() {
        AimAssist.instance.disableTargetLock();
    }

    private AimAssistConfig getConfig() {
        return ConfigStore.instance.getConfig().aimAssist;
    }
}