package gr.hellenic.client.extensions;

import gr.hellenic.client.modules.esp.EntityEsp;

public interface EntityRenderStateExtension {
    EntityEsp.EntityRenderParameters getParameters_CU();
    void setParameters_CU(EntityEsp.EntityRenderParameters parameters);
}