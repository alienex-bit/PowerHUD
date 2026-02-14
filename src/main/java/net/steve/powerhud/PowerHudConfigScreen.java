// ...existing code...
package net.steve.powerhud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.widget.TextFieldWidget;

import static net.steve.powerhud.HudConstants.*;

public class PowerHudConfigScreen extends Screen {
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (currentCategory == Category.PROFILES && profileNameField != null && profileNameField.isFocused()) {
                // Enter or Ctrl+S to save
                if (keyCode == 257 || (keyCode == 83 && (modifiers & 2) != 0)) { // 257: Enter, 83: S, 2: Ctrl
                    if (profileNameField.getText() != null && !profileNameField.getText().trim().isEmpty()) {
                        profileInput = profileNameField.getText();
                        String trimmed = profileInput.trim();
                        String sanitized = net.steve.powerhud.PowerHudConfig.sanitizeProfileName(trimmed);
                        if (!sanitized.isEmpty()) {
                            List<String> profiles = net.steve.powerhud.PowerHudConfig.listProfiles();
                            boolean exists = profiles.contains(sanitized);
                            Runnable doSave = () -> {
                                boolean success = net.steve.powerhud.PowerHudConfig.saveProfile(trimmed);
                                if (success) showFeedback("Profile saved: " + trimmed, 0xFF22CC22);
                                else showFeedback("Failed to save profile", 0xFFCC2222);
                                clearAndInit();
                            };
                            if (exists) {
                                this.client.setScreen(new ConfirmOverwriteProfileScreen(this, trimmed, doSave));
                            } else {
                                doSave.run();
                            }
                        } else {
                            showFeedback("Invalid profile name", 0xFFCC2222);
                        }
                    }
                    return true;
                }
                // Esc to cancel
                if (keyCode == 256) { // 256: Esc
                    profileNameField.setFocused(false);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    // Visual feedback message and timer
    private String feedbackMessage = null;
    private int feedbackTicks = 0;
    private static final int FEEDBACK_DURATION = 60; // 3 seconds at 20 TPS

    private enum Category { 
        DISPLAY, ELEMENTS, THEME, FPS_TWEAK, INV_TWEAK, ABOUT, PROFILES
    }
    
    private static Category currentCategory = Category.DISPLAY;
    private final List<TooltipArea> tooltips = new ArrayList<>();
    // Removed final modVer field; replaced with dynamic getter
    private String profileInput = null;
    private TextFieldWidget profileNameField;

    public PowerHudConfigScreen() {
        super(Text.literal("PowerHUD Config"));
    }

    private String getModVersion() {
        return FabricLoader.getInstance()
            .getModContainer("powerhud")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    }

    @Override
    public void tick() {
        super.tick();
        if (feedbackTicks > 0) {
            feedbackTicks--;
            if (feedbackTicks == 0) feedbackMessage = null;
        }
    }

    private void showFeedback(String msg, int color) {
        this.feedbackMessage = msg;
        this.feedbackTicks = FEEDBACK_DURATION;
    }
    
    @Override
    public void renderBackground(DrawContext c, int mx, int my, float d) {}
    

    private record TooltipArea(int x, int y, int w, int h, String text) {}

    
    @Override
    public void close() {
        PowerHudConfig.save();
        super.close();
    }
    
    @Override
    protected void init() {
        feedbackMessage = null;
        feedbackTicks = 0;
        tooltips.clear();
        
        int mid = this.width / 2;
        int startY = UI_START_Y;
        int leading = SPACING_LEADING;
        int btnH = BUTTON_HEIGHT;
        int btnW = BUTTON_WIDTH_STANDARD;
        int rX = mid + SCREEN_CENTER_OFFSET;
        int lX = mid - SCREEN_LEFT_OFFSET;
        int hW = BUTTON_WIDTH_HALF;
        int gap = SPACING_GAP;
        
        // Left side tabs
        addTab(lX, startY, "Display", Category.DISPLAY);
        addTab(lX, startY + leading, "HUD Elements", Category.ELEMENTS);
        addDrawableChild(ButtonWidget.builder(
            Text.literal("HUD Element Order"),
            b -> { this.client.setScreen(new HudOrderScreen(this)); }
        ).dimensions(lX, startY + (leading * 2), BUTTON_WIDTH_TAB, BUTTON_HEIGHT_TALL).build());
        addTab(lX, startY + (leading * 3), "Theme", Category.THEME);
        addTab(lX, startY + (leading * 4), "FPS Tweak", Category.FPS_TWEAK);
        addTab(lX, startY + (leading * 5), "Inventory Tweak", Category.INV_TWEAK);
        addTab(lX, startY + (leading * 6), "Profiles", Category.PROFILES);
        addTab(lX, startY + (leading * 7), "About", Category.ABOUT);
        // Right side content based on category
        int profileY = startY;
        int profileRX = rX;
        switch (currentCategory) {
            case DISPLAY:
                addToggle(
                    rX, startY,
                    "HUD Master",
                    PowerHudConfig.hudEnabled ? "ON" : "OFF",
                    "Toggle HUD Visibility",
                    b -> {
                        PowerHudConfig.hudEnabled = !PowerHudConfig.hudEnabled;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                addStepper(
                    rX, startY + leading,
                    "HUD Scale",
                    PowerHudConfig.hudScaleVert,
                    "Global HUD Scale",
                    val -> {
                        if(isValidScale(val)) {
                            PowerHudConfig.hudScaleVert = val;
                        }
                    },
                    btnW, btnH, 10
                );
                break;
            case PROFILES:
                // Show current profile at the top
                String loadedProfile = net.steve.powerhud.PowerHudConfig.currentProfile;
                String loadedProfileLabel = (loadedProfile == null || loadedProfile.isEmpty()) ? "Current: Default (Unsaved)" : ("Current: " + loadedProfile);
                addLabel(profileRX, profileY, loadedProfileLabel, btnW, btnH);
                profileY += leading;
                // Export/Import buttons with tooltips
                tooltips.add(new TooltipArea(profileRX, profileY, btnW/2 - gap, btnH, "Export current profile to your home folder"));
                addButton(profileRX, profileY, "Export", btnW/2 - gap, btnH, b -> {
                    if (PowerHudConfig.currentProfile != null && !PowerHudConfig.currentProfile.isEmpty()) {
                        String name = PowerHudConfig.currentProfile;
                        java.nio.file.Path exportPath = java.nio.file.Paths.get(System.getProperty("user.home"), name + ".json");
                        boolean ok = ProfileExportImport.exportProfile(name, exportPath);
                        showFeedback(ok ? "Exported to " + exportPath : "Export failed", ok ? 0xFF22CC22 : 0xFFCC2222);
                    }
                });
                tooltips.add(new TooltipArea(profileRX + btnW/2 + gap, profileY, btnW/2 - gap, btnH, "Import a profile from 'import_profile.json' in your home folder"));
                addButton(profileRX + btnW/2 + gap, profileY, "Import", btnW/2 - gap, btnH, b -> {
                    java.nio.file.Path importPath = java.nio.file.Paths.get(System.getProperty("user.home"), "import_profile.json");
                    boolean ok = ProfileExportImport.importProfile(importPath);
                    String importedName = importPath.getFileName().toString().replaceFirst("\\.json$", "");
                    if (ok) {
                        showFeedback("Imported profile: " + importedName, 0xFF22CC22);
                    } else {
                        showFeedback("Import failed", 0xFFCC2222);
                    }
                    clearAndInit();
                });
                profileY += leading;
                if (profileNameField == null) {
                    profileNameField = new TextFieldWidget(this.textRenderer, profileRX, profileY, btnW, btnH, Text.literal("Profile Name"));
                    profileNameField.setText("");
                    profileNameField.setFocused(true);
                }
                profileNameField.setText(this.profileInput != null ? this.profileInput : "");
                addDrawableChild(profileNameField);
                tooltips.add(new TooltipArea(profileRX, profileY, btnW, btnH, "Enter a new profile name here (leave blank to save to current profile). Only letters, numbers, -, _ allowed. Max 32 chars."));
                profileY += leading;

                tooltips.add(new TooltipArea(profileRX, profileY, btnW, btnH, "Save to current profile, or create/overwrite a new profile if a name is entered above"));
                ButtonWidget saveBtn = ButtonWidget.builder(
                    Text.literal("Save Current"),
                    b -> {
                        this.profileInput = profileNameField.getText();
                        String trimmed = this.profileInput != null ? this.profileInput.trim() : "";
                        if (trimmed.isEmpty()) {
                            // Save to current profile
                            String current = net.steve.powerhud.PowerHudConfig.currentProfile;
                            if (current == null || current.isEmpty()) {
                                showFeedback("No profile selected to save.", 0xFFCC2222);
                                return;
                            }
                            boolean success = net.steve.powerhud.PowerHudConfig.saveProfile(current);
                            if (success) showFeedback("Profile saved: " + current, 0xFF22CC22);
                            else showFeedback("Failed to save profile", 0xFFCC2222);
                            clearAndInit();
                        } else {
                            // Validate name: only allow a-z, A-Z, 0-9, -, _ and max 32 chars
                            String sanitized = net.steve.powerhud.PowerHudConfig.sanitizeProfileName(trimmed);
                            if (sanitized.isEmpty() || !trimmed.equals(sanitized)) {
                                showFeedback("Invalid profile name. Only letters, numbers, -, _ allowed. Max 32 chars.", 0xFFCC2222);
                                return;
                            }
                            List<String> profiles = net.steve.powerhud.PowerHudConfig.listProfiles();
                            boolean exists = profiles.contains(sanitized);
                            Runnable doSave = () -> {
                                boolean success = net.steve.powerhud.PowerHudConfig.saveProfile(trimmed);
                                if (success) showFeedback("Profile saved: " + trimmed, 0xFF22CC22);
                                else showFeedback("Failed to save profile", 0xFFCC2222);
                                clearAndInit();
                            };
                            if (exists) {
                                this.client.setScreen(new ConfirmOverwriteProfileScreen(this, trimmed, doSave));
                            } else {
                                doSave.run();
                            }
                        }
                    }
                ).dimensions(profileRX, profileY, btnW, btnH).build();
                saveBtn.active = true;
                addDrawableChild(saveBtn);
                profileY += leading;

                List<String> profiles = net.steve.powerhud.PowerHudConfig.listProfiles();
                for (String profileName : profiles) {
                    int labelW = btnW - 3 * BUTTON_WIDTH_COMPACT - 3 * gap;
                    boolean isCurrent = profileName.equals(net.steve.powerhud.PowerHudConfig.currentProfile);
                    if (isCurrent) {
                        addDrawableChild(ButtonWidget.builder(
                            Text.literal("[" + profileName + "]").styled(s -> s.withColor(0xFF22CC22).withBold(true)),
                            b -> {}
                        ).dimensions(profileRX, profileY, labelW, btnH).build()).active = false;
                        tooltips.add(new TooltipArea(profileRX, profileY, labelW, btnH, "This is the currently loaded profile"));
                    } else {
                        addLabel(profileRX, profileY, profileName, labelW, btnH);
                        tooltips.add(new TooltipArea(profileRX, profileY, labelW, btnH, "Profile: " + profileName));
                    }
                    int btnLoadX = profileRX + labelW + gap;
                    int btnRenameX = btnLoadX + BUTTON_WIDTH_COMPACT + gap;
                    int btnDelX = btnRenameX + BUTTON_WIDTH_COMPACT + gap;
                    tooltips.add(new TooltipArea(btnLoadX, profileY, BUTTON_WIDTH_COMPACT, btnH, "Load this profile"));
                    addButton(btnLoadX, profileY, "L", BUTTON_WIDTH_COMPACT, btnH, b -> {
                        net.steve.powerhud.PowerHudConfig.loadProfile(profileName);
                        clearAndInit();
                    });
                    tooltips.add(new TooltipArea(btnRenameX, profileY, BUTTON_WIDTH_COMPACT, btnH, "Rename this profile"));
                    addButton(btnRenameX, profileY, "R", BUTTON_WIDTH_COMPACT, btnH, b -> {
                        this.client.setScreen(new RenameProfileScreen(this, profileName, this::clearAndInit));
                    });
                    tooltips.add(new TooltipArea(btnDelX, profileY, BUTTON_WIDTH_COMPACT, btnH, "Delete this profile"));
                    addButton(btnDelX, profileY, "x", BUTTON_WIDTH_COMPACT, btnH, b -> {
                        this.client.setScreen(new ConfirmDeleteProfileScreen(this, profileName));
                    });
                    profileY += leading;
                }
                break;
                
            case ELEMENTS:
                addBool(rX, startY, "FPS", PowerHudConfig.showFps, 
                    v -> PowerHudConfig.showFps = v, hW, btnH, "Toggle FPS");
                addBool(rX + hW + gap, startY, "XYZ", PowerHudConfig.showCoords, 
                    v -> PowerHudConfig.showCoords = v, hW, btnH, "Toggle Co-ordinates");
                tooltips.add(new TooltipArea(profileRX, profileY, btnW, btnH, "Enter a new profile name here"));
                
                addBool(rX, startY + leading, "Facing", PowerHudConfig.showDirection, 
                    v -> PowerHudConfig.showDirection = v, hW, btnH, "Toggle Direction");
                addBool(rX + hW + gap, startY + leading, "Biome", PowerHudConfig.showBiome, 
                    v -> PowerHudConfig.showBiome = v, hW, btnH, "Toggle Biome");
                
                addBool(rX, startY + (leading * 2), "Time", PowerHudConfig.showTime, 
                    v -> PowerHudConfig.showTime = v, hW, btnH, "Toggle Time");
                addBool(rX + hW + gap, startY + (leading * 2), "Vitality", PowerHudConfig.showVitality, 
                    v -> PowerHudConfig.showVitality = v, hW, btnH, "Toggle Vitality");
                
                addBool(rX, startY + (leading * 3), "Block", PowerHudConfig.showBlock, 
                    v -> PowerHudConfig.showBlock = v, hW, btnH, "Toggle Block Info");
                addBool(rX + hW + gap, startY + (leading * 3), "Best Tool", PowerHudConfig.showBestTool, 
                    v -> PowerHudConfig.showBestTool = v, hW, btnH, "Recommend tool for block");
                
                addBool(rX, startY + (leading * 4), "Inventory", PowerHudConfig.showInventory, 
                    v -> PowerHudConfig.showInventory = v, hW, btnH, "Toggle Inventory");
                addBool(rX + hW + gap, startY + (leading * 4), "Oxygen Overlay", PowerHudConfig.showOxygen,
                    v -> PowerHudConfig.showOxygen = v, hW, btnH, "Toggle Centered Oxygen Display");

                addBool(rX, startY + (leading * 5), "Vanilla Air", !PowerHudConfig.hideVanillaOxygen, 
                    v -> PowerHudConfig.hideVanillaOxygen = !v, hW, btnH, "Show/Hide Default Air Bubbles");
                addStepper(rX + hW + gap, startY + (leading * 5), "Oxygen Height", PowerHudConfig.oxygenOverlayY,
                    "Distance from bottom", v -> PowerHudConfig.oxygenOverlayY = Math.max(10, Math.min(200, v)), hW, btnH, 5);

                addBool(rX, startY + (leading * 6), "Block Stats", PowerHudConfig.showBlockStats,
                    v -> PowerHudConfig.showBlockStats = v, hW, btnH, "Track session blocks");
                addBool(rX + hW + gap, startY + (leading * 6), "Gamemode", PowerHudConfig.showGamemode,
                    v -> PowerHudConfig.showGamemode = v, hW, btnH, "Show current game mode");
                break;
                
            case THEME:
                // ...existing toggles...
                                // Move color preset selector to the bottom for alignment
                                addToggle(
                                    rX, startY + (leading * 5),
                                    "Accessibility",
                                    PowerHudConfig.COLOR_PRESET_NAMES[PowerHudConfig.colorPresetIndex],
                                    "Switch accessibility preset (color blindness, high contrast, dyslexia)",
                                    b -> {
                                        int next = (PowerHudConfig.colorPresetIndex + 1) % PowerHudConfig.COLOR_PRESET_NAMES.length;
                                        PowerHudConfig.setColorPreset(next);
                                        clearAndInit();
                                    },
                                    btnW, btnH
                                );
                addToggle(
                    rX, startY,
                    "HUD Font",
                    PowerHudConfig.FONT_NAMES[PowerHudConfig.fontIndex],
                    "Change global HUD font",
                    b -> {
                        PowerHudConfig.fontIndex = (PowerHudConfig.fontIndex + 1) % PowerHudConfig.FONT_NAMES.length;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + leading,
                    "Background",
                    (PowerHudConfig.boxStyle != null ? PowerHudConfig.boxStyle : PowerHudConfig.BoxStyle.OFF).toString(),
                    "HUD element background style",
                    b -> {
                        PowerHudConfig.boxStyle = PowerHudConfig.BoxStyle.values()[
                            ((PowerHudConfig.boxStyle != null ? PowerHudConfig.boxStyle : PowerHudConfig.BoxStyle.OFF).ordinal() + 1) % PowerHudConfig.BoxStyle.values().length
                        ];
                        PowerHudConfig.save(); // Ensure config is saved immediately after change
                        clearAndInit();
                    },
                    btnW, btnH
                );

                // Data Color toggle: only enabled if custom colors
                addToggle(
                    rX, startY + (leading * 2),
                    "Data Color",
                    PowerHudConfig.COLOR_NAMES[PowerHudConfig.themeIndex] + (PowerHudConfig.isCustomColors() ? "" : " (Preset)"),
                    PowerHudConfig.isCustomColors() ? "HUD data colour" : "Disabled by preset",
                    b -> {
                        if (PowerHudConfig.isCustomColors()) {
                            PowerHudConfig.themeIndex = (PowerHudConfig.themeIndex + 1) % PowerHudConfig.COLORS.length;
                            clearAndInit();
                        }
                    },
                    btnW, btnH
                );
                // Title Color toggle: only enabled if custom colors
                addToggle(
                    rX, startY + (leading * 3),
                    "Title Color",
                    PowerHudConfig.COLOR_NAMES[PowerHudConfig.titleColorIndex] + (PowerHudConfig.isCustomColors() ? "" : " (Preset)"),
                    PowerHudConfig.isCustomColors() ? "HUD title colour" : "Disabled by preset",
                    b -> {
                        if (PowerHudConfig.isCustomColors()) {
                            PowerHudConfig.titleColorIndex = (PowerHudConfig.titleColorIndex + 1) % PowerHudConfig.COLORS.length;
                            clearAndInit();
                        }
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + (leading * 4),
                    "Bold Titles",
                    PowerHudConfig.boldTitles ? "ON" : "OFF",
                    "HUD title bold?",
                    b -> {
                        PowerHudConfig.boldTitles = !PowerHudConfig.boldTitles;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                break;
                
            case FPS_TWEAK:
                addToggle(
                    rX, startY,
                    "Mode",
                    PowerHudConfig.fpsMode.toString(),
                    "FPS Complexity",
                    b -> {
                        PowerHudConfig.fpsMode = PowerHudConfig.FpsMode.values()[
                            (PowerHudConfig.fpsMode.ordinal() + 1) % 3
                        ];
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + leading,
                    "FPS Dot",
                    PowerHudConfig.showFpsDot ? "ON" : "OFF",
                    "Show Status Indicator",
                    b -> {
                        PowerHudConfig.showFpsDot = !PowerHudConfig.showFpsDot;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addStepper(
                    rX, startY + (leading * 2),
                    "Red <",
                    PowerHudConfig.redThresh,
                    "Low FPS Threshold",
                    val -> PowerHudConfig.setRed(val),
                    btnW, btnH, FPS_THRESHOLD_MIN_GAP
                );
                
                addStepper(
                    rX, startY + (leading * 3),
                    "Orange <",
                    PowerHudConfig.orangeThresh,
                    "Medium FPS Threshold",
                    val -> PowerHudConfig.setOrange(val),
                    btnW, btnH, FPS_THRESHOLD_MIN_GAP
                );
                
                addStepper(
                    rX, startY + (leading * 4),
                    "Yellow <",
                    PowerHudConfig.yellowThresh,
                    "High FPS Threshold",
                    val -> PowerHudConfig.setYellow(val),
                    btnW, btnH, FPS_THRESHOLD_MIN_GAP
                );
                break;
                
            case INV_TWEAK:
                addToggle(
                    rX, startY,
                    "Inv Style",
                    PowerHudConfig.inventoryMode.toString(),
                    "Percent, value or boxes",
                    b -> {
                        PowerHudConfig.inventoryMode = PowerHudConfig.InventoryMode.values()[
                            (PowerHudConfig.inventoryMode.ordinal() + 1) % 3
                        ];
                        clearAndInit();
                    },
                    btnW, btnH
                );
                break;
                
                // ...existing code...
            case ABOUT:
                // Move rendering logic to render() method
                break;
        }


        // Done button at bottom
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            b -> close()
        ).dimensions(mid - SCREEN_BUTTON_CENTER, height - SCREEN_BUTTON_BOTTOM, BUTTON_WIDTH_SMALL, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float t) {
        super.render(dc, mx, my, t);

        dc.drawCenteredTextWithShadow(
            this.textRenderer,
            "PowerHUD Settings",
            this.width / 2,
            SCREEN_TITLE_Y,
            COLOR_TEXT_WHITE
        );

        // Render tooltips
        for (TooltipArea tip : tooltips) {
            if (mx >= tip.x && mx <= tip.x + tip.w && my >= tip.y && my <= tip.y + tip.h) {
                dc.drawTooltip(
                    this.textRenderer,
                    Text.literal(tip.text),
                    mx - 160,
                    my - 10
                );
            }
        }

        // Render feedback message (centered box)
        if (feedbackMessage != null && feedbackTicks > 0) {
            int boxW = 260;
            int boxH = 40;
            int x = (this.width - boxW) / 2;
            int y = (this.height - boxH) / 2;
            int color = 0xCC222222;
            dc.fill(x, y, x + boxW, y + boxH, color);
            dc.drawCenteredTextWithShadow(this.textRenderer, feedbackMessage, this.width / 2, y + 14, 0xFFFFFFAA);
        }

        // Render ABOUT section content
        if (currentCategory == Category.ABOUT) {
            int rX = this.width / 2 + SCREEN_CENTER_OFFSET;
            int startY = UI_START_Y;
            int p = SPACING_PADDING;
            int boxW = ABOUT_BOX_WIDTH;
            int boxH = ABOUT_BOX_HEIGHT;

            dc.fill(rX, startY, rX + boxW, startY + boxH, COLOR_BACKGROUND_DARK);

            dc.drawTextWithShadow(
                this.textRenderer,
                "PowerHUD",
                rX + p,
                startY + p,
                COLOR_TEXT_GOLD
            );

            dc.drawTextWithShadow(
                this.textRenderer,
                getModVersion(),
                rX + p,
                startY + p + ABOUT_TEXT_SPACING,
                COLOR_TEXT_GRAY
            );

            String[] lines = {
                "PowerHud is customizable HUD",
                "providing extra info if required.",
                "Please support the developer scan",
                "QR code below to donate towards",
                "a coffee."
            };

            int ty = startY + p + ABOUT_TEXT_OFFSET;
            for (String line : lines) {
                int lineW = this.textRenderer.getWidth(line);
                dc.drawTextWithShadow(
                    this.textRenderer,
                    line,
                    rX + (boxW / 2) - (lineW / 2),
                    ty,
                    COLOR_TEXT_LIGHT_GRAY
                );
                ty += ABOUT_TEXT_SPACING;
            }

            ty += 6;
            String sStr = "Support - watkins.steve@gmail.com";
            int sW = this.textRenderer.getWidth(sStr);
            dc.drawTextWithShadow(
                this.textRenderer,
                sStr,
                rX + (boxW / 2) - (sW / 2),
                ty,
                COLOR_TEXT_DARK_GRAY
            );

            Identifier qr = Identifier.of("powerhud", "textures/coffee_qr.png");
            dc.drawTexture(
                RenderLayer::getGuiTextured,
                qr,
                rX + (boxW / 2) - (ABOUT_QR_SIZE / 2),
                startY + boxH - ABOUT_QR_OFFSET,
                0.0f,
                0.0f,
                ABOUT_QR_SIZE,
                ABOUT_QR_SIZE,
                ABOUT_QR_SIZE,
                ABOUT_QR_SIZE
            );
        }
    }
    
    @Override
    protected void clearAndInit() {
        clearChildren();
        profileNameField = null;
        init();
    }
    
    private void addTab(int x, int y, String label, Category cat) {
        boolean active = (currentCategory == cat);
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label),
            b -> {
                currentCategory = cat;
                clearAndInit();
            }
        ).dimensions(x, y, BUTTON_WIDTH_TAB, BUTTON_HEIGHT).build()).active = !active;
    }
    
    private void addToggle(
        int x, int y,
        String label, String val, String tip,
        Consumer<ButtonWidget> action,
        int w, int h
    ) {
        tooltips.add(new TooltipArea(x, y, w, h, tip));
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label + ": " + val),
            action::accept
        ).dimensions(x, y, w, h).build());
    }
    
    private void addBool(
        int x, int y,
        String label, boolean val,
        Consumer<Boolean> setter,
        int w, int h, String tip
    ) {
        tooltips.add(new TooltipArea(x, y, w, h, tip));
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label + ": " + (val ? "ON" : "OFF")),
            b -> {
                setter.accept(!val);
                clearAndInit();
            }
        ).dimensions(x, y, w, h).build());
    }
    
    private void addStepper(
        int x, int y,
        String label, int val, String tip,
        Consumer<Integer> setter,
        int w, int h, int step
    ) {
        tooltips.add(new TooltipArea(x, y, w - 24, h, tip));
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label + " " + val),
            b -> {}
        ).dimensions(x, y, w - 24, h).build()).active = false;
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("-"),
            b -> {
                setter.accept(val - step);
                clearAndInit();
            }
        ).dimensions(x + w - 22, y, BUTTON_WIDTH_MINI, h).build());
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("+"),
            b -> {
                setter.accept(val + step);
                clearAndInit();
            }
        ).dimensions(x + w - 11, y, BUTTON_WIDTH_MINI, h).build());
    }

    private void addLabel(int x, int y, String text, int w, int h) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(text),
            b -> {}
        ).dimensions(x, y, w, h).build()).active = false;
    }


    private void addButton(int x, int y, String label, int w, int h, Consumer<ButtonWidget> action) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label),
            action::accept
        ).dimensions(x, y, w, h).build());
    }
}
