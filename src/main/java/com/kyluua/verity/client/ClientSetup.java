package com.kyluua.verity.client;

import com.kyluua.verity.client.renderer.HallucinationRenderer;
import com.kyluua.verity.client.renderer.VerityBossRenderer;
import com.kyluua.verity.client.renderer.VerityCompanionRenderer;
import com.kyluua.verity.registry.VerityEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup. Bound from {@link com.kyluua.verity.Verity} on the mod event
 * bus, and only when running on the physical client, so none of these classes are
 * ever loaded by a dedicated server.
 */
public final class ClientSetup {

    private ClientSetup() {}

    /** General client init (reserved for key mappings, particle factories, etc.). */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Nothing extra needed yet; GeckoLib self-initialises on Forge.
        });
    }

    /** Binds GeckoLib renderers to Verity's entity types. */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VerityEntities.VERITY_COMPANION.get(), VerityCompanionRenderer::new);
        event.registerEntityRenderer(VerityEntities.VERITY_BOSS.get(), VerityBossRenderer::new);
        event.registerEntityRenderer(VerityEntities.HALLUCINATION.get(), HallucinationRenderer::new);
    }
}
