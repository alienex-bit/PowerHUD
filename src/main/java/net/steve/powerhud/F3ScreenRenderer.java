package net.steve.powerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class F3ScreenRenderer {
    
    // VS CODE PALETTE
    private static final int COL_BG = 0xE61A1A1A;       
    private static final int COL_BORDER = 0xFF007ACC;   
    private static final int COL_TITLE = 0xFFFFFFFF;    
    private static final int COL_TAB_ACT = 0xFFD4D4D4;  
    private static final int COL_TAB_INA = 0xFF808080;  
    private static final int COL_LBL = 0xFF4EC9B0;      
    private static final int COL_VAL_TXT = 0xFFCE9178;  
    private static final int COL_VAL_NUM = 0xFFB5CEA8;  
    private static final int COL_WARN = 0xFFFFC66D;     
    private static final int COL_CRIT = 0xFFFF6B68;     
    private static final int COL_DIM = 0xFF858585;      

    public static void render(GuiGraphicsExtractor dc, int activeTab) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        HudData.update(client);
        Font ren = client.font;
        
        // SCALING: Smaller text (0.85x) for data density
        dc.pose().pushMatrix();
        float scale = 0.85f;
        dc.pose().scale(scale, scale);
        
        int w = (int)(dc.guiWidth() / scale);
        int h = (int)(dc.guiHeight() / scale);
        
        // LARGER BOX for more data
        int boxW = 300;
        int boxH = 210;
        int x = (w - boxW) / 2;
        int y = (h - boxH) / 2;
        
        dc.fill(x, y, x + boxW, y + boxH, COL_BG); 
        dc.outline(x, y, boxW, boxH, COL_BORDER);
        
        // HEADER
        dc.centeredText(ren, "PowerHUD Debug", w / 2, y + 8, COL_TITLE);
        
        // TABS
        int tabY = y + 24;
        int tabW = boxW / 3;
        
        int t1c = (activeTab == 1) ? COL_TAB_ACT : COL_TAB_INA;
        int t2c = (activeTab == 2) ? COL_TAB_ACT : COL_TAB_INA;
        int t3c = (activeTab == 3) ? COL_TAB_ACT : COL_TAB_INA;
        
        // Centered Tab Titles
        dc.text(ren, "[System]", x + (tabW * 0) + (tabW / 2) - (ren.width("[System]") / 2), tabY, t1c, true);
        dc.text(ren, "[Render]", x + (tabW * 1) + (tabW / 2) - (ren.width("[Render]") / 2), tabY, t2c, true);
        dc.text(ren, "[State]",  x + (tabW * 2) + (tabW / 2) - (ren.width("[State]") / 2), tabY, t3c, true);
        
        int cy = tabY + 20;
        int leftX = x + 15;
        
        switch (activeTab) {
            case 1 -> { // SYSTEM & PERFORMANCE
                long maxMem = Runtime.getRuntime().maxMemory();
                long totalMem = Runtime.getRuntime().totalMemory();
                long freeMem = Runtime.getRuntime().freeMemory();
                long usedMem = totalMem - freeMem;
                int usedMB = (int)(usedMem / 1024L / 1024L);
                int maxMB = (int)(maxMem / 1024L / 1024L);
                int percent = (maxMem > 0) ? (int)((usedMem * 100L) / maxMem) : 0;
                int memColor = (percent > 85) ? COL_CRIT : (percent > 70 ? COL_WARN : COL_VAL_NUM);

                drawLabelValue(dc, ren, "Memory: ", usedMB + "MB / " + maxMB + "MB", leftX, cy, COL_LBL, memColor); cy += 10;
                drawLabelValue(dc, ren, "Usage: ", percent + "%", leftX, cy, COL_LBL, memColor); cy += 15;
                
                drawLabelValue(dc, ren, "CPU: ", HudData.cpuName, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 10;
                drawLabelValue(dc, ren, "GPU: ", HudData.gpuName, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 10;
                drawLabelValue(dc, ren, "Display: ", HudData.displayInfo, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 15;
                
                drawLabelValue(dc, ren, "Java: ", System.getProperty("java.version"), leftX, cy, COL_LBL, COL_DIM); cy += 10;
                drawLabelValue(dc, ren, "Mod Loader: ", "Fabric", leftX, cy, COL_LBL, COL_DIM); 
            }
                
            case 2 -> { // SIMULATION & RENDERING
                float ms = 1000.0f / Math.max(HudData.currentFps, 1);
                String fpsDebug = HudData.currentFps + " | Avg " + (int)HudData.avgFps + " | Min " + HudData.minFps + " | Max " + HudData.maxFps + " | " + String.format("%.1f", ms) + "ms";
                
                drawLabelValue(dc, ren, "FPS: ", fpsDebug, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 15;
                
                drawLabelValue(dc, ren, "Entities: ", HudData.entityCount, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 10;
                drawLabelValue(dc, ren, "Particles: ", HudData.particleCount, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 10;
                drawLabelValue(dc, ren, "Chunks: ", HudData.chunkStats, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 15;
                
                drawLabelValue(dc, ren, "Sounds: ", HudData.soundStats, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 10;
                drawLabelValue(dc, ren, "Distances: ", "R=" + client.options.renderDistance().get() + ", S=" + client.options.simulationDistance().get(), leftX, cy, COL_LBL, COL_VAL_NUM);
                
                // --- FPS HISTORY GRAPH (SCROLLING 50s) ---
                int graphW = 250;
                int graphH = 45;
                int graphX = x + (boxW - graphW) / 2;
                int graphY = y + boxH - 75; 
                
                dc.fill(graphX, graphY, graphX + graphW, graphY + graphH, 0x88000000);
                dc.outline(graphX, graphY, graphW, graphH, 0x44FFFFFF);
                dc.text(ren, "History (50s)", graphX + 2, graphY + 2, COL_DIM, true);
                
                int maxGraphY = 240; 
                int historySize = HudData.fpsGraph.size();
                
                if (historySize > 0) {
                    int startX = graphX + graphW - 1;
                    for (int i = 0; i < graphW && i < historySize; i++) {
                        int index = historySize - 1 - i;
                        int val = HudData.fpsGraph.get(index);
                        
                        int barH = (int)((val / (float)maxGraphY) * (graphH - 2));
                        barH = Math.min(barH, graphH - 2); 
                        if (barH < 1) barH = 1;
                        
                        int col;
                        if (val < PowerHudConfig.redThresh) col = 0xFFFF5555;
                        else if (val < PowerHudConfig.orangeThresh) col = 0xFFFFAA00;
                        else if (val < PowerHudConfig.yellowThresh) col = 0xFFFFFF55;
                        else col = 0xFF55FF55;
                        
                        int drawX = startX - i;
                        dc.fill(drawX, graphY + graphH - 1 - barH, drawX + 1, graphY + graphH - 1, col | 0xC0000000);
                    }
                }
            }
                
            case 3 -> { // PLAYER & WORLD STATE
                drawLabelValue(dc, ren, "XYZ: ", HudData.coordsStr, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 10;
                drawLabelValue(dc, ren, "Facing: ", HudData.dirStr, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 10;
                drawLabelValue(dc, ren, "Biome: ", HudData.biomeStr, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 15;
                
                drawLabelValue(dc, ren, "Time: ", HudData.timeStr + " (Day " + HudData.dayStr + ")", leftX, cy, COL_LBL, COL_VAL_NUM); cy += 10;
                drawLabelValue(dc, ren, "Light: ", HudData.lightStr, leftX, cy, COL_LBL, COL_VAL_NUM); cy += 10;
                drawLabelValue(dc, ren, "Flags: ", HudData.moveFlags, leftX, cy, COL_LBL, COL_DIM); cy += 10;
                drawLabelValue(dc, ren, "Effects: ", HudData.effectList, leftX, cy, COL_LBL, COL_VAL_TXT); cy += 15;
                
                String target = (HudData.blockStr == null) ? "-" : HudData.blockStr;
                drawLabelValue(dc, ren, "Target: ", target + " (" + HudData.targetType + ")", leftX, cy, COL_LBL, COL_VAL_TXT); cy += 10;
                
                if (!HudData.blockProps.isEmpty()) {
                    String props = HudData.blockProps;
                    if (props.length() > 45) {
                        dc.text(ren, props.substring(0, 45), leftX, cy, COL_DIM, true); cy += 9;
                        int end = Math.min(props.length(), 90);
                        dc.text(ren, props.substring(45, end), leftX, cy, COL_DIM, true);
                    } else {
                        dc.text(ren, props, leftX, cy, COL_DIM, true);
                    }
                }
            }
        }
        
        dc.centeredText(ren, "Press 'Z' to Cycle", w / 2, y + boxH - 12, COL_DIM);
        dc.pose().popMatrix();
    }
    
    private static void drawLabelValue(GuiGraphicsExtractor dc, Font ren, String label, String value, int x, int y, int lCol, int vCol) {
        dc.text(ren, label, x, y, lCol, true);
        int offset = ren.width(label);
        dc.text(ren, value, x + offset, y, vCol, true);
    }
}
