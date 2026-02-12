package net.steve.powerhud;

/**
 * Centralized constants for PowerHUD
 * Eliminates magic numbers and improves code maintainability
 */
public class HudConstants {
    
    // ==================== UI COLORS ====================
    
    // Background colors (with alpha) - Used for UI screens and special elements
    public static final int COLOR_BACKGROUND_DARK = 0xCC000000;
    public static final int COLOR_BACKGROUND_OVERLAY = 0x77000000;
    public static final int COLOR_BACKGROUND_SEMI = 0xA5000000;

    // Border/separator colors
    public static final int COLOR_SEPARATOR = 0x33FFFFFF;
    public static final int COLOR_BORDER_LIGHT = 0x44FFFFFF;
    
    // Text colors
    public static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    public static final int COLOR_TEXT_GOLD = 0xFFFFD700;
    public static final int COLOR_TEXT_GRAY = 0xFFAAAAAA;
    public static final int COLOR_TEXT_DARK_GRAY = 0xFF777777;
    public static final int COLOR_TEXT_LIGHT_GRAY = 0xFFBBBBBB;
    public static final int COLOR_TEXT_BLACK = 0xFF000000;
    
    // Status colors
    public static final int COLOR_GREEN = 0xFF55FF55;
    public static final int COLOR_RED = 0xFFFF5555;
    public static final int COLOR_YELLOW = 0xFFFFFF55;
    public static final int COLOR_ORANGE = 0xFFFFAA00;
    public static final int COLOR_CYAN = 0xFF00FFFF;
    
    // ==================== UI DIMENSIONS ====================
    
    // Screen positioning
    public static final int UI_START_Y = 35;
    public static final int UI_COLUMN_START_Y = 85;
    public static final int UI_HEADER_Y = 72;
    public static final int UI_BOTTOM_OFFSET = 40;
    public static final int UI_SEPARATOR_Y = 65;
    
    // Widget dimensions
    public static final int BUTTON_HEIGHT = 14;
    public static final int BUTTON_HEIGHT_TALL = 16;
    public static final int BUTTON_HEIGHT_ELEMENT = 15;
    public static final int BUTTON_WIDTH_STANDARD = 150;
    public static final int BUTTON_WIDTH_HALF = 95;
    public static final int BUTTON_WIDTH_TAB = 110;
    public static final int BUTTON_WIDTH_SMALL = 100;
    public static final int BUTTON_WIDTH_ACTION = 120;
    public static final int BUTTON_WIDTH_MINI = 10;
    public static final int BUTTON_WIDTH_TINY = 14;
    public static final int BUTTON_WIDTH_COMPACT = 15;
    
    // Spacing
    public static final int SPACING_LEADING = 16;
    public static final int SPACING_GAP = 4;
    public static final int SPACING_PADDING = 8;
    public static final int SPACING_HUD_TOP = 5;
    public static final int SPACING_MINI = 2;
    
    // About box dimensions
    public static final int ABOUT_BOX_WIDTH = 190;
    public static final int ABOUT_BOX_HEIGHT = 160;
    public static final int ABOUT_QR_SIZE = 44;
    public static final int ABOUT_QR_OFFSET = 52;
    public static final int ABOUT_TEXT_SPACING = 11;
    public static final int ABOUT_TEXT_OFFSET = 26;
    
    // Inventory grid
    public static final int INV_GRID_WIDTH = 62;
    public static final int INV_GRID_HEIGHT = 31;
    public static final int INV_SLOT_SIZE = 5;
    public static final int INV_SLOT_SPACING = 7;
    public static final int INV_GRID_OFFSET = 11;
    public static final int INV_ROWS = 3;
    public static final int INV_COLS = 9;
    public static final int INV_HEIGHT_TOTAL = 35;
    
    // Oxygen bar
    public static final int OXY_BAR_HEIGHT = 22;
    public static final int OXY_BAR_OFFSET = 10;
    
    // HUD rendering
    public static final int HUD_LINE_HEIGHT = 9;
    public static final int HUD_ICON_WIDTH = 18;
    public static final int HUD_ICON_OFFSET = 4;
    public static final int HUD_DOT_WIDTH = 10;
    public static final int HUD_DOT_SIZE = 4;
    public static final int HUD_PADDING = 5;
    
    // Screen layout offsets
    public static final int SCREEN_CENTER_OFFSET = 5;
    public static final int SCREEN_LEFT_OFFSET = 115;
    public static final int SCREEN_BUTTON_BOTTOM = 65;
    public static final int SCREEN_BUTTON_CENTER = 50;
    public static final int SCREEN_TITLE_Y = 15;
    
    // ==================== THRESHOLDS & LIMITS ====================
    
    // FPS thresholds (defaults)
    public static final int FPS_THRESHOLD_RED_DEFAULT = 30;
    public static final int FPS_THRESHOLD_ORANGE_DEFAULT = 60;
    public static final int FPS_THRESHOLD_YELLOW_DEFAULT = 100;
    public static final int FPS_THRESHOLD_MIN_GAP = 5;
    
    // Scale limits
    public static final int SCALE_MIN = 50;
    public static final int SCALE_MAX = 250;
    public static final int SCALE_DEFAULT = 100;
    public static final float SCALE_DIVISOR = 100f;
    
    // Line length limits
    public static final int MAX_LINE_LENGTH = 120;
    
    // ==================== ANIMATION & TIMING ====================
    
    // FPS dot pulse animation
    public static final double PULSE_SPEED = 100.0;
    public static final float PULSE_BASE = 1.0f;
    public static final float PULSE_AMPLITUDE = 0.4f;
    public static final float PHASE_BASE = 0.5f;
    
    // ==================== TEXT RENDERING ====================
    
    // Color masks
    public static final int ALPHA_MASK = 0xFF000000;
    public static final int RGB_MASK = 0x00FFFFFF;
    public static final int RED_MASK = 0xFF;
    public static final int GREEN_SHIFT = 8;
    public static final int BLUE_SHIFT = 16;
    public static final int ALPHA_SHIFT = 24;
    
    // ==================== FPS DOT ====================
    
    public static final float FPS_DOT_SCALE = 1.5f;
    public static final int FPS_DOT_OFFSET_X = 4;
    public static final int FPS_DOT_OFFSET_Y = 4;
    public static final String FPS_DOT_CHAR = "\u25CF";
    
    // ==================== MATRIX TRANSFORMS ====================
    
    public static final float MATRIX_SCALE_Z = 1.0f;
    
    // ==================== COLUMN ALIGNMENT ====================
    
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;
    
    // ==================== DEFAULT STRINGS ====================
    
    public static final String TEXT_PREVIEW = "PREVIEW";
    public static final String TEXT_SPACE = "SPACE";
    public static final String TEXT_AIR = "Air";
    public static final String TEXT_OXYGEN_HOLDING = "Holding Breath...";
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if a value is within scale limits
     */
    public static boolean isValidScale(int scale) {
        return scale >= SCALE_MIN && scale <= SCALE_MAX;
    }
    
    /**
     * Get color with modified alpha
     */
    public static int withAlpha(int color, int alpha) {
        return (color & RGB_MASK) | (alpha << ALPHA_SHIFT);
    }
    
    /**
     * Extract RGB components
     */
    public static int getRed(int color) {
        return (color >> BLUE_SHIFT) & RED_MASK;
    }
    
    public static int getGreen(int color) {
        return (color >> GREEN_SHIFT) & RED_MASK;
    }
    
    public static int getBlue(int color) {
        return color & RED_MASK;
    }
    
    /**
     * Clamp value between min and max
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Get column alignment name
     */
    public static String getAlignmentName(int alignment) {
        return switch(alignment) {
            case ALIGN_LEFT -> "LEFT";
            case ALIGN_CENTER -> "CENTER";
            case ALIGN_RIGHT -> "RIGHT";
            default -> "UNKNOWN";
        };
    }
}