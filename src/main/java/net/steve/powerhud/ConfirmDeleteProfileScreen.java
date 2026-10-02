package net.steve.powerhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteProfileScreen extends Screen {
    private final Screen parent;
    private final String profileName;

    public ConfirmDeleteProfileScreen(Screen parent, String profileName) {
        super(Component.literal("Confirm Delete"));
        this.parent = parent;
        this.profileName = profileName;
    }

    @Override
    protected void init() {
        int mid = this.width / 2;
        int y = this.height / 2 - 20;
        int w = 140;
        int h = 20;

        addRenderableWidget(Button.builder(
            Component.literal("Delete Profile: " + profileName),
            b -> {}
        ).bounds(mid - w / 2, y - 30, w, h).build()).active = false;

        addRenderableWidget(Button.builder(
            Component.literal("Confirm Delete"),
            b -> {
                PowerHudConfig.deleteProfile(profileName);
                if (this.minecraft != null && this.minecraft.gui != null) {
                    this.minecraft.gui.setScreen(parent);
                }
            }
        ).bounds(mid - w - 10, y, w, h).build());

        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            b -> {
                if (this.minecraft != null && this.minecraft.gui != null) {
                    this.minecraft.gui.setScreen(parent);
                }
            }
        ).bounds(mid + 10, y, w, h).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}