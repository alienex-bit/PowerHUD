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
        
        List<Renderable> lCol = new ArrayList<>();
        List<Renderable> cCol = new ArrayList<>();
        List<Renderable> rCol = new ArrayList<>();
        
        // Build columns from layout entries
        for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
            if (entry.id.equals(TEXT_SPACE)) {
                Renderable r = new Renderable(
                    null,
                    null,
                    0,
                    TEXT_SPACE,
                    entry.alignment,
                    entry.spacerHeight,
                    true
                );
                
                if (entry.alignment == ALIGN_LEFT) lCol.add(r);
                else if (entry.alignment == ALIGN_CENTER) cCol.add(r);
                else rCol.add(r);
                continue;
            }
            
            boolean force = HudOrderScreen.isWorkbenchActive;
            if (!shouldShow(entry.id) && !force) continue;
            
            HudLineRaw line = getRaw(entry.id, theme);
            if (line == null && !force) continue;
            if (line == null) {
                line = new HudLineRaw(entry.id, TEXT_PREVIEW, theme);
            }
            
            Renderable r = new Renderable(
                line.title,
                line.value,
                line.valColor,
                entry.id,
                entry.alignment,
                0,
                false
            );
            
            if (entry.alignment == ALIGN_LEFT) lCol.add(r);
            else if (entry.alignment == ALIGN_CENTER) cCol.add(r);
            else rCol.add(r);
        }
        
        // Render all columns
        dc.getMatrices().push();
        dc.getMatrices().scale(s, s, MATRIX_SCALE_Z);
        int sw = (int)(client.getWindow().getScaledWidth() / s);
        
        renderCol(dc, ren, lCol, sw, theme, s, ALIGN_LEFT);
        renderCol(dc, ren, cCol, sw, theme, s, ALIGN_CENTER);
        renderCol(dc, ren, rCol, sw, theme, s, ALIGN_RIGHT);
        
        dc.getMatrices().pop();
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
            // Special handling for oxygen (now single line format)
            else if (item.id.equals("OXY")) {
                int lineW = getWidth(item.value, ren, false);
                int modX = (align == ALIGN_LEFT) ? SPACING_HUD_TOP
                    : (align == ALIGN_CENTER ? (sw/2 - lineW/2)
                    : sw - lineW - SPACING_HUD_TOP);

                boolean shouldRender = HudOrderScreen.isWorkbenchActive
                    || !item.value.isEmpty();

                if (shouldRender) {
                    drawStyledText(dc, ren, item.value, modX, currentY + hAdj, item.valColor, s, false, now);
                    currentY += spacing;
                }
            }
            // Standard line rendering
            else {
                int valW = getWidth(item.value, ren, false);
                int dotW = (item.id.equals("FPS") && PowerHudConfig.showFpsDot) ? HUD_DOT_WIDTH : 0;
                int iconW = (item.id.equals("TOOL") && !HudData.toolStack.isEmpty()) ? HUD_ICON_WIDTH : 0;
                int lineW = getWidth(item.title + ": ", ren, PowerHudConfig.boldTitles) + valW + dotW + iconW + HUD_PADDING;
                int modX = (align == ALIGN_LEFT) ? SPACING_HUD_TOP 
                    : (align == ALIGN_CENTER ? (sw/2 - lineW/2) 
                    : sw - lineW - SPACING_HUD_TOP);
                
                boolean shouldRender = HudOrderScreen.isWorkbenchActive 
                    || (!(item.id.equals("BLOCK") && item.value.equals(TEXT_AIR)) 
                    && !(item.id.equals("TOOL") && item.value.isEmpty()));
                
                if (shouldRender) {
                    drawStyledText(
                        dc,
                        ren,
                        item.title + ": ",
                        modX,
                        currentY + hAdj,
                        tColor,
                        s,
                        PowerHudConfig.boldTitles,
                        now
                    );
                    
                    int curX = modX + getWidth(item.title + ": ", ren, PowerHudConfig.boldTitles);
                    
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
                : new HudLineRaw("Oxygen Level", HudData.oxyStr.isEmpty() ? TEXT_OXYGEN_HOLDING : HudData.oxyStr, HudData.oxyColor));
            case "BLOCK_STATS" -> new HudLineRaw("Blocks", HudData.blockStatsStr, theme);
            case "GAMEMODE" -> new HudLineRaw("Mode", HudData.gamemodeStr, theme);
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

    private int getWidth(String t, TextRenderer ren, boolean b) {
        return ren.getWidth(
            Text.literal(t).setStyle(
                Style.EMPTY.withFont(FONTS[PowerHudConfig.fontIndex]).withBold(b)
            )
        );
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
            default -> false;
        };
    }
}