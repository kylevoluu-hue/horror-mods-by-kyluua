package com.kyluua.verity.client;

import com.kyluua.verity.network.AskQuestionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A tiny "Ask Verity" screen: type a question, hit send, and the client forwards it
 * to the server as an {@link AskQuestionPacket}. The server decides the (stage
 * appropriate) reply. Opened with the configurable keybind (default: V).
 */
public class VerityChatScreen extends Screen {

    private EditBox input;

    public VerityChatScreen() {
        super(Component.translatable("screen.verity.ask"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        input = new EditBox(this.font, cx - 150, cy - 10, 300, 20, Component.translatable("screen.verity.ask"));
        input.setMaxLength(200);
        input.setHint(Component.translatable("screen.verity.hint"));
        this.addRenderableWidget(input);
        this.setInitialFocus(input);

        this.addRenderableWidget(Button.builder(Component.translatable("screen.verity.send"), b -> send())
                .bounds(cx - 50, cy + 20, 100, 20).build());
    }

    private void send() {
        String text = input.getValue().trim();
        if (!text.isEmpty()) {
            PacketDistributor.sendToServer(new AskQuestionPacket(text));
        }
        this.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) { // enter / numpad enter
            send();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 35, 0xFFFFFF55);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
