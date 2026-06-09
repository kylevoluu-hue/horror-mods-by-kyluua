package com.kyluua.verity;

import com.kyluua.verity.client.ClientSetup;
import com.kyluua.verity.command.VerityCommand;
import com.kyluua.verity.dialogue.DialogueManager;
import com.kyluua.verity.event.ServerEvents;
import com.kyluua.verity.network.VerityNetwork;
import com.kyluua.verity.registry.VerityEntities;
import com.kyluua.verity.registry.VerityItems;
import com.kyluua.verity.registry.VeritySounds;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Verity - main mod entry point.
 *
 * <p>This is the class Forge instantiates when the mod loads. Its job is purely
 * wiring: it attaches every {@code DeferredRegister} to the mod event bus,
 * registers the config spec, sets up networking, and subscribes the server-side
 * event handlers. All of the actual behaviour lives in the sub-packages.</p>
 *
 * <p>The mod is fully server-authoritative: every scare, dialogue choice and
 * corruption tick is decided on the logical server and pushed to clients through
 * {@link VerityNetwork}. That keeps multiplayer in sync and means a dedicated
 * server never touches client-only rendering code.</p>
 */
@Mod(Verity.MOD_ID)
public final class Verity {

    /** The mod id - must match the value used everywhere (mods.toml, assets, data). */
    public static final String MOD_ID = "verity";

    /** Shared logger so every class can log under the same name. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Forge injects the mod event bus and container into the constructor.
     *
     * @param modEventBus the mod-specific event bus (registration, setup events)
     * @param modContainer this mod's container (used to register config files)
     */
    public Verity(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[Verity] Waking up. Hello.");

        // --- Registries -------------------------------------------------------
        VerityItems.ITEMS.register(modEventBus);
        VerityItems.CREATIVE_TABS.register(modEventBus);
        VerityEntities.ENTITY_TYPES.register(modEventBus);
        VeritySounds.SOUND_EVENTS.register(modEventBus);

        // --- Lifecycle / setup listeners (mod event bus) ----------------------
        modEventBus.addListener(VerityEntities::onAttributeCreation);
        modEventBus.addListener(VerityNetwork::register);

        // Client-only setup (renderers, screen effects). Guarded so the dedicated
        // server never loads any client classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::onClientSetup);
            modEventBus.addListener(ClientSetup::onRegisterRenderers);
            modEventBus.addListener(com.kyluua.verity.client.VerityKeybinds::register);
        }

        // --- Config -----------------------------------------------------------
        // SERVER config holds gameplay tuning (shared/authoritative); CLIENT config
        // holds purely visual/audio preferences that each player controls locally.
        modContainer.registerConfig(ModConfig.Type.SERVER, VerityConfig.SERVER_SPEC, "verity-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, VerityConfig.CLIENT_SPEC, "verity-client.toml");

        // --- Forge (game) event bus -------------------------------------------
        // Server ticking, command registration, datapack reload listener, etc.
        // ServerEvents and DialogueManager use instance @SubscribeEvent methods, so
        // they are registered as instances; VerityCommand uses static methods.
        NeoForge.EVENT_BUS.register(new ServerEvents());
        NeoForge.EVENT_BUS.register(VerityCommand.class);
        NeoForge.EVENT_BUS.register(DialogueManager.get());
    }
}
