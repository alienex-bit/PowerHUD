package net.steve.powerhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import java.util.*;

import static net.steve.powerhud.HudConstants.*;

public class HudOrderScreen extends Screen {
    private final Screen parent;
    private final List<PowerHudConfig.LayoutEntry> tempOrder;
    public static boolean isWorkbenchActive = false;

    // Drag state
    private PowerHudConfig.LayoutEntry draggedElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean isDragging = false;

    // Palette state
    private static final int PALETTE_HEIGHT = 110;
    private static final int PALETTE_Y_OFFSET = 30;
    private static final int ELEMENT_BUTTON_WIDTH = 70;
    private static final int ELEMENT_BUTTON_HEIGHT = 18;
    private static final int ELEMENT_SPACING = 4;
    private static final int ELEMENTS_PER_ROW = 6;

    // Snap zones
    private static final int SNAP_THRESHOLD = 50;
    private PowerHudConfig.LayoutEntry hoveredPaletteElement = null;

    // Available elements that can be added
    private static final String[] ALL_ELEMENTS = {
        "FPS", "XYZ", "FACING", "BIOME", "TIME", "VIT",
        "BLOCK", "TOOL", "INV", "GAMEMODE", "BLOCK_STATS", "SPACE"
    };

    // Element descriptions for tooltips
    private static final Map<String, String> ELEMENT_TOOLTIPS = Map.ofEntries(
        Map.entry("FPS", "Frames Per Second display"),
        Map.entry("XYZ", "Current coordinates"),
        Map.entry("FACING", "Direction you're facing"),
        Map.entry("BIOME", "Current biome name"),
        Map.entry("TIME", "In-game time"),
        Map.entry("VIT", "Health and vitality"),
        Map.entry("BLOCK", "Block you're looking at"),
        Map.entry("TOOL", "Best tool for block"),
        Map.entry("INV", "Inventory grid display"),
        Map.entry("GAMEMODE", "Current game mode"),
        Map.entry("BLOCK_STATS", "Blocks mined/placed"),
        Map.entry("SPACE", "Spacer (adds vertical gap)")
    );

    private record ElementBounds(int x, int y, int width, int height) {}

    public HudOrderScreen(Screen parent) {
        super(Text.literal("HUD Workbench - WYSIWYG Editor"));
        this.parent = parent;
        this.tempOrder = new ArrayList<>();
        // Deep copy existing layout
        for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
            int normalizedX = normalizeX(entry.x);
            if (entry.useFreeForm) {
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, normalizedX, entry.y));
            } else {
                // Convert old alignment-based to free-form
                int x = calculateXFromAlignment(entry.alignment);
                int y = tempOrder.stream()
                    .filter(e -> e.alignment == entry.alignment)
                    .mapToInt(e -> e.y + 15)
                    .max()
                    .orElse(SPACING_HUD_TOP);
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, x, y));
            }
        }
        // Defer normalization until init() when textRenderer is available
    }

    private int calculateXFromAlignment(int alignment) {
        return switch(alignment) {
            case 0 -> HUD_X_LEFT_MARGIN; // LEFT
            case 1 -> HUD_X_CENTER_SENTINEL; // CENTER
            case 2 -> HUD_X_RIGHT_SENTINEL; // RIGHT
            default -> HUD_X_LEFT_MARGIN;
        };
    }

    private int normalizeX(int x) {
        if (x == HUD_X_CENTER_SENTINEL || x == HUD_X_RIGHT_SENTINEL) {
            return x;
        }
        if (x < 0) {
            return HUD_X_RIGHT_SENTINEL;
        }
        return HUD_X_LEFT_MARGIN;
    }

    private Zone zoneForEntry(PowerHudConfig.LayoutEntry entry) {
        if (entry.x == HUD_X_CENTER_SENTINEL) return Zone.CENTER;
        if (entry.x == HUD_X_RIGHT_SENTINEL || entry.x < 0) return Zone.RIGHT;
        return Zone.LEFT;
    }

    private Zone zoneForMouse(double mouseX) {
        int screenThird = this.width / 3;
        if (mouseX < screenThird) return Zone.LEFT;
        if (mouseX < screenThird * 2) return Zone.CENTER;
        return Zone.RIGHT;
    }

    private void setEntryZone(PowerHudConfig.LayoutEntry entry, Zone zone) {
        entry.x = switch(zone) {
            case LEFT -> HUD_X_LEFT_MARGIN;
            case CENTER -> HUD_X_CENTER_SENTINEL;
            case RIGHT -> HUD_X_RIGHT_SENTINEL;
        };
    }

    private List<PowerHudConfig.LayoutEntry> getZoneEntries(Zone zone) {
        List<PowerHudConfig.LayoutEntry> entries = new ArrayList<>();
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            if (zoneForEntry(entry) == zone) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private int getStackHeight(PowerHudConfig.LayoutEntry entry) {
        return getElementHeight(entry.id) + SPACING_MINI;
    }

    private void reflowZone(Zone zone) {
        List<PowerHudConfig.LayoutEntry> entries = getZoneEntries(zone);
        entries.sort(Comparator.comparingInt(e -> e.y));
        int y = SPACING_HUD_TOP;
        for (PowerHudConfig.LayoutEntry entry : entries) {
            entry.y = y;
            y += getStackHeight(entry);
        }
    }

    private void reflowZoneWithDrag(Zone zone, PowerHudConfig.LayoutEntry dragged, int mouseY) {
        List<PowerHudConfig.LayoutEntry> entries = getZoneEntries(zone);
        entries.remove(dragged);
        entries.sort(Comparator.comparingInt(e -> e.y));

        int insertIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            PowerHudConfig.LayoutEntry entry = entries.get(i);
            int midY = entry.y + (getElementHeight(entry.id) / 2);
            if (mouseY > midY) {
                insertIndex = i + 1;
            }
        }
        entries.add(insertIndex, dragged);

        int y = SPACING_HUD_TOP;
        for (PowerHudConfig.LayoutEntry entry : entries) {
            entry.y = y;
            y += getStackHeight(entry);
        }
    }

    private void normalizeAllZones() {
        reflowZone(Zone.LEFT);
        reflowZone(Zone.CENTER);
        reflowZone(Zone.RIGHT);
    }

    @Override
    protected void init() {
        isWorkbenchActive = true;
        this.clearChildren();
        
        // Normalize zones now that textRenderer is available
        normalizeAllZones();

        int bottomY = this.height - PALETTE_HEIGHT - 35;

        // Action buttons - centered and smaller
        int btnWidth = 80;
        int btnSpacing = 5;
        int totalWidth = (btnWidth * 4) + (btnSpacing * 3);
        int startX = (this.width - totalWidth) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
            PowerHudConfig.resetToVanilla();
            tempOrder.clear();
            for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, entry.x, entry.y));
            }
        }).dimensions(startX, bottomY, btnWidth, 18)
          .tooltip(Tooltip.of(Text.literal("Restore default layout")))
          .build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> {
            tempOrder.clear();
            clearChildren();
            init(); // Refresh the screen to update the display
        }).dimensions(startX + btnWidth + btnSpacing, bottomY, btnWidth, 18)
          .tooltip(Tooltip.of(Text.literal("Remove all elements")))
          .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(tempOrder);
            PowerHudConfig.save();
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(startX + (btnWidth + btnSpacing) * 2, bottomY, btnWidth, 18)
          .tooltip(Tooltip.of(Text.literal("Save and exit")))
          .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(startX + (btnWidth + btnSpacing) * 3, bottomY, btnWidth, 18)
          .tooltip(Tooltip.of(Text.literal("Exit without saving")))
          .build());
    }


    private ElementBounds getElementBounds(PowerHudConfig.LayoutEntry entry, TextRenderer ren, int sw) {
        if (entry.id.equals(TEXT_SPACE)) {
            return new ElementBounds(resolveX(entry.x, 60, sw), entry.y, 60, PowerHudConfig.lineSpacing);
        }

        boolean gridInv = entry.id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID;
        if (gridInv) {
            int titleW = HudRenderer.getHudTextWidth(ren, "Inventory", PowerHudConfig.boldTitles);
            int width = Math.max(titleW, INV_GRID_WIDTH);
            int x = resolveX(entry.x, width, sw);
            return new ElementBounds(x, entry.y, width, INV_HEIGHT_TOTAL);
        }

        String title = getTitleFor(entry.id);
        String value = getValueFor(entry.id);
        if (value.isEmpty()) {
            value = TEXT_PREVIEW;
        }

        int titleW = title.isEmpty() ? 0 : HudRenderer.getHudTextWidth(ren, title + ": ", PowerHudConfig.boldTitles);
        int valW = HudRenderer.getHudTextWidth(ren, value, false);
        int dotW = (entry.id.equals("FPS") && PowerHudConfig.showFpsDot) ? HUD_DOT_WIDTH : 0;
        int iconW = (entry.id.equals("TOOL") && !HudData.toolStack.isEmpty()) ? HUD_ICON_WIDTH : 0;
        int width = titleW + valW + dotW + iconW;
        int x = resolveX(entry.x, width, sw);
        return new ElementBounds(x, entry.y, width, ren.fontHeight);
    }

    private String getTitleFor(String id) {
        return switch(id) {
            case "FPS" -> "FPS";
            case "XYZ" -> "XYZ";
            case "FACING" -> "Facing";
            case "BIOME" -> "Biome";
            case "TIME" -> HudData.timeLabel;
            case "VIT" -> "Vitality";
            case "BLOCK" -> HudData.targetType;
            case "TOOL" -> "Best Tool";
            case "INV" -> "Inventory";
            case "BLOCK_STATS" -> "Blocks";
            case "GAMEMODE" -> "Mode";
            default -> "";
        };
    }

    private String getValueFor(String id) {
        return switch(id) {
            case "FPS" -> HudData.fpsStr;
            case "XYZ" -> HudData.coordsStr;
            case "FACING" -> HudData.dirStr;
            case "BIOME" -> HudData.biomeStr;
            case "TIME" -> HudData.timeStr;
            case "VIT" -> HudData.vitStr;
            case "BLOCK" -> HudData.blockStr;
            case "TOOL" -> HudData.toolStr;
            case "INV" -> HudData.invStr;
            case "BLOCK_STATS" -> HudData.blockStatsStr;
            case "GAMEMODE" -> HudData.gamemodeStr;
            default -> "";
        };
    }

    private int resolveX(int x, int width, int sw) {
        if (x == HUD_X_CENTER_SENTINEL) {
            return (sw - width) / 2;
        }
        if (x == HUD_X_RIGHT_SENTINEL) {
            return sw - width - HUD_X_RIGHT_MARGIN;
        }
        if (x < 0) {
            return sw + x;
        }
        return x;
    }

    private int getElementHeight(String id) {
        if (id.equals("SPACE")) {
            return PowerHudConfig.lineSpacing;
        }
        if (id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID) {
            return INV_HEIGHT_TOTAL;
        }
        // Safe fallback if textRenderer not yet initialized
        return this.textRenderer != null ? this.textRenderer.fontHeight : HUD_LINE_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
            int hudMouseX = (int)(mouseX / s);
            int hudMouseY = (int)(mouseY / s);
            int sw = (int)(this.width / s);

            // Check if clicking on an existing element
            for (PowerHudConfig.LayoutEntry entry : tempOrder) {
                ElementBounds bounds = getElementBounds(entry, this.textRenderer, sw);
                if (hudMouseX >= bounds.x && hudMouseX <= bounds.x + bounds.width
                    && hudMouseY >= bounds.y && hudMouseY <= bounds.y + bounds.height) {
                    // Start dragging this element
                    draggedElement = entry;
                    dragOffsetX = hudMouseX - bounds.x;
                    dragOffsetY = hudMouseY - bounds.y;
                    isDragging = true;
                    return true;
                }
            }

            // Check if clicking on palette elements (multi-row layout)
            int paletteY = this.height - PALETTE_HEIGHT;
            int totalWidth = ELEMENTS_PER_ROW * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
            int paletteStartX = (this.width - totalWidth) / 2;

            // Build set of placed element IDs for O(1) lookup
            Set<String> placedIds = new HashSet<>();
            for (PowerHudConfig.LayoutEntry entry : tempOrder) {
                placedIds.add(entry.id);
            }

            for (int i = 0; i < ALL_ELEMENTS.length; i++) {
                String elementId = ALL_ELEMENTS[i];
                int row = i / ELEMENTS_PER_ROW;
                int col = i % ELEMENTS_PER_ROW;
                int btnX = paletteStartX + col * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
                int btnY = paletteY + 20 + row * (ELEMENT_BUTTON_HEIGHT + ELEMENT_SPACING);

                // Skip if element already placed (except spacer)
                boolean alreadyPlaced = !elementId.equals("SPACE") && placedIds.contains(elementId);
                if (alreadyPlaced) continue;

                if (mouseX >= btnX && mouseX <= btnX + ELEMENT_BUTTON_WIDTH &&
                    mouseY >= btnY && mouseY <= btnY + ELEMENT_BUTTON_HEIGHT) {
                    // Create new element and start dragging it
                    PowerHudConfig.LayoutEntry newEntry = PowerHudConfig.LayoutEntry.freeForm(
                        elementId,
                        HUD_X_LEFT_MARGIN,
                        SPACING_HUD_TOP
                    );
                    tempOrder.add(newEntry);
                    draggedElement = newEntry;
                    isDragging = true;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            // Check if dropped in palette area (remove element)
            int paletteY = this.height - PALETTE_HEIGHT;
            if (mouseY >= paletteY) {
                Zone zone = zoneForEntry(draggedElement);
                tempOrder.remove(draggedElement);
                reflowZone(zone);
            } else {
                normalizeAllZones();
            }

            isDragging = false;
            draggedElement = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
            Zone zone = zoneForMouse(mouseX);
            setEntryZone(draggedElement, zone);
            reflowZoneWithDrag(zone, draggedElement, (int)(mouseY / s));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float t) {
        this.renderBackground(dc, mx, my, t);

        // Semi-transparent overlay
        dc.fill(0, 0, this.width, this.height, 0x80000000);

        int paletteY = this.height - PALETTE_HEIGHT;
        float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
        int sw = (int)(this.width / s);
        int hudMouseX = (int)(mx / s);
        int hudMouseY = (int)(my / s);


        // Render live HUD preview with current positions (only if tempOrder has elements)
        if (!tempOrder.isEmpty()) {
            List<PowerHudConfig.LayoutEntry> backup = PowerHudConfig.hudOrder;
            PowerHudConfig.hudOrder = tempOrder;

            new HudRenderer().renderMainHud(dc, MinecraftClient.getInstance());

            PowerHudConfig.hudOrder = backup;
        }

        dc.getMatrices().push();
        dc.getMatrices().scale(s, s, MATRIX_SCALE_Z);

        // Highlight elements being hovered or dragged
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            if (entry == draggedElement && isDragging) continue; // Don't highlight dragged element in place

            ElementBounds bounds = getElementBounds(entry, this.textRenderer, sw);
            boolean hovered = hudMouseX >= bounds.x && hudMouseX <= bounds.x + bounds.width
                && hudMouseY >= bounds.y && hudMouseY <= bounds.y + bounds.height;

            if (hovered) {
                // Draw hover outline
                int x = bounds.x;
                int y = bounds.y;
                int width = bounds.width;
                int height = bounds.height;
                dc.fill(x - 2, y - 2, x + width + 2, y - 1, 0xFFFFFFFF);
                dc.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, 0xFFFFFFFF);
                dc.fill(x - 2, y - 1, x - 1, y + height + 1, 0xFFFFFFFF);
                dc.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, 0xFFFFFFFF);
            }
        }

        // Draw dragged element at cursor (showing where it will snap)
        if (isDragging && draggedElement != null) {
            ElementBounds bounds = getElementBounds(draggedElement, this.textRenderer, sw);

            // Draw semi-transparent box at snap position
            dc.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x8800FF00);
            dc.drawCenteredTextWithShadow(this.textRenderer, draggedElement.id,
                bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 - 4, 0xFFFFFFFF);
        }

        dc.getMatrices().pop();

        // Draw palette elements in multi-row grid
        int totalWidth = ELEMENTS_PER_ROW * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
        int paletteStartX = (this.width - totalWidth) / 2;
        hoveredPaletteElement = null;

        // Build set of placed element IDs for O(1) lookup
        Set<String> placedIds = new HashSet<>();
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            placedIds.add(entry.id);
        }

        for (int i = 0; i < ALL_ELEMENTS.length; i++) {
            String elementId = ALL_ELEMENTS[i];
            int row = i / ELEMENTS_PER_ROW;
            int col = i % ELEMENTS_PER_ROW;
            int btnX = paletteStartX + col * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
            int btnY = paletteY + 20 + row * (ELEMENT_BUTTON_HEIGHT + ELEMENT_SPACING);

            boolean alreadyPlaced = !elementId.equals("SPACE") && placedIds.contains(elementId);
            boolean hovered = mx >= btnX && mx <= btnX + ELEMENT_BUTTON_WIDTH &&
                            my >= btnY && my <= btnY + ELEMENT_BUTTON_HEIGHT;

            int color = alreadyPlaced ? 0xFF333333 : (hovered ? 0xFF777777 : 0xFF555555);

            dc.fill(btnX, btnY, btnX + ELEMENT_BUTTON_WIDTH, btnY + ELEMENT_BUTTON_HEIGHT, color);
            int textColor = alreadyPlaced ? 0xFF666666 : 0xFFFFFFFF;
            dc.drawCenteredTextWithShadow(this.textRenderer, elementId, btnX + ELEMENT_BUTTON_WIDTH/2, btnY + 5, textColor);

            // Track hovered element for tooltip
            if (hovered && !alreadyPlaced) {
                hoveredPaletteElement = PowerHudConfig.LayoutEntry.freeForm(elementId, 0, 0);
            }
        }

        super.render(dc, mx, my, t);

        // Draw tooltip for hovered palette element
        if (hoveredPaletteElement != null && ELEMENT_TOOLTIPS.containsKey(hoveredPaletteElement.id)) {
            dc.drawTooltip(this.textRenderer, Text.literal(ELEMENT_TOOLTIPS.get(hoveredPaletteElement.id)), mx, my);
        }
    }

    @Override
    public void renderBackground(DrawContext dc, int mx, int my, float d) {}

    @Override
    public void close() {
        isWorkbenchActive = false;
        super.close();
    }

    private enum Zone { LEFT, CENTER, RIGHT }
}

