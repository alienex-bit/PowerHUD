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
    private boolean skipNextRender = false;

    public HudOrderScreen(Screen parent) {
        super(Text.literal("HUD Workbench"));
        this.parent = parent;
        this.tempOrder = dedupe(new ArrayList<>(PowerHudConfig.hudOrder));
    }

    private List<PowerHudConfig.LayoutEntry> dedupe(List<PowerHudConfig.LayoutEntry> entries) {
        LinkedHashMap<String, PowerHudConfig.LayoutEntry> map = new LinkedHashMap<>();
        for (PowerHudConfig.LayoutEntry e : entries) {
            if (TEXT_SPACE.equals(e.id)) {
                String sid = TEXT_SPACE + "_" + java.util.UUID.randomUUID().toString();
                map.put(sid, new PowerHudConfig.LayoutEntry(TEXT_SPACE, e.spacerHeight, e.alignment));
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
        int sy = UI_COLUMN_START_Y;
        int eh = BUTTON_HEIGHT_ELEMENT;
        
        renderCol(ALIGN_LEFT, 10, sy, cw - 20, eh);
        renderCol(ALIGN_CENTER, cw + 10, sy, cw - 20, eh);
        renderCol(ALIGN_RIGHT, (cw * 2) + 10, sy, cw - 20, eh);
        
        int slotY = this.height - UI_BOTTOM_OFFSET;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Default"), b -> {
            PowerHudConfig.resetToVanilla();
            tempOrder.clear();
            tempOrder.addAll(dedupe(PowerHudConfig.hudOrder));
            init();
        }).dimensions(this.width/2 - 160, slotY + SPACING_HUD_TOP, BUTTON_WIDTH_ACTION, BUTTON_HEIGHT_TALL)
          .tooltip(Tooltip.of(Text.literal("Restore default layout")))
          .build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            PowerHudConfig.hudOrder.clear();
            PowerHudConfig.hudOrder.addAll(dedupe(tempOrder));
            PowerHudConfig.save();
            isWorkbenchActive = false;
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + SCREEN_CENTER_OFFSET + 45, slotY + SPACING_HUD_TOP, BUTTON_WIDTH_SMALL, BUTTON_HEIGHT_TALL)
          .tooltip(Tooltip.of(Text.literal("Save and Exit")))
          .build());
    }

    private void renderCol(int align, int x, int y, int w, int h) {
        List<PowerHudConfig.LayoutEntry> items = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        for (int i = 0; i < tempOrder.size(); i++) {
            PowerHudConfig.LayoutEntry entry = tempOrder.get(i);
            // Skip oxygen - it's now a standalone overlay, not part of columns
            if (entry.id.equals("OXY")) {
                continue;
            }
            if (entry.alignment == align) {
                items.add(entry);
                indices.add(i);
            }
        }
        
        int cy = y;
        for (int i = 0; i < items.size(); i++) {
            final int lIdx = indices.get(i);
            final int locIdx = i;
            PowerHudConfig.LayoutEntry ent = items.get(i);
            String label = ent.id.equals(TEXT_SPACE) ? "Spacer" : ent.id;
            
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
            
            if (align > ALIGN_LEFT) {
                addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    ent.alignment--;
                    init();
                }).dimensions(cx, cy, BUTTON_WIDTH_TINY, h - 1)
                  .tooltip(Tooltip.of(Text.literal("Move Left")))
                  .build());
            }
            
            if (align < ALIGN_RIGHT) {
                addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    ent.alignment++;
                    init();
                }).dimensions(cx + BUTTON_HEIGHT_ELEMENT, cy, BUTTON_WIDTH_TINY, h - 1)
                  .tooltip(Tooltip.of(Text.literal("Move Right")))
                  .build());
            }
            
            addDrawableChild(ButtonWidget.builder(Text.literal("U"), b -> {
                move(align, locIdx, -1);
                init();
            }).dimensions(cx + 30, cy, BUTTON_WIDTH_COMPACT, h - 1)
              .tooltip(Tooltip.of(Text.literal("Move UP")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("D"), b -> {
                move(align, locIdx, 1);
                init();
            }).dimensions(cx + 46, cy, BUTTON_WIDTH_COMPACT, h - 1)
              .tooltip(Tooltip.of(Text.literal("Move DOWN")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
                tempOrder.add(lIdx + 1, new PowerHudConfig.LayoutEntry(TEXT_SPACE, 10, align));
                init();
            }).dimensions(cx + 62, cy, BUTTON_WIDTH_COMPACT, h - 1)
              .tooltip(Tooltip.of(Text.literal("Add Spacer")))
              .build());
            
            addDrawableChild(ButtonWidget.builder(Text.literal("x"), b -> {
                tempOrder.remove(lIdx);
                init();
            }).dimensions(cx + 78, cy, BUTTON_WIDTH_COMPACT, h - 1)
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
            dc.fill(0, 0, this.width, this.height, COLOR_BACKGROUND_OVERLAY);
            super.render(dc, mx, my, t);
            return;
        }
        
        this.renderBackground(dc, mx, my, t);
        dc.fill(0, 0, this.width, this.height, COLOR_BACKGROUND_OVERLAY);
        
        List<PowerHudConfig.LayoutEntry> backup = PowerHudConfig.hudOrder;
        PowerHudConfig.hudOrder = tempOrder;
        
        new HudRenderer().renderMainHud(dc, MinecraftClient.getInstance());
        
        PowerHudConfig.hudOrder = backup;
        
        int cw = this.width / 3;
        dc.drawCenteredTextWithShadow(textRenderer, "LEFT STACK", cw / 2, UI_HEADER_Y, COLOR_TEXT_WHITE);
        dc.drawCenteredTextWithShadow(textRenderer, "CENTER STACK", this.width / 2, UI_HEADER_Y, COLOR_TEXT_WHITE);
        dc.drawCenteredTextWithShadow(textRenderer, "RIGHT STACK", (this.width - cw / 2), UI_HEADER_Y, COLOR_TEXT_WHITE);
        dc.fill(cw, UI_SEPARATOR_Y, cw + 1, this.height - UI_BOTTOM_OFFSET, COLOR_SEPARATOR);
        dc.fill(cw * 2, UI_SEPARATOR_Y, cw * 2 + 1, this.height - UI_BOTTOM_OFFSET, COLOR_SEPARATOR);
        
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