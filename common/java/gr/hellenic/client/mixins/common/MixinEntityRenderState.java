package gr.hellenic.client.mixins.common;

import gr.hellenic.client.extensions.EntityRenderStateExtension;
import gr.hellenic.client.modules.esp.EntityEsp;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements EntityRenderStateExtension {

    @Unique
    private EntityEsp.EntityRenderParameters parameters_CU;

    public EntityEsp.EntityRenderParameters getParameters_CU() {
        return parameters_CU;
    }

    public void setParameters_CU(EntityEsp.EntityRenderParameters parameters) {
        parameters_CU = parameters;
    }
}