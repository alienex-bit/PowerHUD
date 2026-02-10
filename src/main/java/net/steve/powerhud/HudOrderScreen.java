package net.steve.powerhud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;

public class HudOrderScreen extends Screen {
    private final Screen parent;
    private final List<PowerHudConfig.LayoutEntry> tempOrder;
    public static boolean isWorkbenchActive = false;
    private boolean skipNextRender = false;

    public HudOrderScreen(Screen parent) {
        super(Text.literal("HUD Workbench"));
        this.parent = parent;
        this.tempOrder = dedupe(new ArrayList<>(PowerHudConfig.hudOrder));
    }

    private List<PowerHudConfig.LayoutEntry> dedupe(List<PowerHudConfig.LayoutEntry> entries) {
        LinkedHashMap<String, PowerHudConfig.LayoutEntry> map = new LinkedHashMap<>();
        for (PowerHudConfig.LayoutEntry e : entries) {
            if ("SPACE".equals(e.id)) {
                String sid = "SPACE_" + java.util.UUID.randomUUID().toString();
                map.put(sid, new PowerHudConfig.LayoutEntry("SPACE", e.spacerHeight, e.alignment));
            } else {
                map.put(e.id, new PowerHudConfig.LayoutEntry(e.id, e.spacerHeight, e.alignment));
            }
        }
        return new ArrayList<>(map.values());
    }

    @Override
    protected void init() {
        skipNextRender = true;
        isWorkbenchActive = true;
        this.clearChildren();
        
        int cw = this.width / 3;
        int sy = 85;
        int eh = 15;
        
        renderCol(0, 10, sy, cw - 20, eh);
        renderCol(1, cw + 10, sy, cw - 20, eh);
        renderCol(2, (cw * 2) + 10, sy, cw - 20, eh);
        
        int slotY = this.height - 40;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Default"), b -> {
            PowerHudConfig.resetToVanilla();
            tempOrder.clear();
            tempOrder.addAll(dedupe(PowerHudConfig.hudOrder));
            init();
        }).dimensions(this.width/2 - 160, slotY + 5, 120, 16)
          .tooltip(Tooltip.of(Text.literal("Restore default layout")))
          .build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(dedupe(tempOrder));
            PowerHudConfig.save();
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + 50, slotY + 5, 100, 16)
          .tooltip(Tooltip.of(Text.literal("Save and Exit")))
          .build());
    }

    private void renderCol(int align, int x, int y, int w, int h) {
        List<PowerHudConfig.LayoutEntry> items = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        for (int i = 0; i < tempOrder.size(); i++) {
            if (tempOrder.get(i).alignment == align) {
                items.add(tempOrder.get(i));
                indices.add(i);
            }
        }
        
        int cy = y;
        for (int i = 0; i < items.size(); i++) {
            final int lIdx = indices.get(i);
            final int locIdx = i;
            PowerHudConfig.LayoutEntry ent = items.get(i);
            String label = ent.id.equals("SPACE") ? "Spacer" : ent.id;
            
            String tip = switch(ent.id) {
                case "XYZ" -> "Co-Ordinates Element";
                case "FACING" -> "Facing Direction Element";
                case "BIOME" -> "Biome Type Element";
                case "TIME" -> "Time Until Element";
                case "VIT" -> "Current Vitality Element";
                case "BLOCK" -> "Block Type Element";
                case "OXY" -> "Oxygen Depletion Element";
                case "TOOL" -> "Recommend Tool Element";
                case "INV" -> "Inventory Display Element";
                case "FPS" -> "Frames Per Second Element";
                case "GAMEMODE" -> "Current Gamemode Element";
                case "BLOCK_STATS" -> "Block Stats Element";
                case "SPACE" -> "Vertical Space Element";
                default -> ent.id + " Element";
            };
            
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {})
                .dimensions(x, cy, w - 100, h - 1)
                .tooltip(Tooltip.of(Text.literal(tip)))
                .build()).active = false;
            
            int cx = x + w - 98;
            
            if (align > 0) {
                addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    ent.alignment--;
                    init();
                }).dimensions(cx, cy, 14, h-1)
                  .tooltip(Tooltip.of(Text.literal("Move Left")))
                  .build());
            }
            
            if (align < 2) {
                addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    ent.alignment++;
                    init();
                }).dimensions(cx + 15, cy, 14, h-1)
                  .tooltip(Tooltip.of(Text.literal("Move Right")))
                  .build());
            }
            
            addDrawableChild(ButtonWidget.builder(Text.literal("U"), b -> {
                move(align, locIdx, -1);
                init();
            }).dimensions(cx + 30, cy, 15, h-1)
              .tooltip(Tooltip.of(Text.literal("Move UP")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("D"), b -> {
                move(align, locIdx, 1);
                init();
            }).dimensions(cx + 46, cy, 15, h-1)
              .tooltip(Tooltip.of(Text.literal("Move DOWN")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
                tempOrder.add(lIdx + 1, new PowerHudConfig.LayoutEntry("SPACE", 10, align));
                init();
            }).dimensions(cx + 62, cy, 15, h-1)
              .tooltip(Tooltip.of(Text.literal("Add Spacer")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("x"), b -> {
                tempOrder.remove(lIdx);
                init();
            }).dimensions(cx + 78, cy, 15, h-1)
              .tooltip(Tooltip.of(Text.literal("Remove")))
              .build());
            
            cy += h + 1;
        }
    }

    private void move(int align, int locIdx, int dir) {
        List<Integer> colIdx = new ArrayList<>();
        for (int i = 0; i < tempOrder.size(); i++) {
            if (tempOrder.get(i).alignment == align) {
                colIdx.add(i);
            }
        }
        
        int target = locIdx + dir;
        if (target >= 0 && target < colIdx.size()) {
            Collections.swap(tempOrder, colIdx.get(locIdx), colIdx.get(target));
        }
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float t) {
        if (skipNextRender) {
            skipNextRender = false;
            this.renderBackground(dc, mx, my, t);
            dc.fill(0, 0, this.width, this.height, 0x77000000);
            super.render(dc, mx, my, t);
            return;
        }
        
        this.renderBackground(dc, mx, my, t);
        dc.fill(0, 0, this.width, this.height, 0x77000000);
        
        List<PowerHudConfig.LayoutEntry> backup = PowerHudConfig.hudOrder;
        PowerHudConfig.hudOrder = tempOrder;
        
        new HudRenderer().renderMainHud(dc, MinecraftClient.getInstance());
        
        PowerHudConfig.hudOrder = backup;
        
        int cw = this.width / 3;
        dc.drawCenteredTextWithShadow(textRenderer, "LEFT STACK", cw / 2, 72, 0xFFFFFFFF);
        dc.drawCenteredTextWithShadow(textRenderer, "CENTER STACK", this.width / 2, 72, 0xFFFFFFFF);
        dc.drawCenteredTextWithShadow(textRenderer, "RIGHT STACK", (this.width - cw / 2), 72, 0xFFFFFFFF);
        dc.fill(cw, 65, cw + 1, this.height - 40, 0x33FFFFFF);
        dc.fill(cw * 2, 65, cw * 2 + 1, this.height - 40, 0x33FFFFFF);
        
        super.render(dc, mx, my, t);
    }

    @Override
    public void renderBackground(DrawContext dc, int mx, int my, float d) {}
}