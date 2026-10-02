package net.steve.powerhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import static net.steve.powerhud.HudConstants.*;

public class HudRenderer {
    public static final KeyMapping.Category POWERHUD_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("powerhud", "powerhud"));

    private static final Identifier[] FONTS = {
        null,
        Identifier.fromNamespaceAndPath("powerhud", "jetbrainsmono-regular"),
        Identifier.fromNamespaceAndPath("powerhud", "robotomono-regular"),
        Identifier.fromNamespaceAndPath("powerhud", "firacode-regular"),
        Identifier.fromNamespaceAndPath("powerhud", "cascadiacode"),
        Identifier.fromNamespaceAndPath("powerhud", "sourcecodepro-regular"),
        Identifier.fromNamespaceAndPath("powerhud", "comicmono"),
        Identifier.fromNamespaceAndPath("powerhud", "monofur"),
        Identifier.fromNamespaceAndPath("powerhud", "ubuntumono"),
        Identifier.fromNamespaceAndPath("powerhud", "intermono")
    };
    
    private static KeyMapping configKey, toggleKey, resetKey, debugKey;
    
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
        configKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.powerhud.open_config", InputConstants.KEY_U, POWERHUD_CATEGORY)
        );
        toggleKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.powerhud.toggle_hud", InputConstants.KEY_H, POWERHUD_CATEGORY)
        );
        resetKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.powerhud.reset_fps", InputConstants.KEY_R, POWERHUD_CATEGORY)
        );
        debugKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.powerhud.cycle_debug", InputConstants.KEY_Z, POWERHUD_CATEGORY)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null) return;

            while (configKey.consumeClick()) {
                if (client.gui != null) {
                    client.gui.setScreen(new PowerHudConfigScreen());
                }
            }

            while (toggleKey.consumeClick()) {
                PowerHudConfig.hudEnabled = !PowerHudConfig.hudEnabled;
                PowerHudConfig.save();
            }

            while (resetKey.consumeClick()) {
                HudData.resetFps();
            }

            while (debugKey.consumeClick()) {
                PowerHudConfig.debugTab++;
                if (PowerHudConfig.debugTab > 3) {
                    PowerHudConfig.debugTab = 0;
                }
            }
        });
    }

    public static void render(GuiGraphicsExtractor dc, DeltaTracker tc) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        
        HudData.update(client);
        
        // Render debug screen if enabled
        if (PowerHudConfig.debugTab > 0) {
            F3ScreenRenderer.render(dc, PowerHudConfig.debugTab);
        }
        
        // Avoid double-rendering while the HUD workbench is open
        boolean inWorkbench = (client.gui != null && client.gui.screen() instanceof HudOrderScreen);
        HudOrderScreen.isWorkbenchActive = inWorkbench;
        if (inWorkbench) {
            return;
        }

        // Don't render HUD if disabled or hidden
        if (!PowerHudConfig.hudEnabled || (client.gui != null && client.gui.hud != null && client.gui.hud.isHidden())) {
            return;
        }

        renderMainHud(dc, client);
    }

    public static void renderMainHud(GuiGraphicsExtractor dc, Minecraft client) {
        Font ren = client.font;
        int theme = PowerHudConfig.getPresetDataColor();
        float s = PowerHudConfig.hudScaleVert / SCALE_DIVISOR;
        
        dc.pose().pushMatrix();
        dc.pose().scale(s, s);
        int sw = (int)(dc.guiWidth() / s);

        long now = System.currentTimeMillis();
        int tColor = PowerHudConfig.getPresetTitleColor();
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

        dc.pose().popMatrix();

        // Render oxygen as standalone centered overlay OUTSIDE the scaled matrix
        // This ensures consistent positioning regardless of HUD scale or vanilla air bubble state
        if (PowerHudConfig.showOxygen) {
            renderOxygenOverlay(dc, ren, dc.guiWidth());
        }
    }

    public static void renderElementAt(
        GuiGraphicsExtractor dc,
        Font ren,
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
            Minecraft client = Minecraft.getInstance();
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
            int extraPad = 8;
            int boxRows = isCreative ? 1 : 4;
            int boxH = boxRows * INV_ROW_HEIGHT + INV_GRID_OFFSET + 2 + extraPad;
            if (PowerHudConfig.boxStyle != null && PowerHudConfig.boxStyle != PowerHudConfig.BoxStyle.OFF) {
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
            // Enforce dark background for accessibility presets
            if (PowerHudConfig.forceDarkBackground()) {
                int boxColor = COLOR_BACKGROUND_DARK;
                int boxX = renderX - 2;
                int boxY = y + hAdj - 1;
                int boxW = totalW + 4;
                int boxH = 10;
                dc.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxColor);
            } else if (PowerHudConfig.boxStyle != null && PowerHudConfig.boxStyle != PowerHudConfig.BoxStyle.OFF) {
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
                int boxY = y + hAdj - 1;
                int boxW = totalW + 4;
                int boxH = 10;
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

            // Bold data if accessibility preset is active
            drawStyledText(dc, ren, line.value, curX, y + hAdj, line.valColor, s, PowerHudConfig.isPresetBoldData(), now);

            if (iconW > 0) {
                dc.item(HudData.toolStack, curX + valW + HUD_ICON_OFFSET, y + hAdj - HUD_ICON_OFFSET);
            }
        }
    }

    public static int resolveX(int x, int width, int sw) {
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

    public record HudLineRaw(String title, String value, int valColor) {}

    public static HudLineRaw getRaw(String id, int theme) {
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

    private static void drawFpsDot(GuiGraphicsExtractor dc, Font ren, int x, int y, long now) {
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
        
        dc.pose().pushMatrix();
        dc.pose().translate(x + FPS_DOT_OFFSET_X, y + FPS_DOT_OFFSET_Y);
        dc.pose().scale(FPS_DOT_SCALE * pulse, FPS_DOT_SCALE * pulse);
        dc.pose().translate(-FPS_DOT_OFFSET_X, -FPS_DOT_OFFSET_Y);
        dc.text(ren, Component.literal(FPS_DOT_CHAR), 0, 0, dotColor, true);
        dc.pose().popMatrix();
    }

    public static void drawStyledText(
        GuiGraphicsExtractor dc,
        Font ren,
        String t,
        int x,
        int y,
        int c,
        float s,
        boolean b,
        long now
    ) {
        Identifier f = FONTS[PowerHudConfig.fontIndex];
        Style st = Style.EMPTY.withFont(f != null ? new FontDescription.Resource(f) : FontDescription.DEFAULT).withBold(b);
        dc.text(ren, Component.literal(t).setStyle(st), x, y, c, false);
    }

    public static Identifier getHudFont() {
        return FONTS[PowerHudConfig.fontIndex];
    }

    public static int getHudTextWidth(Font ren, String text, boolean bold) {
        Identifier f = getHudFont();
        Style st = Style.EMPTY.withFont(f != null ? new FontDescription.Resource(f) : FontDescription.DEFAULT).withBold(bold);
        return ren.width(Component.literal(text).setStyle(st));
    }

    public static int getWidth(String t, Font ren, boolean b) {
        return getHudTextWidth(ren, t, b);
    }

    private static void renderOxygenOverlay(GuiGraphicsExtractor dc, Font ren, int sw) {
        String overlay = HudData.oxyOverlayStr;
        String status = HudData.oxyStatusStr;

        if (overlay.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int sh = dc.guiHeight();

        String centerMsg = "Oxygen: " + overlay + " - " + status;
        int textW = getWidth(centerMsg, ren, false);
        int textH = ren.lineHeight;

        int barW = 200;
        int barH = textH + 6;

        int barX = sw / 2 - barW / 2;
        int barY = sh - PowerHudConfig.oxygenOverlayY;

        int barBg = 0x99000000;
        dc.fill(barX, barY, barX + barW, barY + barH, barBg);

        int filled = (int)(barW * HudData.oxyPercent);
        if (filled > 0) {
            dc.fill(barX, barY, barX + filled, barY + barH, HudData.oxyColor);
        }

        int textX = barX + (barW - textW) / 2;
        int textY = barY + (barH - textH) / 2;

        Identifier f = FONTS[PowerHudConfig.fontIndex];
        Style st = Style.EMPTY.withFont(f != null ? new FontDescription.Resource(f) : FontDescription.DEFAULT);
        dc.text(ren, Component.literal(centerMsg).setStyle(st), textX + 1, textY + 1, 0xFF000000, false);
        drawStyledText(dc, ren, centerMsg, textX, textY, 0xFFFFFFFF, 1.0f, false, now);
    }

    public static boolean shouldShow(String id) {
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
