package com.kyluua.verity.client;

import com.kyluua.verity.Verity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The "Ask Verity" keybind (default: V). Registered on the mod bus; polled on the
 * Forge bus to open {@link VerityChatScreen}. Client-only.
 */
public final class VerityKeybinds {

    private VerityKeybinds() {}

    public static final KeyMapping ASK_KEY = new KeyMapping(
            "key.verity.ask",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.verity");

    /** Registered from the mod event bus (client only). */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ASK_KEY);
    }

    /** Polls the key each client tick and opens the screen when pressed. */
    @Mod.EventBusSubscriber(modid = Verity.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Input {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null && ASK_KEY.consumeClick()) {
                mc.setScreen(new VerityChatScreen());
            }
        }
    }
}
