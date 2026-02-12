package net.steve.powerhud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

import static net.steve.powerhud.HudConstants.*;

public class HudRenderer implements HudRenderCallback {
    private static final Identifier[] FONTS = {
        null,
        Identifier.of("powerhud", "jetbrainsmono-regular"),
        Identifier.of("powerhud", "robotomono-regular"),
        Identifier.of("powerhud", "firacode-regular"),
        Identifier.of("powerhud", "cascadiacode"),
        Identifier.of("powerhud", "sourcecodepro-regular"),
        Identifier.of("powerhud", "comicmono"),
        Identifier.of("powerhud", "monofur"),
        Identifier.of("powerhud", "ubuntumono"),
        Identifier.of("powerhud", "intermono")
    };
    
    private static KeyBinding configKey, toggleKey, resetKey, debugKey;
    
    private record Renderable(
        String title,
        String value,
        int valColor,
        String id,
        int align,
        int spaceH,
        boolean isSpace
    ) {}

    public static void initKeys() {
        configKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Open Config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "PowerHUD")
        );
        toggleKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Toggle HUD", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "PowerHUD")
        );
        resetKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Reset FPS stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "PowerHUD")
        );
        debugKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Cycle Debug Mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "PowerHUD")
        );
    }

    @Override
    public void onHudRender(DrawContext dc, RenderTickCounter tc) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        HudData.update(client);
        
        // Handle key bindings
        while (configKey.wasPressed()) {
            client.setScreen(new PowerHudConfigScreen());
        }
        
        while (toggleKey.wasPressed()) {
            PowerHudConfig.hudEnabled = !PowerHudConfig.hudEnabled;
            PowerHudConfig.save();
        }
        
        while (resetKey.wasPressed()) {
            HudData.resetFps();
        }
        
        while (debugKey.wasPressed()) {
            PowerHudConfig.debugTab++;
            if (PowerHudConfig.debugTab > 3) {
                PowerHudConfig.debugTab = 0;
            }
        }
        
        // Render debug screen if enabled
        if (PowerHudConfig.debugTab > 0) {
            F3ScreenRenderer.render(dc, PowerHudConfig.debugTab);
        }
        
        // Avoid double-rendering while the HUD workbench is open
        if (HudOrderScreen.isWorkbenchActive) {
            return;
        }

        // Don't render HUD if disabled or hidden
        if (!PowerHudConfig.hudEnabled || client.options.hudHidden) {
            return;
        }
        
        renderMainHud(dc, client);
    }

    public void renderMainHud(DrawContext dc, MinecraftClient client) {
        TextRenderer ren = client.textRenderer;
        int theme = PowerHudConfig.COLORS[PowerHudConfig.themeIndex];
        float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
        
        dc.getMatrices().push();
        dc.getMatrices().scale(s, s, MATRIX_SCALE_Z);
        int sw = (int)(client.getWindow().getScaledWidth() / s);

        long now = System.currentTimeMillis();
        int tColor = PowerHudConfig.COLORS[PowerHudConfig.titleColorIndex];
        int hAdj = (PowerHudConfig.fontIndex > 0) ? 1 : 0;

        // Render each element at its absolute position
        for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
            // Skip oxygen - it's rendered as a standalone overlay
            if (entry.id.equals("OXY")) {
                continue;
            }

            boolean force = HudOrderScreen.isWorkbenchActive;
            if (!shouldShow(entry.id) && !force) continue;
            
            HudLineRaw line = getRaw(entry.id, theme);
            if (line == null && !force) continue;
            if (line == null) {
                line = new HudLineRaw(entry.id, TEXT_PREVIEW, theme);
            }
            
            // Calculate absolute position
            int x = entry.x;
            int y = entry.y;

            // Render element at position
            renderElementAt(dc, ren, entry.id, line, x, y, tColor, theme, s, now, hAdj, sw);
        }

        dc.getMatrices().pop();

        // Render oxygen as standalone centered overlay OUTSIDE the scaled matrix
        // This ensures consistent positioning regardless of HUD scale or vanilla air bubble state
        if (PowerHudConfig.showOxygen) {
            renderOxygenOverlay(dc, ren, client.getWindow().getScaledWidth());
        }
    }

    private void renderElementAt(
        DrawContext dc,
        TextRenderer ren,
        String id,
        HudLineRaw line,
        int x,
        int y,
        int tColor,
        int theme,
        float s,
        long now,
        int hAdj,
        int sw
    ) {
        if (id.equals(TEXT_SPACE)) {
            return;
        }
        // Special handling for inventory grid mode
        if (id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID) {
            boolean isCreative = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                isCreative = client.player.isCreative();
            }
            int titleW = getWidth("Inventory", ren, PowerHudConfig.boldTitles);
            int gridW = Math.max(titleW, INV_GRID_WIDTH);
            int renderX = resolveX(x, gridW, sw);

            // Calculate padding for centering grid within box
            int boxW = gridW + 4;
            int gridWActual = INV_COLS * INV_SLOT_SPACING;
            int gridPad = (boxW - gridWActual) / 2;
            int gridStartX = renderX + gridPad;
            // Add extra background padding at the bottom
            int extraPad = 8; // Increase as needed for more space
            int boxRows = isCreative ? 1 : 4;
            int boxH = boxRows * INV_ROW_HEIGHT + INV_GRID_OFFSET + 2 + extraPad;
            if (PowerHudConfig.boxStyle != PowerHudConfig.BoxStyle.OFF) {
                int alpha = switch (PowerHudConfig.boxStyle) {
                    case FAINT -> 0x20;
                    case LIGHT -> 0x40;
                    case SUBTLE -> 0x60;
                    case MEDIUM -> 0x80;
                    case STRONG -> 0xA0;
                    case DARK -> 0xC0;
                    case SOLID -> 0xFF;
                    default -> 0x60;
                };
                int boxColor = (alpha << 24) | 0x000000;
                int boxX = renderX - 2;
                int boxY = y - 1;
                dc.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxColor);
            }
            drawStyledText(dc, ren, "Inventory", renderX, y, tColor, s, PowerHudConfig.boldTitles, now);
            if (isCreative) {
                // Draw hotbar row at the top, centered
                int hotbarY = y + INV_GRID_OFFSET;
                for (int c = 0; c < 9; c++) {
                    int i = 27 + c;
                    int sx = gridStartX + (int)(c * INV_SLOT_SPACING);
                    int sy = hotbarY;
                    dc.fill(
                        sx,
                        sy,
                        sx + INV_SLOT_SIZE,
                        sy + INV_SLOT_SIZE,
                        HudData.invSlots[i] ? HudData.invColor : COLOR_BORDER_LIGHT
                    );
                }
                return;
            } else {
                // Draw inventory grid (top 3 rows), centered
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 9; c++) {
                        int i = r * 9 + c;
                        int sx = gridStartX + (int)(c * INV_SLOT_SPACING);
                        int sy = y + INV_GRID_OFFSET + (int)(r * INV_SLOT_SPACING);
                        dc.fill(
                            sx,
                            sy,
                            sx + INV_SLOT_SIZE,
                            sy + INV_SLOT_SIZE,
                            HudData.invSlots[i] ? HudData.invColor : COLOR_BORDER_LIGHT
                        );
                    }
                }
                // Add 3 spaces (pixels) before hotbar
                int hotbarY = y + INV_GRID_OFFSET + (int)(3 * INV_SLOT_SPACING) + 3;
                for (int c = 0; c < 9; c++) {
                    int i = 27 + c;
                    int sx = gridStartX + (int)(c * INV_SLOT_SPACING);
                    int sy = hotbarY;
                    dc.fill(
                        sx,
                        sy,
                        sx + INV_SLOT_SIZE,
                        sy + INV_SLOT_SIZE,
                        HudData.invSlots[i] ? HudData.invColor : COLOR_BORDER_LIGHT
                    );
                }
                return;
            }
        }

        // Standard line rendering
        int valW = getWidth(line.value, ren, false);
        int dotW = (id.equals("FPS") && PowerHudConfig.showFpsDot) ? HUD_DOT_WIDTH : 0;
        int iconW = (id.equals("TOOL") && !HudData.toolStack.isEmpty()) ? HUD_ICON_WIDTH : 0;
        int titleW = line.title.isEmpty() ? 0 : getWidth(line.title + ": ", ren, PowerHudConfig.boldTitles);
        int totalW = titleW + dotW + valW + iconW;

        int renderX = resolveX(x, totalW, sw);

        boolean shouldRender = HudOrderScreen.isWorkbenchActive
            || (!(id.equals("BLOCK") && line.value.equals(TEXT_AIR))
            && !(id.equals("TOOL") && line.value.isEmpty())
            && !line.value.isEmpty());

        if (shouldRender) {
            // Draw background box if enabled
            if (PowerHudConfig.boxStyle != PowerHudConfig.BoxStyle.OFF) {
                int alpha = switch (PowerHudConfig.boxStyle) {
                    case FAINT -> 0x20;
                    case LIGHT -> 0x40;
                    case SUBTLE -> 0x60;
                    case MEDIUM -> 0x80;
                    case STRONG -> 0xA0;
                    case DARK -> 0xC0;
                    case SOLID -> 0xFF;
                    default -> 0x60;
                };
                int boxColor = (alpha << 24) | 0x000000; // Black background with variable alpha
                int boxX = renderX - 2;
                int boxY = y + hAdj - 1;
                int boxW = totalW + 4;
                int boxH = 10; // Approximate line height
                dc.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxColor);
            }

            int curX = renderX;

            // Only render title if it's not empty
            if (!line.title.isEmpty()) {
                drawStyledText(
                    dc,
                    ren,
                    line.title + ": ",
                    curX,
                    y + hAdj,
                    tColor,
                    s,
                    PowerHudConfig.boldTitles,
                    now
                );
                curX += titleW;
            }

            if (dotW > 0) {
                drawFpsDot(dc, ren, curX, y + hAdj, now);
                curX += dotW;
            }

            drawStyledText(dc, ren, line.value, curX, y + hAdj, line.valColor, s, false, now);

            if (iconW > 0) {
                dc.drawItem(HudData.toolStack, curX + valW + HUD_ICON_OFFSET, y + hAdj - HUD_ICON_OFFSET);
            }
        }
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

    private void renderCol(
        DrawContext dc,
        TextRenderer ren,
        List<Renderable> items,
        int sw,
        int theme,
        float s,
        int align
    ) {
        int spacing = PowerHudConfig.lineSpacing;
        int currentY = SPACING_HUD_TOP;
        int hAdj = (PowerHudConfig.fontIndex > 0) ? 1 : 0;
        int tColor = PowerHudConfig.COLORS[PowerHudConfig.titleColorIndex];
        long now = System.currentTimeMillis();
        
        for (Renderable item : items) {
            // Skip spacers - they just add vertical space
            if (item.isSpace) {
                currentY += item.spaceH;
                continue;
            }
            
            // Special handling for inventory grid mode
            if (item.id.equals("INV") && PowerHudConfig.inventoryMode == PowerHudConfig.InventoryMode.GRID) {
                int modX = (align == ALIGN_LEFT) ? SPACING_HUD_TOP 
                    : (align == ALIGN_CENTER ? (sw/2 - INV_GRID_WIDTH/2) 
                    : sw - INV_GRID_WIDTH - SPACING_HUD_TOP);
                
                drawStyledText(dc, ren, "Inventory", modX, currentY, tColor, s, PowerHudConfig.boldTitles, now);
                
                // Draw inventory grid
                for (int r = 0; r < INV_ROWS; r++) {
                    for (int c = 0; c < INV_COLS; c++) {
                        int i = r * INV_COLS + c;
                        int sx = modX + (int)(c * INV_SLOT_SPACING);
                        int sy = currentY + INV_GRID_OFFSET + (int)(r * INV_SLOT_SPACING);
                        
                        dc.fill(
                            sx,
                            sy,
                            sx + INV_SLOT_SIZE,
                            sy + INV_SLOT_SIZE,
                            HudData.invSlots[i] ? HudData.invColor : COLOR_BORDER_LIGHT
                        );
                    }
                }
                
                currentY += INV_HEIGHT_TOTAL;
            }
            // Standard line rendering
            else {
                int valW = getWidth(item.value, ren, false);
                int dotW = (item.id.equals("FPS") && PowerHudConfig.showFpsDot) ? HUD_DOT_WIDTH : 0;
                int iconW = (item.id.equals("TOOL") && !HudData.toolStack.isEmpty()) ? HUD_ICON_WIDTH : 0;
                int titleW = item.title.isEmpty() ? 0 : getWidth(item.title + ": ", ren, PowerHudConfig.boldTitles);
                int lineW = titleW + valW + dotW + iconW + HUD_PADDING;
                int modX = (align == ALIGN_LEFT) ? SPACING_HUD_TOP
                    : (align == ALIGN_CENTER ? (sw/2 - lineW/2) 
                    : sw - lineW - SPACING_HUD_TOP);
                
                boolean shouldRender = HudOrderScreen.isWorkbenchActive 
                    || (!(item.id.equals("BLOCK") && item.value.equals(TEXT_AIR)) 
                    && !(item.id.equals("TOOL") && item.value.isEmpty())
                    && !item.value.isEmpty());

                if (shouldRender) {
                    int curX = modX;

                    // Only render title if it's not empty
                    if (!item.title.isEmpty()) {
                        drawStyledText(
                            dc,
                            ren,
                            item.title + ": ",
                            curX,
                            currentY + hAdj,
                            tColor,
                            s,
                            PowerHudConfig.boldTitles,
                            now
                        );
                        curX += titleW;
                    }

                    if (dotW > 0) {
                        drawFpsDot(dc, ren, curX, currentY + hAdj, now);
                        curX += dotW;
                    }
                    
                    drawStyledText(dc, ren, item.value, curX, currentY + hAdj, item.valColor, s, false, now);
                    
                    if (iconW > 0) {
                        dc.drawItem(HudData.toolStack, curX + valW + HUD_ICON_OFFSET, currentY + hAdj - HUD_ICON_OFFSET);
                    }
                    
                    currentY += spacing;
                }
            }
        }
    }

    private record HudLineRaw(String title, String value, int valColor) {}

    private HudLineRaw getRaw(String id, int theme) {
        return switch(id) {
            case "FPS" -> new HudLineRaw("FPS", HudData.fpsStr, HudData.fpsColor);
            case "XYZ" -> new HudLineRaw("XYZ", HudData.coordsStr, theme);
            case "FACING" -> new HudLineRaw("Facing", HudData.dirStr, theme);
            case "BIOME" -> new HudLineRaw("Biome", HudData.biomeStr, theme);
            case "TIME" -> new HudLineRaw(HudData.timeLabel, HudData.timeStr, theme);
            case "VIT" -> new HudLineRaw("Vitality", HudData.vitStr, COLOR_GREEN);
            case "BLOCK" -> new HudLineRaw(HudData.targetType, HudData.blockStr, theme);
            case "TOOL" -> new HudLineRaw("Best Tool", HudData.toolStr, theme);
            case "INV" -> new HudLineRaw("Inventory", HudData.invStr, HudData.invColor);
            case "OXY" -> (HudData.oxyStr.isEmpty() && !HudOrderScreen.isWorkbenchActive 
                ? null 
                : new HudLineRaw("Oxygen", HudData.oxyStr.isEmpty() ? TEXT_OXYGEN_HOLDING : HudData.oxyStr, HudData.oxyColor));
            case "BLOCK_STATS" -> new HudLineRaw("Blocks", HudData.blockStatsStr, theme);
            case "GAMEMODE" -> new HudLineRaw("Mode", HudData.gamemodeStr, theme);
            case "SPACE" -> new HudLineRaw("", "", theme);
            default -> null;
        };
    }

    private void drawFpsDot(DrawContext dc, TextRenderer ren, int x, int y, long now) {
        int dotColor = HudData.fpsColor;
        float phase = (float)(Math.sin(now / PULSE_SPEED) * PHASE_BASE + PHASE_BASE);
        float pulse = PULSE_BASE + PULSE_AMPLITUDE * phase;
        
        int r = getRed(dotColor);
        int g = getGreen(dotColor);
        int b = getBlue(dotColor);
        
        r = r + (int)((RED_MASK - r) * phase);
        g = g + (int)((RED_MASK - g) * phase);
        b = b + (int)((RED_MASK - b) * phase);
        
        dotColor = ALPHA_MASK | (r << BLUE_SHIFT) | (g << GREEN_SHIFT) | b;
        
        dc.getMatrices().push();
        dc.getMatrices().translate(x + FPS_DOT_OFFSET_X, y + FPS_DOT_OFFSET_Y, 0);
        dc.getMatrices().scale(FPS_DOT_SCALE * pulse, FPS_DOT_SCALE * pulse, MATRIX_SCALE_Z);
        dc.getMatrices().translate(-FPS_DOT_OFFSET_X, -FPS_DOT_OFFSET_Y, 0);
        dc.drawText(ren, Text.literal(FPS_DOT_CHAR), 0, 0, dotColor, true);
        dc.getMatrices().pop();
    }

    private void drawStyledText(
        DrawContext dc,
        TextRenderer ren,
        String t,
        int x,
        int y,
        int c,
        float s,
        boolean b,
        long now
    ) {
        Identifier f = FONTS[PowerHudConfig.fontIndex];
        Style st = Style.EMPTY.withFont(f).withBold(b);
        
        dc.drawText(ren, Text.literal(t).setStyle(st), x, y, c, false);
    }

    public static Identifier getHudFont() {
        return FONTS[PowerHudConfig.fontIndex];
    }

    public static int getHudTextWidth(TextRenderer ren, String text, boolean bold) {
        return ren.getWidth(
            Text.literal(text).setStyle(
                Style.EMPTY.withFont(getHudFont()).withBold(bold)
            )
        );
    }

    private int getWidth(String t, TextRenderer ren, boolean b) {
        return getHudTextWidth(ren, t, b);
    }

    private void renderOxygenOverlay(DrawContext dc, TextRenderer ren, int sw) {
        String overlay = HudData.oxyOverlayStr;
        String status = HudData.oxyStatusStr;

        // Don't render if not underwater
        if (overlay.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int sh = MinecraftClient.getInstance().getWindow().getScaledHeight();

        // Build the centered message
        String centerMsg = "Oxygen: " + overlay + " - " + status;
        int textW = getWidth(centerMsg, ren, false);
        int textH = ren.fontHeight;

        // Fixed bar width - use a consistent width regardless of text
        int barW = 200;
        int barH = textH + 6;

        // Position centered horizontally, configurable vertical position (from bottom)
        int barX = sw / 2 - barW / 2;
        int barY = sh - PowerHudConfig.oxygenOverlayY;

        // Draw darker semi-transparent background bar for better contrast
        int barBg = 0x99000000;
        dc.fill(barX, barY, barX + barW, barY + barH, barBg);

        // Draw filled portion based on oxygen level
        int filled = (int)(barW * HudData.oxyPercent);
        if (filled > 0) {
            dc.fill(barX, barY, barX + filled, barY + barH, HudData.oxyColor);
        }

        // Draw text centered on bar with shadow for better visibility
        int textX = barX + (barW - textW) / 2;
        int textY = barY + (barH - textH) / 2;

        // Draw black shadow/outline for text visibility against bright colors
        dc.drawText(ren, Text.literal(centerMsg).setStyle(Style.EMPTY.withFont(FONTS[PowerHudConfig.fontIndex])),
            textX + 1, textY + 1, 0xFF000000, false);
        // Draw main text in white
        drawStyledText(dc, ren, centerMsg, textX, textY, 0xFFFFFFFF, 1.0f, false, now);
    }

    private boolean shouldShow(String id) {
        return switch(id) {
            case "FPS" -> PowerHudConfig.showFps;
            case "XYZ" -> PowerHudConfig.showCoords;
            case "FACING" -> PowerHudConfig.showDirection;
            case "BIOME" -> PowerHudConfig.showBiome;
            case "TIME" -> PowerHudConfig.showTime;
            case "VIT" -> PowerHudConfig.showVitality;
            case "BLOCK" -> PowerHudConfig.showBlock;
            case "TOOL" -> PowerHudConfig.showBestTool;
            case "INV" -> PowerHudConfig.showInventory;
            case "OXY" -> PowerHudConfig.showOxygen;
            case "BLOCK_STATS" -> PowerHudConfig.showBlockStats;
            case "GAMEMODE" -> PowerHudConfig.showGamemode;
            case "SPACE" -> true;
            default -> false;
        };
    }
}























