package com.kyluua.verity.client.renderer;

import com.kyluua.verity.Verity;
import com.kyluua.verity.entity.VerityCompanionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the floating smiley companion with GeckoLib. The texture is chosen from
 * the entity's current {@link com.kyluua.verity.entity.VeritySmileState}, so the
 * face visibly degrades from happy to wrong as corruption rises.
 */
public class VerityCompanionRenderer extends GeoEntityRenderer<VerityCompanionEntity> {

    public VerityCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.3f;
    }

    /** GeoModel binds the .geo.json geometry, the per-stage texture and the animations. */
    private static final class Model extends GeoModel<VerityCompanionEntity> {
        private static final ResourceLocation GEO =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "geo/verity_companion.geo.json");
        private static final ResourceLocation ANIM =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "animations/verity_companion.animation.json");

        @Override
        public ResourceLocation getModelResource(VerityCompanionEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(VerityCompanionEntity animatable) {
            // Per-stage face swap (innocent -> evil).
            return animatable.getSmileState().texture();
        }

        @Override
        public ResourceLocation getAnimationResource(VerityCompanionEntity animatable) {
            return ANIM;
        }
    }
}
