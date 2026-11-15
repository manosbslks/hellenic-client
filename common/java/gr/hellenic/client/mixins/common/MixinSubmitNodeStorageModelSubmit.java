package gr.hellenic.client.mixins.common;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.hellenic.client.extensions.ParametrizedSubmit;
import gr.hellenic.client.helpers.MixinLevelRendererHelper;
import gr.hellenic.client.modules.esp.EntityEsp;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.ModelSubmit.class)
public abstract class MixinSubmitNodeStorageModelSubmit implements ParametrizedSubmit {

    @Unique
    private EntityEsp.EntityRenderParameters parameters_CU;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onConstructor(
            PoseStack.Pose pose,
            Model<?> model,
            Object state,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfo info
    ) {
        parameters_CU = MixinLevelRendererHelper.CURRENT_SUBMIT_ENTITY_RENDER_PARAMETERS;
    }

    public EntityEsp.EntityRenderParameters getParameters_CU() {
        return parameters_CU;
    }
}