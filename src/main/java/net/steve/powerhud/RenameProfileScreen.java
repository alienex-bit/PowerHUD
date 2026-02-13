package net.steve.powerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class RenameProfileScreen extends Screen {
    private final Screen parent;
    private final String oldProfileName;
    private TextFieldWidget nameField;
    private final Runnable onRename;

    public RenameProfileScreen(Screen parent, String oldProfileName, Runnable onRename) {
        super(Text.literal("Rename Profile"));
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
        nameField = new TextFieldWidget(this.textRenderer, mid - w/2, y - 30, w, h, Text.literal("New Profile Name"));
        nameField.setText(oldProfileName);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Rename"),
            b -> {
                String newName = nameField.getText();
                String trimmed = newName != null ? newName.trim() : "";
                String sanitized = PowerHudConfig.sanitizeProfileName(trimmed);
                if (!sanitized.isEmpty() && !sanitized.equals(PowerHudConfig.sanitizeProfileName(oldProfileName))) {
                    PowerHudConfig.renameProfile(oldProfileName, trimmed);
                    onRename.run();
                }
                this.client.setScreen(parent);
            }
        ).dimensions(mid - w - 10, y, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Cancel"),
            b -> this.client.setScreen(parent)
        ).dimensions(mid + 10, y, w, h).build());
    }
}
