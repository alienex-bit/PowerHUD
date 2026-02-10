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

public class PowerHudConfigScreen extends Screen {
    private enum Category { 
        DISPLAY, ELEMENTS, THEME, FPS_TWEAK, INV_TWEAK, ABOUT 
    }
    
    private static Category currentCategory = Category.DISPLAY;
    private final List<TooltipArea> tooltips = new ArrayList<>();
    private final String modVer;
    
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
        int startY = 35;
        int leading = 16;
        int btnH = 14;
        int btnW = 150;
        int rX = mid + 5;
        int lX = mid - 115;
        int hW = 95;
        int gap = 4;
        
        // Left side tabs
        addTab(lX, startY, "Display", Category.DISPLAY);
        addTab(lX, startY + leading, "HUD Elements", Category.ELEMENTS);
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("HUD Element Order"),
            b -> { this.client.setScreen(new HudOrderScreen(this)); }
        ).dimensions(lX, startY + (leading * 2), 110, 16).build());
        
        addTab(lX, startY + (leading * 3), "Theme", Category.THEME);
        addTab(lX, startY + (leading * 4), "FPS Tweak", Category.FPS_TWEAK);
        addTab(lX, startY + (leading * 5), "Inventory Tweak", Category.INV_TWEAK);
        addTab(lX, startY + (leading * 6), "About", Category.ABOUT);
        
        // Right side content based on category
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
                        if(val >= 50 && val <= 250) {
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
                addBool(rX + hW + gap, startY + (leading * 4), "Oxygen", PowerHudConfig.showOxygen, 
                    v -> PowerHudConfig.showOxygen = v, hW, btnH, "Toggle Oxygen Info");
                
                addBool(rX, startY + (leading * 5), "Vanilla Air", !PowerHudConfig.hideVanillaOxygen, 
                    v -> PowerHudConfig.hideVanillaOxygen = !v, hW, btnH, "Show/Hide Default Air Bubbles");
                addBool(rX + hW + gap, startY + (leading * 5), "Block Stats", PowerHudConfig.showBlockStats, 
                    v -> PowerHudConfig.showBlockStats = v, hW, btnH, "Track session blocks");
                
                addBool(rX, startY + (leading * 6), "Gamemode", PowerHudConfig.showGamemode, 
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
                    "Shadow Level",
                    PowerHudConfig.boxStyle.toString(),
                    "HUD background style",
                    b -> {
                        PowerHudConfig.boxStyle = PowerHudConfig.BoxStyle.values()[
                            (PowerHudConfig.boxStyle.ordinal() + 1) % PowerHudConfig.BoxStyle.values().length
                        ];
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + (leading * 2),
                    "Soft Corners",
                    PowerHudConfig.roundCorners ? "ON" : "OFF",
                    "Adds corners to HUD boxes",
                    b -> {
                        PowerHudConfig.roundCorners = !PowerHudConfig.roundCorners;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + (leading * 3),
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
                    rX, startY + (leading * 4),
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
                    rX, startY + (leading * 5),
                    "Bold Titles",
                    PowerHudConfig.boldTitles ? "ON" : "OFF",
                    "HUD title bold?",
                    b -> {
                        PowerHudConfig.boldTitles = !PowerHudConfig.boldTitles;
                        clearAndInit();
                    },
                    btnW, btnH
                );
                
                addToggle(
                    rX, startY + (leading * 6),
                    "Visual FX",
                    PowerHudConfig.textEffect.toString(),
                    "Applies HUD text effects",
                    b -> {
                        PowerHudConfig.textEffect = PowerHudConfig.TextEffect.values()[
                            (PowerHudConfig.textEffect.ordinal() + 1) % PowerHudConfig.TextEffect.values().length
                        ];
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
                    btnW, btnH, 5
                );
                
                addStepper(
                    rX, startY + (leading * 3),
                    "Orange <",
                    PowerHudConfig.orangeThresh,
                    "Medium FPS Threshold",
                    val -> PowerHudConfig.setOrange(val),
                    btnW, btnH, 5
                );
                
                addStepper(
                    rX, startY + (leading * 4),
                    "Yellow <",
                    PowerHudConfig.yellowThresh,
                    "High FPS Threshold",
                    val -> PowerHudConfig.setYellow(val),
                    btnW, btnH, 5
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
                
            case ABOUT:
                break;
        }
        
        // Done button at bottom
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            b -> close()
        ).dimensions(mid - 50, height - 65, 100, 14).build());
    }
    
    @Override
    public void render(DrawContext dc, int mx, int my, float t) {
        super.render(dc, mx, my, t);
        
        dc.drawCenteredTextWithShadow(
            this.textRenderer,
            "PowerHUD Settings",
            this.width / 2,
            15,
            0xFFFFFFFF
        );
        
        if (currentCategory == Category.ABOUT) {
            int rX = this.width / 2 + 5;
            int startY = 35;
            int p = 8;
            int boxW = 190;
            int boxH = 160;
            
            dc.fill(rX, startY, rX + boxW, startY + boxH, 0xCC000000);
            
            dc.drawTextWithShadow(
                this.textRenderer,
                "PowerHUD",
                rX + p,
                startY + p,
                0xFFFFD700
            );
            
            dc.drawTextWithShadow(
                this.textRenderer,
                modVer,
                rX + p,
                startY + p + 11,
                0xFFAAAAAA
            );
            
            String[] lines = {
                "PowerHud is customizable HUD",
                "providing extra info if required.",
                "Please support the developer scan",
                "QR code below to donate towards",
                "a coffee."
            };
            
            int ty = startY + p + 26;
            for (String line : lines) {
                int lineW = this.textRenderer.getWidth(line);
                dc.drawTextWithShadow(
                    this.textRenderer,
                    line,
                    rX + (boxW / 2) - (lineW / 2),
                    ty,
                    0xFFBBBBBB
                );
                ty += 11;
            }
            
            ty += 6;
            String sStr = "Support - watkins.steve@gmail.com";
            int sW = this.textRenderer.getWidth(sStr);
            dc.drawTextWithShadow(
                this.textRenderer,
                sStr,
                rX + (boxW / 2) - (sW / 2),
                ty,
                0xFF777777
            );
            
            Identifier qr = Identifier.of("powerhud", "textures/coffee_qr.png");
            dc.drawTexture(
                RenderLayer::getGuiTextured,
                qr,
                rX + (boxW / 2) - 22,
                startY + boxH - 52,
                0.0f,
                0.0f,
                44,
                44,
                44,
                44
            );
        }
        
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
    }
    
    @Override
    protected void clearAndInit() {
        clearChildren();
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
        ).dimensions(x, y, 110, 14).build()).active = !active;
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
        ).dimensions(x + w - 22, y, 10, h).build());
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("+"),
            b -> {
                setter.accept(val + step);
                clearAndInit();
            }
        ).dimensions(x + w - 11, y, 10, h).build());
    }
}