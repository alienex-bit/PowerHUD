package net.steve.powerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
    private static final int ELEMENT_BUTTON_WIDTH = 70;
    private static final int ELEMENT_BUTTON_HEIGHT = 18;
    private static final int ELEMENT_SPACING = 4;
    private static final int ELEMENTS_PER_ROW = 6;

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
        super(Component.literal("HUD Workbench - WYSIWYG Editor"));
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

    private List<PowerHudConfig.LayoutEntry> entriesInZone(Zone zone) {
        List<PowerHudConfig.LayoutEntry> result = new ArrayList<>();
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            if (zoneForEntry(entry) == zone) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparingInt(e -> e.y));
        return result;
    }

    private void reflowZone(Zone zone) {
        List<PowerHudConfig.LayoutEntry> zoneEntries = entriesInZone(zone);
        int currentY = SPACING_HUD_TOP;
        for (PowerHudConfig.LayoutEntry entry : zoneEntries) {
            entry.y = currentY;
            currentY += getElementHeight(entry.id) + PowerHudConfig.lineSpacing;
        }
    }

    private void reflowZoneWithDrag(Zone zone, PowerHudConfig.LayoutEntry dragged, int targetY) {
        List<PowerHudConfig.LayoutEntry> zoneEntries = entriesInZone(zone);
        zoneEntries.remove(dragged);

        int insertIndex = zoneEntries.size();
        for (int i = 0; i < zoneEntries.size(); i++) {
            if (targetY < zoneEntries.get(i).y) {
                insertIndex = i;
                break;
            }
        }
        zoneEntries.add(insertIndex, dragged);

        int currentY = SPACING_HUD_TOP;
        for (PowerHudConfig.LayoutEntry entry : zoneEntries) {
            entry.y = currentY;
            currentY += getElementHeight(entry.id) + PowerHudConfig.lineSpacing;
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
        this.clearWidgets();
        
        normalizeAllZones();

        int bottomY = this.height - PALETTE_HEIGHT - 35;

        int btnWidth = 80;
        int btnSpacing = 5;
        int totalWidth = (btnWidth * 5) + (btnSpacing * 4);
        int startX = (this.width - totalWidth) / 2;

        // 1. Reset
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            PowerHudConfig.resetToVanilla();
            tempOrder.clear();
            for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, entry.x, entry.y));
            }
        }).bounds(startX, bottomY, btnWidth, 18)
            .tooltip(Tooltip.create(Component.literal("Restore default layout")))
            .build());

        // 2. Save Changes
        addRenderableWidget(Button.builder(Component.literal("Save Changes"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(tempOrder);
            boolean success = false;
            if (PowerHudConfig.currentProfile != null && !PowerHudConfig.currentProfile.isEmpty()) {
                success = PowerHudConfig.saveProfile(PowerHudConfig.currentProfile);
            } else {
                PowerHudConfig.save();
            }
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(success ? "Profile layout saved!" : "Layout saved to config."));
            }
        }).bounds(startX + (btnWidth + btnSpacing) * 1, bottomY, btnWidth, 18)
            .tooltip(Tooltip.create(Component.literal("Save layout to current profile")))
            .build());

        // 3. Clear Layout
        addRenderableWidget(Button.builder(Component.literal("Clear Layout"), b -> {
            tempOrder.clear();
        }).bounds(startX + (btnWidth + btnSpacing) * 2, bottomY, btnWidth, 18)
            .tooltip(Tooltip.create(Component.literal("Remove all HUD elements")))
            .build());

        // 4. Back
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            isWorkbenchActive = false;
            if (this.minecraft != null && this.minecraft.gui != null) {
                this.minecraft.gui.setScreen(parent);
            }
        }).bounds(startX + (btnWidth + btnSpacing) * 3, bottomY, btnWidth, 18)
            .tooltip(Tooltip.create(Component.literal("Return without saving")))
            .build());

        // 5. Done
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(tempOrder);
            if (PowerHudConfig.currentProfile != null && !PowerHudConfig.currentProfile.isEmpty()) {
                PowerHudConfig.saveProfile(PowerHudConfig.currentProfile);
            } else {
                PowerHudConfig.save();
            }
            isWorkbenchActive = false;
            if (this.minecraft != null && this.minecraft.gui != null) {
                this.minecraft.gui.setScreen(null);
            }
        }).bounds(startX + (btnWidth + btnSpacing) * 4, bottomY, btnWidth, 18)
            .tooltip(Tooltip.create(Component.literal("Save and close")))
            .build());
    }

    private ElementBounds getElementBounds(PowerHudConfig.LayoutEntry entry, Font ren, int sw) {
        if (entry.id.equals(TEXT_SPACE)) {
            return new ElementBounds(HudRenderer.resolveX(entry.x, 60, sw), entry.y, 60, PowerHudConfig.lineSpacing);
        }

        boolean gridInv = entry.id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID;
        if (gridInv) {
            int titleW = HudRenderer.getHudTextWidth(ren, "Inventory", PowerHudConfig.boldTitles);
            int width = Math.max(titleW, INV_GRID_WIDTH);
            int x = HudRenderer.resolveX(entry.x, width, sw);
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
        int x = HudRenderer.resolveX(entry.x, width, sw);
        return new ElementBounds(x, entry.y, width, ren.lineHeight);
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
            default -> "";
        };
    }

    private int getElementHeight(String id) {
        if (id.equals("SPACE")) {
            return PowerHudConfig.lineSpacing;
        }
        if (id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID) {
            return INV_HEIGHT_TOTAL;
        }
        return this.font != null ? this.font.lineHeight : HUD_LINE_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) { // Left click
            float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
            int hudMouseX = (int)(mouseX / s);
            int hudMouseY = (int)(mouseY / s);
            int sw = (int)(this.width / s);

            // Check if clicking on an existing element
            for (PowerHudConfig.LayoutEntry entry : tempOrder) {
                ElementBounds bounds = getElementBounds(entry, this.font, sw);
                if (hudMouseX >= bounds.x && hudMouseX <= bounds.x + bounds.width
                    && hudMouseY >= bounds.y && hudMouseY <= bounds.y + bounds.height) {
                    draggedElement = entry;
                    dragOffsetX = hudMouseX - bounds.x;
                    dragOffsetY = hudMouseY - bounds.y;
                    isDragging = true;
                    return true;
                }
            }

            // Check if clicking on palette elements
            int paletteY = this.height - PALETTE_HEIGHT;
            int totalWidth = ELEMENTS_PER_ROW * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
            int paletteStartX = (this.width - totalWidth) / 2;

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
                if (alreadyPlaced) continue;

                if (mouseX >= btnX && mouseX <= btnX + ELEMENT_BUTTON_WIDTH &&
                    mouseY >= btnY && mouseY <= btnY + ELEMENT_BUTTON_HEIGHT) {
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

        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0 && isDragging) {
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
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
            Zone zone = zoneForMouse(event.x());
            setEntryZone(draggedElement, zone);
            reflowZoneWithDrag(zone, draggedElement, (int)(event.y() / s));
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor dc, int mx, int my, float t) {
        super.extractRenderState(dc, mx, my, t);

        // Semi-transparent overlay
        dc.fill(0, 0, this.width, this.height, 0x80000000);

        int paletteY = this.height - PALETTE_HEIGHT;
        float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
        int sw = (int)(this.width / s);
        int hudMouseX = (int)(mx / s);
        int hudMouseY = (int)(my / s);

        // Render live HUD preview with current positions
        if (!tempOrder.isEmpty()) {
            List<PowerHudConfig.LayoutEntry> backup = PowerHudConfig.hudOrder;
            PowerHudConfig.hudOrder = tempOrder;

            HudRenderer.renderMainHud(dc, Minecraft.getInstance());

            PowerHudConfig.hudOrder = backup;
        }

        dc.pose().pushMatrix();
        dc.pose().scale(s, s);

        // Highlight elements being hovered or dragged
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            if (entry == draggedElement && isDragging) continue;

            ElementBounds bounds = getElementBounds(entry, this.font, sw);
            boolean hovered = hudMouseX >= bounds.x && hudMouseX <= bounds.x + bounds.width
                && hudMouseY >= bounds.y && hudMouseY <= bounds.y + bounds.height;

            if (hovered) {
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
            ElementBounds bounds = getElementBounds(draggedElement, this.font, sw);

            dc.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x8800FF00);
            dc.centeredText(this.font, draggedElement.id,
                bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 - 4, 0xFFFFFFFF);
        }

        dc.pose().popMatrix();

        // Draw palette elements in multi-row grid
        int totalWidth = ELEMENTS_PER_ROW * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
        int paletteStartX = (this.width - totalWidth) / 2;
        hoveredPaletteElement = null;

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
            dc.centeredText(this.font, elementId, btnX + ELEMENT_BUTTON_WIDTH / 2, btnY + 5, textColor);

            if (hovered && !alreadyPlaced) {
                hoveredPaletteElement = PowerHudConfig.LayoutEntry.freeForm(elementId, 0, 0);
            }
        }

        // Draw tooltip for hovered palette element
        if (hoveredPaletteElement != null && ELEMENT_TOOLTIPS.containsKey(hoveredPaletteElement.id)) {
            dc.setTooltipForNextFrame(this.font, Component.literal(ELEMENT_TOOLTIPS.get(hoveredPaletteElement.id)), mx, my);
        }
    }

    @Override
    public void removed() {
        isWorkbenchActive = false;
        super.removed();
    }

    @Override
    public void onClose() {
        isWorkbenchActive = false;
        super.onClose();
    }

    private enum Zone { LEFT, CENTER, RIGHT }
}
