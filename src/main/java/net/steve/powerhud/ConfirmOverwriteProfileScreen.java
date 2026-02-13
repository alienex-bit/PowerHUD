package net.steve.powerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfirmOverwriteProfileScreen extends Screen {
    private final Screen parent;
    private final String profileName;
    private final Runnable onConfirm;

    public ConfirmOverwriteProfileScreen(Screen parent, String profileName, Runnable onConfirm) {
        super(Text.literal("Confirm Overwrite"));
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
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Overwrite Profile: " + profileName),
            b -> {}
        ).dimensions(mid - w/2, y - 30, w, h).build()).active = false;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Confirm Overwrite"),
            b -> {
                onConfirm.run();
                this.client.setScreen(parent);
            }
        ).dimensions(mid - w - 10, y, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Cancel"),
            b -> this.client.setScreen(parent)
        ).dimensions(mid + 10, y, w, h).build());
    }
}
