package net.steve.powerhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    private static final int PALETTE_HEIGHT = 80;
    private static final int PALETTE_Y_OFFSET = 100;
    private static final int ELEMENT_BUTTON_WIDTH = 80;
    private static final int ELEMENT_BUTTON_HEIGHT = 20;
    private static final int ELEMENT_SPACING = 5;

    // Available elements that can be added
    private static final String[] ALL_ELEMENTS = {
        "FPS", "XYZ", "FACING", "BIOME", "TIME", "VIT",
        "BLOCK", "TOOL", "INV", "GAMEMODE", "BLOCK_STATS"
    };

    public HudOrderScreen(Screen parent) {
        super(Text.literal("HUD Workbench - WYSIWYG Editor"));
        this.parent = parent;
        this.tempOrder = new ArrayList<>();
        // Deep copy existing layout
        for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
            if (entry.useFreeForm) {
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, entry.x, entry.y));
            } else {
                // Convert old alignment-based to free-form
                int x = calculateXFromAlignment(entry.alignment);
                int y = tempOrder.stream()
                    .filter(e -> e.alignment == entry.alignment)
                    .mapToInt(e -> e.y + 15)
                    .max()
                    .orElse(10);
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, x, y));
            }
        }
    }

    private int calculateXFromAlignment(int alignment) {
        return switch(alignment) {
            case 0 -> 10; // LEFT
            case 1 -> -200; // CENTER (will be calculated at render)
            case 2 -> -150; // RIGHT
            default -> 10;
        };
    }

    @Override
    protected void init() {
        isWorkbenchActive = true;
        this.clearChildren();
        
        int bottomY = this.height - PALETTE_Y_OFFSET;

        // Reset button
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Default"), b -> {
            PowerHudConfig.resetToVanilla();
            tempOrder.clear();
            for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
                tempOrder.add(PowerHudConfig.LayoutEntry.freeForm(entry.id, entry.x, entry.y));
            }
        }).dimensions(this.width/2 - 200, bottomY, 90, 20)
          .tooltip(Tooltip.of(Text.literal("Restore default layout")))
          .build());
        
        // Clear all button
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> {
            tempOrder.clear();
        }).dimensions(this.width/2 - 100, bottomY, 90, 20)
          .tooltip(Tooltip.of(Text.literal("Remove all elements")))
          .build());

        // Done button
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(tempOrder);
            PowerHudConfig.save();
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + 10, bottomY, 90, 20)
          .tooltip(Tooltip.of(Text.literal("Save and exit")))
          .build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + 110, bottomY, 90, 20)
          .tooltip(Tooltip.of(Text.literal("Exit without saving")))
          .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if clicking on an existing element
            for (PowerHudConfig.LayoutEntry entry : tempOrder) {
                int x = calculateAbsoluteX(entry.x);
                int y = entry.y;
                int width = getElementWidth(entry.id);
                int height = getElementHeight(entry.id);

                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    // Start dragging this element
                    draggedElement = entry;
                    dragOffsetX = (int)(mouseX - x);
                    dragOffsetY = (int)(mouseY - y);
                    isDragging = true;
                    return true;
                }
            }

            // Check if clicking on palette elements
            int paletteY = this.height - PALETTE_HEIGHT - 30;
            int paletteX = (this.width - (ALL_ELEMENTS.length * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING))) / 2;

            for (int i = 0; i < ALL_ELEMENTS.length; i++) {
                String elementId = ALL_ELEMENTS[i];
                int btnX = paletteX + i * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
                int btnY = paletteY + 20;

                // Skip if element already placed
                boolean alreadyPlaced = tempOrder.stream().anyMatch(e -> e.id.equals(elementId));
                if (alreadyPlaced) continue;

                if (mouseX >= btnX && mouseX <= btnX + ELEMENT_BUTTON_WIDTH &&
                    mouseY >= btnY && mouseY <= btnY + ELEMENT_BUTTON_HEIGHT) {
                    // Create new element and start dragging it
                    PowerHudConfig.LayoutEntry newEntry = PowerHudConfig.LayoutEntry.freeForm(
                        elementId,
                        (int)mouseX - 40,
                        (int)mouseY - 10
                    );
                    tempOrder.add(newEntry);
                    draggedElement = newEntry;
                    dragOffsetX = 40;
                    dragOffsetY = 10;
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
            int paletteY = this.height - PALETTE_HEIGHT - 30;
            if (mouseY >= paletteY) {
                tempOrder.remove(draggedElement);
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
            // Update position
            int newX = (int)mouseX - dragOffsetX;
            int newY = (int)mouseY - dragOffsetY;

            // Handle negative X (right-aligned elements)
            if (newX > this.width / 2) {
                draggedElement.x = newX - this.width; // Store as negative offset from right
            } else {
                draggedElement.x = newX;
            }

            draggedElement.y = Math.max(0, Math.min(this.height - PALETTE_HEIGHT - 50, newY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private int calculateAbsoluteX(int x) {
        if (x < 0) {
            return this.width + x; // Negative = from right edge
        }
        return x;
    }

    private int getElementWidth(String id) {
        // Estimate width based on element type
        return switch(id) {
            case "INV" -> 200;
            case "XYZ" -> 150;
            case "FPS" -> PowerHudConfig.fpsMode == PowerHudConfig.FpsMode.FULL ? 180 : 120;
            case "BLOCK_STATS" -> 160;
            default -> 120;
        };
    }

    private int getElementHeight(String id) {
        // Estimate height based on element type
        return switch(id) {
            case "INV" -> 60;
            default -> 12;
        };
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float t) {
        this.renderBackground(dc, mx, my, t);

        // Semi-transparent overlay
        dc.fill(0, 0, this.width, this.height, 0x80000000);

        // Render live HUD preview with current positions
        List<PowerHudConfig.LayoutEntry> backup = PowerHudConfig.hudOrder;
        PowerHudConfig.hudOrder = tempOrder;
        
        new HudRenderer().renderMainHud(dc, MinecraftClient.getInstance());
        
        PowerHudConfig.hudOrder = backup;
        
        // Highlight elements being hovered or dragged
        for (PowerHudConfig.LayoutEntry entry : tempOrder) {
            if (entry == draggedElement && isDragging) continue; // Don't highlight dragged element in place

            int x = calculateAbsoluteX(entry.x);
            int y = entry.y;
            int width = getElementWidth(entry.id);
            int height = getElementHeight(entry.id);

            boolean hovered = mx >= x && mx <= x + width && my >= y && my <= y + height;

            if (hovered) {
                // Draw hover outline
                dc.fill(x - 2, y - 2, x + width + 2, y - 1, 0xFFFFFFFF);
                dc.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, 0xFFFFFFFF);
                dc.fill(x - 2, y - 1, x - 1, y + height + 1, 0xFFFFFFFF);
                dc.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, 0xFFFFFFFF);
            }
        }

        // Draw dragged element at cursor
        if (isDragging && draggedElement != null) {
            int dragX = (int)mx - dragOffsetX;
            int dragY = (int)my - dragOffsetY;
            int width = getElementWidth(draggedElement.id);
            int height = getElementHeight(draggedElement.id);

            // Draw semi-transparent box
            dc.fill(dragX, dragY, dragX + width, dragY + height, 0x8800FF00);
            dc.drawCenteredTextWithShadow(this.textRenderer, draggedElement.id, dragX + width/2, dragY + height/2 - 4, 0xFFFFFFFF);
        }

        // Draw palette area at bottom
        int paletteY = this.height - PALETTE_HEIGHT - 30;
        dc.fill(0, paletteY, this.width, this.height, 0xCC222222);
        dc.drawCenteredTextWithShadow(this.textRenderer, "Available Elements (Drag to add)", this.width / 2, paletteY + 5, 0xFFFFFFFF);

        // Draw palette elements
        int paletteX = (this.width - (ALL_ELEMENTS.length * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING))) / 2;
        for (int i = 0; i < ALL_ELEMENTS.length; i++) {
            String elementId = ALL_ELEMENTS[i];
            int btnX = paletteX + i * (ELEMENT_BUTTON_WIDTH + ELEMENT_SPACING);
            int btnY = paletteY + 20;

            boolean alreadyPlaced = tempOrder.stream().anyMatch(e -> e.id.equals(elementId));
            int color = alreadyPlaced ? 0xFF333333 : 0xFF555555;

            dc.fill(btnX, btnY, btnX + ELEMENT_BUTTON_WIDTH, btnY + ELEMENT_BUTTON_HEIGHT, color);
            int textColor = alreadyPlaced ? 0xFF666666 : 0xFFFFFFFF;
            dc.drawCenteredTextWithShadow(this.textRenderer, elementId, btnX + ELEMENT_BUTTON_WIDTH/2, btnY + 6, textColor);
        }

        // Instructions
        dc.drawCenteredTextWithShadow(this.textRenderer,
            "Drag elements to position • Drop in palette to remove • Right side = negative X offset",
            this.width / 2, 10, 0xFFAAAAAA);

        super.render(dc, mx, my, t);
    }

    @Override
    public void renderBackground(DrawContext dc, int mx, int my, float d) {}

    @Override
    public void close() {
        isWorkbenchActive = false;
        super.close();
    }
}