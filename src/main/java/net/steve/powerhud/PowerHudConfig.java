package net.steve.powerhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.steve.powerhud.HudConstants.*;

public class PowerHudConfig {
    // Enums
    public enum FpsMode { MINIMAL, NORMAL, FULL }
    public enum BoxStyle { OFF, FAINT, LIGHT, SUBTLE, MEDIUM, STRONG, DARK, SOLID }
    public enum InventoryMode { GRID, PERCENT, FRACTION }
    
    // Color constants - 20 colors for comprehensive theming
    public static final int[] COLORS = {
        0xFFFFFFFF, // 0  - White
        0xFF55FF55, // 1  - Green (Lime)
        0xFFFFD700, // 2  - Gold
        0xFF00FFFF, // 3  - Cyan (Aqua)
        0xFFFF5555, // 4  - Red (Light)
        0xFFFFFF55, // 5  - Yellow (Bright)
        0xFF5555FF, // 6  - Blue (Fixed!)
        0xFFAA00AA, // 7  - Purple (Dark)
        0xFFFF55FF, // 8  - Magenta (Pink)
        0xFFAAAAAA, // 9  - Gray (Medium)
        0xFFFF8800, // 10 - Orange
        0xFF00AA88, // 11 - Teal
        0xFFAAFF00, // 12 - Lime (Bright)
        0xFFFF88DD, // 13 - Pink (Light)
        0xFFAADDFF, // 14 - Ice Blue
        0xFF8855FF, // 15 - Violet
        0xFFFFAA55, // 16 - Peach
        0xFF55FFAA, // 17 - Mint
        0xFFDD4444, // 18 - Crimson
        0xFF333333  // 19 - Charcoal
    };
    
    public static final String[] COLOR_NAMES = {
        "White", "Green", "Gold", "Cyan", "Red",
        "Yellow", "Blue", "Purple", "Magenta", "Gray",
        "Orange", "Teal", "Lime", "Pink", "Ice",
        "Violet", "Peach", "Mint", "Crimson", "Charcoal"
    };
    
    public static final String[] FONT_NAMES = {
        "Vanilla", "JetBrains", "Roboto", "Fira", "Cascadia", 
        "Source", "Comic", "Monofur", "Ubuntu", "Inter"
    };
    
    // Boolean settings
    public static boolean hudEnabled = true;
    public static boolean showFps = true;
    public static boolean showCoords = true;
    public static boolean showDirection = true;
    public static boolean showBiome = true;
    public static boolean showTime = true;
    public static boolean showVitality = true;
    public static boolean showBlock = true;
    public static boolean showInventory = true;
    public static boolean showOxygen = true;
    public static int oxygenOverlayY = 65; // Distance from bottom of screen
    public static boolean showGamemode = true;
    public static boolean fpsPulse = true;
    public static boolean boldTitles = false;
    public static boolean hideVanillaOxygen = false;
    public static boolean enableF3Replacement = false;
    public static boolean showFpsDot = true;
    public static boolean showBestTool = true;
    public static boolean showBlockStats = true;
    
    // Integer settings
    public static int debugTab = 0;
    public static int hudScaleVert = 100;
    public static int themeIndex = 0;
    public static int lineSpacing = 12;
    public static int fontIndex = 0;
    public static int titleColorIndex = 0;
    public static int redThresh = 30;
    public static int orangeThresh = 60;
    public static int yellowThresh = 100;
    
    // Enum settings
    public static FpsMode fpsMode = FpsMode.MINIMAL;
    public static BoxStyle boxStyle = BoxStyle.OFF; // Changed default to OFF for clean look
    public static InventoryMode inventoryMode = InventoryMode.GRID;
    
    // Layout settings
    public static List<LayoutEntry> hudOrder = new ArrayList<>();
    public static List<List<LayoutEntry>> layoutSlots = new ArrayList<>(
        Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())
    );
    
    public static class LayoutEntry {
        public String id;
        public int spacerHeight;
        public int alignment;
        public int x; // Absolute X position on screen
        public int y; // Absolute Y position on screen
        public boolean useFreeForm; // true = use x/y, false = use alignment/stack

        // Constructor for free-form positioning (new default)
        public LayoutEntry(String id, int x, int y, boolean isFreeForm) {
            this.id = id;
            this.spacerHeight = 0;
            this.alignment = 0;
            this.x = x;
            this.y = y;
            this.useFreeForm = isFreeForm;
        }

        // Convenience constructor for free-form
        public static LayoutEntry freeForm(String id, int x, int y) {
            return new LayoutEntry(id, x, y, true);
        }

        // Old constructor for backwards compatibility (alignment-based)
        public static LayoutEntry aligned(String id, int spacerHeight, int alignment) {
            LayoutEntry entry = new LayoutEntry(id, -1, -1, false);
            entry.spacerHeight = spacerHeight;
            entry.alignment = alignment;
            return entry;
        }
    }
    
    // Reset to default layout
    public static void resetToVanilla() {
        hudOrder.clear();
        // Left side elements (x=10)
        hudOrder.add(LayoutEntry.freeForm("XYZ", 10, 10));
        hudOrder.add(LayoutEntry.freeForm("FACING", 10, 25));
        hudOrder.add(LayoutEntry.freeForm("BIOME", 10, 40));
        hudOrder.add(LayoutEntry.freeForm("TIME", 10, 55));
        hudOrder.add(LayoutEntry.freeForm("VIT", 10, 70));
        hudOrder.add(LayoutEntry.freeForm("BLOCK", 10, 85));
        hudOrder.add(LayoutEntry.freeForm("GAMEMODE", 10, 100));
        // Right side elements (will be calculated relative to screen width)
        hudOrder.add(LayoutEntry.freeForm("FPS", -150, 10)); // negative = from right edge
        hudOrder.add(LayoutEntry.freeForm("BLOCK_STATS", -150, 35));
        hudOrder.add(LayoutEntry.freeForm("TOOL", -150, 50));
        hudOrder.add(LayoutEntry.freeForm("INV", -150, 65));
        save();
    }
    
    public static void saveSlot(int slot, List<LayoutEntry> current) {
        layoutSlots.set(slot, new ArrayList<>(current));
        save();
    }
    
    // Threshold setters with validation
    public static void setRed(Integer v) {
        if (v != null && v <= orangeThresh - 5) {
            redThresh = v;
        }
    }
    
    public static void setOrange(Integer v) {
        if (v != null && v >= redThresh + 5 && v <= yellowThresh - 5) {
            orangeThresh = v;
        }
    }
    
    public static void setYellow(Integer v) {
        if (v != null && v >= orangeThresh + 5) {
            yellowThresh = v;
        }
    }
    
    // File I/O
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("powerhud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static class ConfigData {
        // Boolean fields
        boolean hudEnabled = PowerHudConfig.hudEnabled;
        boolean showFps = PowerHudConfig.showFps;
        boolean showCoords = PowerHudConfig.showCoords;
        boolean showDirection = PowerHudConfig.showDirection;
        boolean showBiome = PowerHudConfig.showBiome;
        boolean showTime = PowerHudConfig.showTime;
        boolean showVitality = PowerHudConfig.showVitality;
        boolean showBlock = PowerHudConfig.showBlock;
        boolean showInventory = PowerHudConfig.showInventory;
        boolean showOxygen = PowerHudConfig.showOxygen;
        boolean showGamemode = PowerHudConfig.showGamemode;
        boolean hideVanillaOxygen = PowerHudConfig.hideVanillaOxygen;
        boolean enableF3Replacement = PowerHudConfig.enableF3Replacement;
        boolean showFpsDot = PowerHudConfig.showFpsDot;
        boolean showBestTool = PowerHudConfig.showBestTool;
        boolean showBlockStats = PowerHudConfig.showBlockStats;
        boolean fpsPulse = PowerHudConfig.fpsPulse;
        boolean boldTitles = PowerHudConfig.boldTitles;

        // Enum fields
        FpsMode fpsMode = PowerHudConfig.fpsMode;
        BoxStyle boxStyle = PowerHudConfig.boxStyle;
        InventoryMode inventoryMode = PowerHudConfig.inventoryMode;
        
        // Integer fields
        int hudScaleVert = PowerHudConfig.hudScaleVert;
        int themeIndex = PowerHudConfig.themeIndex;
        int lineSpacing = PowerHudConfig.lineSpacing;
        int redThresh = PowerHudConfig.redThresh;
        int orangeThresh = PowerHudConfig.orangeThresh;
        int yellowThresh = PowerHudConfig.yellowThresh;
        int fontIndex = PowerHudConfig.fontIndex;
        int titleColorIndex = PowerHudConfig.titleColorIndex;
        int oxygenOverlayY = PowerHudConfig.oxygenOverlayY;

        // Layout fields
        List<LayoutEntry> hudOrder = new ArrayList<>(PowerHudConfig.hudOrder);
        List<List<LayoutEntry>> layoutSlots = new ArrayList<>(PowerHudConfig.layoutSlots);
    }
    
    public static void load() {
        if (!CONFIG_FILE.toFile().exists()) {
            resetToVanilla();
            return;
        }
        
        try (Reader reader = new FileReader(CONFIG_FILE.toFile())) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                resetToVanilla();
                return;
            }
            
            // Apply boolean settings
            hudEnabled = data.hudEnabled;
            showFps = data.showFps;
            showCoords = data.showCoords;
            showDirection = data.showDirection;
            showBiome = data.showBiome;
            showTime = data.showTime;
            showVitality = data.showVitality;
            showBlock = data.showBlock;
            showInventory = data.showInventory;
            showOxygen = data.showOxygen;
            showGamemode = data.showGamemode;
            hideVanillaOxygen = data.hideVanillaOxygen;
            enableF3Replacement = data.enableF3Replacement;
            showFpsDot = data.showFpsDot;
            showBestTool = data.showBestTool;
            showBlockStats = data.showBlockStats;
            fpsPulse = data.fpsPulse;
            boldTitles = data.boldTitles;

            // Apply enum settings
            fpsMode = data.fpsMode;
            boxStyle = data.boxStyle;
            inventoryMode = data.inventoryMode;
            
            // Apply integer settings
            hudScaleVert = data.hudScaleVert;
            themeIndex = data.themeIndex;
            lineSpacing = data.lineSpacing;
            redThresh = data.redThresh;
            orangeThresh = data.orangeThresh;
            yellowThresh = data.yellowThresh;
            fontIndex = data.fontIndex;
            titleColorIndex = data.titleColorIndex;
            oxygenOverlayY = data.oxygenOverlayY;

            // Apply layout settings
            hudOrder = data.hudOrder != null ? data.hudOrder : new ArrayList<>();
            layoutSlots = data.layoutSlots != null ? data.layoutSlots : new ArrayList<>(
                Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())
            );
            
            if (hudOrder.isEmpty()) {
                resetToVanilla();
            }
        } catch (Exception e) {
            System.err.println("Failed to load PowerHUD config: " + e.getMessage());
            resetToVanilla();
        }
    }
    
    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE.toFile())) {
            ConfigData data = new ConfigData();
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Failed to save PowerHUD config: " + e.getMessage());
        }
    }
}