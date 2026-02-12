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
    private enum Category { 
        DISPLAY, ELEMENTS, THEME, FPS_TWEAK, INV_TWEAK, ABOUT, PROFILES
    }
    
    private static Category currentCategory = Category.DISPLAY;
    private final List<TooltipArea> tooltips = new ArrayList<>();
    private final String modVer;
    private String profileInput = null;
    private TextFieldWidget profileNameField;

    public PowerHudConfigScreen() {
        super(Text.literal("PowerHUD Config"));
        this.modVer = FabricLoader.getInstance()
            .getModContainer("powerhud")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
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
                
            case ELEMENTS:
                addBool(rX, startY, "FPS", PowerHudConfig.showFps, 
                    v -> PowerHudConfig.showFps = v, hW, btnH, "Toggle FPS");
                addBool(rX + hW + gap, startY, "XYZ", PowerHudConfig.showCoords, 
                    v -> PowerHudConfig.showCoords = v, hW, btnH, "Toggle Co-ordinates");
                
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
                    PowerHudConfig.boxStyle.toString(),
                    "HUD element background style",
                    b -> {
                        PowerHudConfig.boxStyle = PowerHudConfig.BoxStyle.values()[
                            (PowerHudConfig.boxStyle.ordinal() + 1) % PowerHudConfig.BoxStyle.values().length
                        ];
                        PowerHudConfig.save(); // Ensure config is saved immediately after change
                        clearAndInit();
                    },
                    btnW, btnH
                );

                addToggle(
                    rX, startY + (leading * 2),
                    "Data Color",
                    PowerHudConfig.COLOR_NAMES[PowerHudConfig.themeIndex],
                    "HUD data colour",
                    b -> {
                        PowerHudConfig.themeIndex = (PowerHudConfig.themeIndex + 1) % PowerHudConfig.COLORS.length;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + (leading * 3),
                    "Title Color",
                    PowerHudConfig.COLOR_NAMES[PowerHudConfig.titleColorIndex],
                    "HUD title colour",
                    b -> {
                        PowerHudConfig.titleColorIndex = (PowerHudConfig.titleColorIndex + 1) % PowerHudConfig.COLORS.length;
                        clearAndInit();
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
                
            case PROFILES:
                addLabel(profileRX, profileY, "HUD Profiles", btnW, btnH);
                profileY += leading;
                if (profileNameField == null) {
                    profileNameField = new TextFieldWidget(this.textRenderer, profileRX, profileY, btnW, btnH, Text.literal("Profile Name"));
                    profileNameField.setText("");
                }
                profileNameField.setText(this.profileInput != null ? this.profileInput : "");
                addDrawableChild(profileNameField);
                profileY += leading;
                addButton(profileRX, profileY, "Save Current", btnW, btnH, b -> {
                    this.profileInput = profileNameField.getText();
                    if (this.profileInput != null && !this.profileInput.isEmpty()) {
                        PowerHudConfig.saveProfile(this.profileInput);
                        clearAndInit();
                    }
                });
                profileY += leading;
                List<String> profiles = PowerHudConfig.listProfiles();
                for (String profileName : profiles) {
                    addLabel(profileRX, profileY, profileName, btnW, btnH);
                    addButton(profileRX + btnW + gap, profileY, "Load", hW, btnH, b -> {
                        PowerHudConfig.loadProfile(profileName);
                        clearAndInit();
                    });
                    addButton(profileRX + btnW + gap + hW + gap, profileY, "Delete", hW, btnH, b -> {
                        PowerHudConfig.deleteProfile(profileName);
                        clearAndInit();
                    });
                    profileY += leading;
                }
                break;

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
                modVer,
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

    private void addTextInput(int x, int y, String label, String def, int w, int h, Consumer<String> onSubmit) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label + ": " + def),
            b -> {}
        ).dimensions(x, y, w, h).build()).active = false;

        // TODO: Implement actual text input field and logic
    }

    private void addButton(int x, int y, String label, int w, int h, Consumer<ButtonWidget> action) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label),
            action::accept
        ).dimensions(x, y, w, h).build());
    }
}
