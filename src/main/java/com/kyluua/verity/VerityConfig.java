package com.kyluua.verity;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for Verity.
 *
 * <p>Split into two specs:</p>
 * <ul>
 *   <li><b>SERVER</b> - gameplay tuning that must be identical for everyone on a
 *       server (progression speed, scare frequency, feature toggles). On a
 *       dedicated server only the admin can edit these.</li>
 *   <li><b>CLIENT</b> - personal comfort options each player controls (screen
 *       distortion intensity, master horror volume). These never affect other
 *       players.</li>
 * </ul>
 *
 * <p>Server admins who want to soften (or intensify) the experience only need to
 * touch {@code verity-server.toml}.</p>
 */
public final class VerityConfig {

    // =========================================================================
    //  SERVER spec
    // =========================================================================
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    // =========================================================================
    //  CLIENT spec
    // =========================================================================
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();

        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private VerityConfig() {}

    /** Server-side, admin-controlled gameplay configuration. */
    public static final class Server {

        // --- Progression -----------------------------------------------------
        public final ModConfigSpec.DoubleValue progressionSpeed;
        public final ModConfigSpec.DoubleValue interactionWeight;
        public final ModConfigSpec.BooleanValue progressionEnabled;
        public final ModConfigSpec.IntValue startingCorruption;

        // --- Scares ----------------------------------------------------------
        public final ModConfigSpec.DoubleValue scareFrequency;
        public final ModConfigSpec.BooleanValue enableJumpscares;
        public final ModConfigSpec.BooleanValue enableHallucinations;
        public final ModConfigSpec.BooleanValue enableFakeChat;
        public final ModConfigSpec.BooleanValue enableEnvironmentScares;

        // --- Boss ------------------------------------------------------------
        public final ModConfigSpec.BooleanValue enableFinalBoss;

        Server(ModConfigSpec.Builder b) {
            b.comment("Verity - server gameplay settings.",
                    "These values are shared by everyone on the server.").push("progression");

            progressionEnabled = b
                    .comment("Master switch for corruption progression. If false, Verity stays at its starting level forever.")
                    .define("progressionEnabled", true);

            progressionSpeed = b
                    .comment("How fast corruption rises over real playtime.",
                            "1.0 = default (~a few hours to reach the final stage). Higher = faster.")
                    .defineInRange("progressionSpeed", 1.0D, 0.0D, 100.0D);

            interactionWeight = b
                    .comment("Extra corruption gained each time a player talks to / interacts with Verity.")
                    .defineInRange("interactionWeight", 1.0D, 0.0D, 100.0D);

            startingCorruption = b
                    .comment("Corruption level (0-100) a freshly released Verity starts at.")
                    .defineInRange("startingCorruption", 0, 0, 100);

            b.pop();

            b.comment("Scare director settings.").push("scares");

            scareFrequency = b
                    .comment("Global multiplier on how often scares fire. 1.0 = default, 0.0 = never.")
                    .defineInRange("scareFrequency", 1.0D, 0.0D, 10.0D);

            enableJumpscares = b
                    .comment("Allow rare direct jumpscare events (stage 4+).")
                    .define("enableJumpscares", true);

            enableHallucinations = b
                    .comment("Allow client-only hallucination entities visible to single players.")
                    .define("enableHallucinations", true);

            enableFakeChat = b
                    .comment("Allow fake chat lines and fake join/leave messages.")
                    .define("enableFakeChat", true);

            enableEnvironmentScares = b
                    .comment("Allow environmental scares (doors, torch flicker, etc.).")
                    .define("enableEnvironmentScares", true);

            b.pop();

            b.comment("Final boss settings.").push("boss");

            enableFinalBoss = b
                    .comment("If false, corruption caps just below the final transformation.")
                    .define("enableFinalBoss", true);

            b.pop();
        }
    }

    /** Client-side, per-player comfort configuration. */
    public static final class Client {

        public final ModConfigSpec.DoubleValue screenEffectIntensity;
        public final ModConfigSpec.DoubleValue horrorVolume;
        public final ModConfigSpec.BooleanValue enableFog;
        public final ModConfigSpec.BooleanValue enableScreenDistortion;

        Client(ModConfigSpec.Builder b) {
            b.comment("Verity - client comfort settings. These only affect your own game.").push("comfort");

            screenEffectIntensity = b
                    .comment("Multiplier for screen distortion / vignette / glitch overlays. 0.0 disables visuals.")
                    .defineInRange("screenEffectIntensity", 1.0D, 0.0D, 1.0D);

            horrorVolume = b
                    .comment("Local volume multiplier for Verity's whispers, drones and stingers.")
                    .defineInRange("horrorVolume", 1.0D, 0.0D, 1.0D);

            enableFog = b
                    .comment("Enable the creeping fog that thickens with corruption.")
                    .define("enableFog", true);

            enableScreenDistortion = b
                    .comment("Enable the wobble/glitch screen distortion at high corruption.")
                    .define("enableScreenDistortion", true);

            b.pop();
        }
    }
}
