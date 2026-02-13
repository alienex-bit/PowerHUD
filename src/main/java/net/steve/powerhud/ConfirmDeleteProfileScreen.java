package net.steve.powerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfirmDeleteProfileScreen extends Screen {
    private final Screen parent;
    private final String profileName;
    public ConfirmDeleteProfileScreen(Screen parent, String profileName) {
        super(Text.literal("Confirm Delete"));
        this.parent = parent;
        this.profileName = profileName;
    }

    @Override
    protected void init() {
        int mid = this.width / 2;
        int y = this.height / 2 - 20;
        int w = 120;
        int h = 20;
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Delete Profile: " + profileName),
            b -> {}
        ).dimensions(mid - w/2, y - 30, w, h).build()).active = false;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Confirm Delete"),
            b -> {
                PowerHudConfig.deleteProfile(profileName);
                this.client.setScreen(parent);
            }
        ).dimensions(mid - w - 10, y, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Cancel"),
            b -> this.client.setScreen(parent)
        ).dimensions(mid + 10, y, w, h).build());
    }
}