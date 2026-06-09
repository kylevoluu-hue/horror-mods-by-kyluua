package com.kyluua.verity.client;

import com.kyluua.verity.Verity;
import com.kyluua.verity.VerityConfig;
import com.kyluua.verity.network.ScreenEffectPacket;
import com.kyluua.verity.progression.CorruptionStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Random;

/**
 * Draws Verity's client-side screen effects and the creeping corruption fog.
 *
 * <p>Two layers of horror visuals:</p>
 * <ul>
 *   <li><b>Ambient</b> - a permanent vignette + fog that thickens smoothly with the
 *       cached corruption level ({@link ClientCorruptionState}); the world simply
 *       gets darker and closer as Verity corrupts.</li>
 *   <li><b>Triggered</b> - one-off pulses ({@link ScreenEffectPacket}) the director
 *       fires for scares: vignette stab, wobble, glitch bars, jumpscare flash. These
 *       decay over their duration.</li>
 * </ul>
 *
 * <p>Everything here is gated behind the player's client comfort config, so anyone
 * can soften or disable the visuals without affecting the server.</p>
 */
@EventBusSubscriber(modid = Verity.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ScreenEffectHandler {

    private ScreenEffectHandler() {}

    private static final Random RANDOM = new Random();

    // Active triggered effect state.
    private static int activeEffect = -1;
    private static int remainingTicks = 0;
    private static int totalTicks = 1;
    private static float effectIntensity = 0.0f;

    /** Called from the packet handler to start a timed effect. */
    public static void trigger(int effect, float intensity, int durationTicks) {
        activeEffect = effect;
        effectIntensity = intensity;
        remainingTicks = Math.max(1, durationTicks);
        totalTicks = remainingTicks;
    }

    // =========================================================================
    //  Tick: decay the active effect.
    // =========================================================================
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks > 0 && --remainingTicks <= 0) {
            activeEffect = -1;
        }
    }

    // =========================================================================
    //  Fog: thicken with corruption (and during a triggered distortion).
    // =========================================================================
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!VerityConfig.CLIENT.enableFog.get()) return;
        float corruption = ClientCorruptionState.normalized();
        if (corruption <= 0.01f) return;

        // Pull the far plane in as corruption rises: clear at 0, oppressive near 1.
        float vanillaFar = event.getFarPlaneDistance();
        float pulledFar = Math.max(6.0f, vanillaFar * (1.0f - 0.6f * corruption));
        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(pulledFar);
        event.setCanceled(true);
    }

    // =========================================================================
    //  GUI overlay: vignette + glitch + flash.
    // =========================================================================
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        double comfort = VerityConfig.CLIENT.screenEffectIntensity.get();
        if (comfort <= 0.0) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // --- Ambient vignette that grows with corruption ---------------------
        float corruption = ClientCorruptionState.normalized();
        if (corruption > 0.05f) {
            int alpha = (int) (Math.min(0.55f, corruption * 0.55f) * comfort * 255);
            drawVignette(g, w, h, alpha << 24); // black vignette
        }

        // --- Triggered effect ------------------------------------------------
        if (activeEffect < 0) return;
        float progress = remainingTicks / (float) totalTicks;       // 1 -> 0
        float strength = (float) (effectIntensity * progress * comfort);

        switch (activeEffect) {
            case ScreenEffectPacket.VIGNETTE -> drawVignette(g, w, h, ((int) (strength * 200) << 24));
            case ScreenEffectPacket.GLITCH -> drawGlitch(g, w, h, strength);
            case ScreenEffectPacket.JUMPSCARE -> {
                int a = (int) (strength * 255);
                g.fill(0, 0, w, h, (a << 24) | 0x000000); // black stab; texture overlay handled by resource pack
            }
            case ScreenEffectPacket.DISTORTION -> drawGlitch(g, w, h, strength * 0.5f);
            default -> { }
        }
    }

    /** Cheap four-edge vignette using gradient-ish solid bands. */
    private static void drawVignette(GuiGraphics g, int w, int h, int argb) {
        int band = Math.max(8, Math.min(w, h) / 6);
        g.fill(0, 0, w, band, argb);              // top
        g.fill(0, h - band, w, h, argb);          // bottom
        g.fill(0, 0, band, h, argb);              // left
        g.fill(w - band, 0, w, h, argb);          // right
    }

    /** Random horizontal glitch bars in eerie colours. */
    private static void drawGlitch(GuiGraphics g, int w, int h, float strength) {
        int bars = (int) (strength * 12);
        for (int i = 0; i < bars; i++) {
            int y = RANDOM.nextInt(h);
            int barH = 1 + RANDOM.nextInt(3);
            int alpha = (int) (strength * 120);
            int colour = (alpha << 24) | (RANDOM.nextBoolean() ? 0x880000 : 0x111111);
            g.fill(0, y, w, y + barH, colour);
        }
    }

    /** Used by ClientSetup to expose the current stage tint, if needed elsewhere. */
    public static CorruptionStage currentStage() {
        return ClientCorruptionState.stage();
    }
}
