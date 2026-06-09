package com.kyluua.verity.client.renderer;

import com.kyluua.verity.Verity;
import com.kyluua.verity.entity.VerityBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the tall humanoid final boss. Kept deliberately simple in the first
 * version - a single texture and the shared boss geo/animation set; phase-specific
 * animation switching is handled by the entity's animation controller.
 */
public class VerityBossRenderer extends GeoEntityRenderer<VerityBossEntity> {

    public VerityBossRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.6f;
    }

    private static final class Model extends GeoModel<VerityBossEntity> {
        private static final ResourceLocation GEO =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "geo/verity_boss.geo.json");
        private static final ResourceLocation TEX =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "textures/entity/verity_boss.png");
        private static final ResourceLocation ANIM =
                ResourceLocation.fromNamespaceAndPath(Verity.MOD_ID, "animations/verity_boss.animation.json");

        @Override
        public ResourceLocation getModelResource(VerityBossEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(VerityBossEntity animatable) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(VerityBossEntity animatable) {
            return ANIM;
        }
    }
}
