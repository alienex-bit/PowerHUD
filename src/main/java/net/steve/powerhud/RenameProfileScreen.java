package net.steve.powerhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RenameProfileScreen extends Screen {
    private final Screen parent;
    private final String oldProfileName;
    private EditBox nameField;
    private final Runnable onRename;

    public RenameProfileScreen(Screen parent, String oldProfileName, Runnable onRename) {
        super(Component.literal("Rename Profile"));
        this.parent = parent;
        this.oldProfileName = oldProfileName;
        this.onRename = onRename;
    }

    @Override
    protected void init() {
        int mid = this.width / 2;
        int y = this.height / 2 - 20;
        int w = 160;
        int h = 20;
        nameField = new EditBox(this.font, mid - w / 2, y - 30, w, h, Component.literal("New Profile Name"));
        nameField.setValue(oldProfileName);
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(
            Component.literal("Rename"),
            b -> {
                String newName = nameField.getValue();
                String trimmed = newName != null ? newName.trim() : "";
                String sanitized = PowerHudConfig.sanitizeProfileName(trimmed);
                if (!sanitized.isEmpty() && !sanitized.equals(PowerHudConfig.sanitizeProfileName(oldProfileName))) {
                    PowerHudConfig.renameProfile(oldProfileName, trimmed);
                    onRename.run();
                }
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
