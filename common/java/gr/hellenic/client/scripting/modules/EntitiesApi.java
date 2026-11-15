package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.collections.ImmutableList;
import gr.hellenic.client.common.Registries;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.EntityEspConfig;
import gr.hellenic.client.modules.esp.EntityEsp;
import gr.hellenic.client.scripting.ApiVisibility;
import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.utils.ColorUtils;
import gr.hellenic.client.wrappers.ClassRemapper;
import com.zergatul.scripting.MethodDescription;

import java.awt.*;

@SuppressWarnings("unused")
public class EntitiesApi {

    @MethodDescription("""
            Checks if Entity ESP rendering is enabled
            """)
    public boolean isEnabled() {
        return EntityEsp.instance.isEnabled();
    }

    @MethodDescription("""
            Toggles Entity ESP rendering
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        EntityEsp.instance.toggle();
    }

    @MethodDescription("""
            Returns all entity class names you have configured for Entity ESP. Does not skip disabled ones.
            """)
    public String[] getEntries() {
        return ConfigStore.instance.getConfig().entities.configs.stream()
                .map(c -> ClassRemapper.fromObf(c.clazz.getName()))
                .toArray(String[]::new);
    }

    @MethodDescription("""
            Checks if Entity ESP is enabled for specified entity class
            """)
    public boolean isEnabled(String className) {
        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return false;
        }
        return config.enabled;
    }

    @MethodDescription("""
            Toggles enabled state for specified entity class
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle(String className) {
        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }
        config.enabled = !config.enabled;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setTracerColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.tracerColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setOutlineColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.glowColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setCollisionBoxColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.outlineColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setOverlayColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.overlayColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    private EntityEspConfig getConfig(String className) {
        ImmutableList<EntityEspConfig> list = ConfigStore.instance.getConfig().entities.configs;
        return list.stream()
                .filter(c -> c.clazz.getName().equals(ClassRemapper.toObf(className)))
                .findFirst()
                .orElse(null);
    }
}