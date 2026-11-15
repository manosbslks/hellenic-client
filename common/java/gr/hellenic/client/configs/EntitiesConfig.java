package gr.hellenic.client.configs;

import gr.hellenic.client.collections.ImmutableList;

public class EntitiesConfig implements ModuleStateProvider {

    public ImmutableList<EntityEspConfig> configs = new ImmutableList<>();

    public void add(EntityEspConfig config) {
        configs = configs.add(config);
    }

    public void remove(EntityEspConfig config) {
        configs = configs.remove(config);
    }

    @Override
    public boolean isEnabled() {
        return configs.stream().anyMatch(c -> c.enabled);
    }
}