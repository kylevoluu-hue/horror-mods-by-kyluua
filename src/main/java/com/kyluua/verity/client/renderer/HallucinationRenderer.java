package com.kyluua.verity.client.renderer;

import com.kyluua.verity.Verity;
import com.kyluua.verity.entity.HallucinationEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders hallucination sightings. Reuses the boss geometry as a distant silhouette
 * by default (a "tall figure that was just there"); swap the texture/model here to
 * add more sighting variants keyed off {@code HallucinationEntity} state.
 */
public class HallucinationRenderer extends GeoEntityRenderer<HallucinationEntity> {

    public HallucinationRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.0f; // hallucinations cast no shadow
    }

    private static final class Model extends GeoModel<HallucinationEntity> {
        private static final ResourceLocation GEO =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "geo/verity_boss.geo.json");
        private static final ResourceLocation TEX =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "textures/entity/verity_boss.png");
        private static final ResourceLocation ANIM =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "animations/verity_boss.animation.json");

        @Override
        public ResourceLocation getModelResource(HallucinationEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(HallucinationEntity animatable) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(HallucinationEntity animatable) {
            return ANIM;
        }
    }
}
