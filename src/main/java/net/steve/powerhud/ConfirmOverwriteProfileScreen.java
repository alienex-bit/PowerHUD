package net.steve.powerhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmOverwriteProfileScreen extends Screen {
    private final Screen parent;
    private final String profileName;
    private final Runnable onConfirm;

    public ConfirmOverwriteProfileScreen(Screen parent, String profileName, Runnable onConfirm) {
        super(Component.literal("Confirm Overwrite"));
        this.parent = parent;
        this.profileName = profileName;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int mid = this.width / 2;
        int y = this.height / 2 - 20;
        int w = 160;
        int h = 20;

        addRenderableWidget(Button.builder(
            Component.literal("Overwrite Profile: " + profileName),
            b -> {}
        ).bounds(mid - w / 2, y - 30, w, h).build()).active = false;

        addRenderableWidget(Button.builder(
            Component.literal("Confirm Overwrite"),
            b -> {
                onConfirm.run();
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
