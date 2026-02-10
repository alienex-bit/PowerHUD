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
        float s = PowerHudConfig.hudScaleVert / 100f;
        
        List<Renderable> lCol = new ArrayList<>();
        List<Renderable> cCol = new ArrayList<>();
        List<Renderable> rCol = new ArrayList<>();
        
        // Build columns from layout entries
        for (PowerHudConfig.LayoutEntry entry : PowerHudConfig.hudOrder) {
            if (entry.id.equals("SPACE")) {
                Renderable r = new Renderable(
                    null,
                    null,
                    0,
                    "SPACE",
                    entry.alignment,
                    entry.spacerHeight,
                    true
                );
                
                if (entry.alignment == 0) lCol.add(r);
                else if (entry.alignment == 1) cCol.add(r);
                else rCol.add(r);
                continue;
            }
            
            boolean force = HudOrderScreen.isWorkbenchActive;
            if (!shouldShow(entry.id) && !force) continue;
            
            HudLineRaw line = getRaw(entry.id, theme);
            if (line == null && !force) continue;
            if (line == null) {
                line = new HudLineRaw(entry.id, "PREVIEW", theme);
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
            
            if (entry.alignment == 0) lCol.add(r);
            else if (entry.alignment == 1) cCol.add(r);
            else rCol.add(r);
        }
        
        // Render all columns
        dc.getMatrices().push();
        dc.getMatrices().scale(s, s, 1.0f);
        int sw = (int)(client.getWindow().getScaledWidth() / s);
        
        renderCol(dc, ren, lCol, sw, theme, s, 0);
        renderCol(dc, ren, cCol, sw, theme, s, 1);
        renderCol(dc, ren, rCol, sw, theme, s, 2);
        
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
        int currentY = 5;
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
                int modX = (align == 0) ? 5 : (align == 1 ? (sw/2 - 31) : sw - 67);
                
                renderModularBackground(dc, modX, currentY, 62, 31, theme, item.id);
                drawStyledText(dc, ren, "Inventory", modX, currentY, tColor, s, PowerHudConfig.boldTitles, now);
                
                // Draw inventory grid
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 9; c++) {
                        int i = r * 9 + c;
                        int sx = modX + (int)(c * 7.0);
                        int sy = currentY + 11 + (int)(r * 7.0);
                        
                        dc.fill(
                            sx,
                            sy,
                            sx + 5,
                            sy + 5,
                            HudData.invSlots[i] ? HudData.invColor : 0x44FFFFFF
                        );
                    }
                }
                
                currentY += 35;
            }
            // Special handling for oxygen bar
            else if (item.id.equals("OXY")) {
                int barW = getWidth("Holding Breath...", ren, false);
                int modX = (align == 0) ? 5 : (align == 1 ? (sw/2 - barW/2) : sw - barW - 5);
                String previewVal = item.value.equals("PREVIEW") ? "Holding Breath..." : item.value;
                
                renderModularBackground(dc, modX, currentY, barW, 22 + hAdj, theme, item.id);
                
                int barColor = (item.valColor & 0x00FFFFFF) | 0xA5000000;
                int fillW = (int)((barW + 6) * HudData.oxyPercent);
                
                if (fillW > 0) {
                    dc.fill(
                        modX - 2,
                        currentY - 2,
                        (modX - 2) + fillW,
                        currentY + 22 + hAdj + 1,
                        barColor
                    );
                }
                
                drawStyledText(dc, ren, item.title, modX, currentY + hAdj, tColor, s, PowerHudConfig.boldTitles, now);
                drawStyledText(
                    dc,
                    ren,
                    previewVal,
                    modX + (barW - getWidth(previewVal, ren, false)) / 2,
                    currentY + hAdj + 10,
                    0xFFFFFFFF,
                    s,
                    false,
                    now
                );
                
                currentY += 22 + (spacing - 12);
            }
            // Standard line rendering
            else {
                int valW = getWidth(item.value, ren, false);
                int dotW = (item.id.equals("FPS") && PowerHudConfig.showFpsDot) ? 10 : 0;
                int iconW = (item.id.equals("TOOL") && !HudData.toolStack.isEmpty()) ? 18 : 0;
                int lineW = getWidth(item.title + ": ", ren, PowerHudConfig.boldTitles) + valW + dotW + iconW + 5;
                int modX = (align == 0) ? 5 : (align == 1 ? (sw/2 - lineW/2) : sw - lineW - 5);
                
                boolean shouldRender = HudOrderScreen.isWorkbenchActive 
                    || (!(item.id.equals("BLOCK") && item.value.equals("Air")) 
                    && !(item.id.equals("TOOL") && item.value.isEmpty()));
                
                if (shouldRender) {
                    renderModularBackground(dc, modX, currentY, lineW, 9 + hAdj, theme, item.id);
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
                        dc.drawItem(HudData.toolStack, curX + valW + 4, currentY + hAdj - 4);
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
            case "VIT" -> new HudLineRaw("Vitality", HudData.vitStr, 0xFF55FF55);
            case "BLOCK" -> new HudLineRaw(HudData.targetType, HudData.blockStr, theme);
            case "TOOL" -> new HudLineRaw("Best Tool", HudData.toolStr, theme);
            case "INV" -> new HudLineRaw("Inventory", HudData.invStr, HudData.invColor);
            case "OXY" -> (HudData.oxyStr.isEmpty() && !HudOrderScreen.isWorkbenchActive 
                ? null 
                : new HudLineRaw("Oxygen Level", HudData.oxyStr.isEmpty() ? "Holding Breath..." : HudData.oxyStr, HudData.oxyColor));
            case "BLOCK_STATS" -> new HudLineRaw("Blocks", HudData.blockStatsStr, theme);
            case "GAMEMODE" -> new HudLineRaw("Mode", HudData.gamemodeStr, theme);
            default -> null;
        };
    }

    private void drawFpsDot(DrawContext dc, TextRenderer ren, int x, int y, long now) {
        int dotColor = HudData.fpsColor;
        float phase = (float)(Math.sin(now / 100.0) * 0.5 + 0.5);
        float pulse = 1.0f + 0.4f * phase;
        
        int r = (dotColor >> 16) & 255;
        int g = (dotColor >> 8) & 255;
        int b = dotColor & 255;
        
        r = r + (int)((255 - r) * phase);
        g = g + (int)((255 - g) * phase);
        b = b + (int)((255 - b) * phase);
        
        dotColor = (0xFF << 24) | (r << 16) | (g << 8) | b;
        
        dc.getMatrices().push();
        dc.getMatrices().translate(x + 4, y + 4, 0);
        dc.getMatrices().scale(1.5f * pulse, 1.5f * pulse, 1.0f);
        dc.getMatrices().translate(-4, -4, 0);
        dc.drawText(ren, Text.literal("\u25CF"), 0, 0, dotColor, true);
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
        
        if (PowerHudConfig.textEffect == PowerHudConfig.TextEffect.OFF) {
            int off = (s > 1.4f) ? 2 : 1;
            dc.drawText(ren, Text.literal(t).setStyle(st), x + off, y + off, (c & 0xFF000000) >> 2 | 0x000000, false);
            dc.drawText(ren, Text.literal(t).setStyle(st), x, y, c, true);
            return;
        }
        
        int curX = x;
        for (int i = 0; i < t.length(); i++) {
            String ch = String.valueOf(t.charAt(i));
            int color = c;
            int dX = curX;
            int dY = y;
            
            switch (PowerHudConfig.textEffect) {
                case SHIMMER -> color = hsbToRgb(
                    0.0f,
                    0.0f,
                    Math.min(1f, 1f * (0.75f + 0.25f * (float)Math.sin(now / 220.0 + i * 0.45)))
                );
                case CHROMA -> color = hsbToRgb(
                    ((now + i * 90L) % 2400L) / 2400.0f,
                    0.65f,
                    1.0f
                );
            }
            
            dc.drawText(ren, Text.literal(ch).setStyle(st), dX, dY, color, true);
            curX += ren.getWidth(Text.literal(ch).setStyle(st));
        }
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

    private void renderModularBackground(
        DrawContext dc,
        int x,
        int y,
        int w,
        int h,
        int theme,
        String id
    ) {
        int boxAlpha = switch(PowerHudConfig.boxStyle) {
            case OFF -> 0x00000000;
            case MIST -> 0x11000000;
            case HAZE -> 0x33000000;
            case DUSK -> 0x55000000;
            case OBSIDIAN -> 0xAA000000;
            case SOLID -> 0xDD000000;
        };
        
        dc.fill(x - 2, y - 2, x + w + 3, y + h + 1, boxAlpha);
        
        if (PowerHudConfig.roundCorners) {
            dc.fill(x - 2, y - 2, x - 1, y - 1, 0x00000000);
            dc.fill(x + w + 2, y - 2, x + w + 3, y - 1, 0x00000000);
            dc.fill(x - 2, y + h, x - 1, y + h + 1, 0x00000000);
            dc.fill(x + w + 2, y + h, x + w + 3, y + h + 1, 0x00000000);
        }
    }

    private static int hsbToRgb(float h, float s, float b) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b);
        return 0xFF000000 | rgb;
    }
}