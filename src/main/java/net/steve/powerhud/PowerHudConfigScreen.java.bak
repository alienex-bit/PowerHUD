package net.steve.powerhud;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen; import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext; import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text; import net.minecraft.util.Identifier;
import java.util.ArrayList; import java.util.List;

public class PowerHudConfigScreen extends Screen {
    private enum Category { DISPLAY, ELEMENTS, THEME, FPS_TWEAK, INV_TWEAK, ABOUT }
    private static Category currentCategory = Category.DISPLAY;
    private final List<TooltipArea> tooltips = new ArrayList<>();
    private final String modVer = FabricLoader.getInstance().getModContainer("powerhud").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("Unknown");
    public PowerHudConfigScreen() { super(Text.literal("PowerHUD Config")); }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}
    private record TooltipArea(int x, int y, int w, int h, String text) {}
    @Override public void close() { PowerHudConfig.save(); super.close(); }
    @Override protected void init() {
        tooltips.clear(); int mid = this.width / 2; int startY = 35, leading = 16, btnH = 14, btnW = 150, rX = mid + 5, lX = mid - 115, hW = 95, gap = 4;
        addTab(lX, startY, "Display", Category.DISPLAY); addTab(lX, startY + leading, "HUD Elements", Category.ELEMENTS); 
        addDrawableChild(ButtonWidget.builder(Text.literal("HUD Element Order"), b -> { this.client.setScreen(new HudOrderScreen(this)); }).dimensions(lX, startY + (leading * 2), 110, 16).build());
        addTab(lX, startY + (leading * 3), "Theme", Category.THEME); addTab(lX, startY + (leading * 4), "FPS Tweak", Category.FPS_TWEAK); addTab(lX, startY + (leading * 5), "Inventory Tweak", Category.INV_TWEAK); addTab(lX, startY + (leading * 6), "About", Category.ABOUT);
        switch (currentCategory) {
            case DISPLAY: addToggle(rX, startY, "HUD Master", PowerHudConfig.hudEnabled ? "ON" : "OFF", "Toggle HUD Visibility", b -> { PowerHudConfig.hudEnabled = !PowerHudConfig.hudEnabled; clearAndInit(); }, btnW, btnH); addStepper(rX, startY + leading, "HUD Scale", PowerHudConfig.hudScaleVert, "Global HUD Scale", val -> { if(val >= 50 && val <= 250) PowerHudConfig.hudScaleVert = val; }, btnW, btnH, 10); break;
            case ELEMENTS: 
                addBool(rX, startY, "FPS", PowerHudConfig.showFps, v -> PowerHudConfig.showFps = v, hW, btnH, "Toggle FPS"); addBool(rX + hW + gap, startY, "XYZ", PowerHudConfig.showCoords, v -> PowerHudConfig.showCoords = v, hW, btnH, "Toggle Co-ordinates"); 
                addBool(rX, startY + leading, "Facing", PowerHudConfig.showDirection, v -> PowerHudConfig.showDirection = v, hW, btnH, "Toggle Direction"); addBool(rX + hW + gap, startY + leading, "Biome", PowerHudConfig.showBiome, v -> PowerHudConfig.showBiome = v, hW, btnH, "Toggle Biome"); 
                addBool(rX, startY + (leading * 2), "Time", PowerHudConfig.showTime, v -> PowerHudConfig.showTime = v, hW, btnH, "Toggle Time"); addBool(rX + hW + gap, startY + (leading * 2), "Vitality", PowerHudConfig.showVitality, v -> PowerHudConfig.showVitality = v, hW, btnH, "Toggle Vitality"); 
                addBool(rX, startY + (leading * 3), "Block", PowerHudConfig.showBlock, v -> PowerHudConfig.showBlock = v, hW, btnH, "Toggle Block Info"); addBool(rX + hW + gap, startY + (leading * 3), "Best Tool", PowerHudConfig.showBestTool, v -> PowerHudConfig.showBestTool = v, hW, btnH, "Recommend tool for block"); 
                addBool(rX, startY + (leading * 4), "Inventory", PowerHudConfig.showInventory, v -> PowerHudConfig.showInventory = v, hW, btnH, "Toggle Inventory"); addBool(rX + hW + gap, startY + (leading * 4), "Oxygen", PowerHudConfig.showOxygen, v -> PowerHudConfig.showOxygen = v, hW, btnH, "Toggle Oxygen Info"); 
                addBool(rX, startY + (leading * 5), "Vanilla Air", !PowerHudConfig.hideVanillaOxygen, v -> PowerHudConfig.hideVanillaOxygen = !v, hW, btnH, "Show/Hide Default Air Bubbles"); addBool(rX + hW + gap, startY + (leading * 5), "Block Stats", PowerHudConfig.showBlockStats, v -> PowerHudConfig.showBlockStats = v, hW, btnH, "Track session blocks"); 
                addBool(rX, startY + (leading * 6), "Gamemode", PowerHudConfig.showGamemode, v -> PowerHudConfig.showGamemode = v, hW, btnH, "Show current game mode");
                break;
            case THEME: addToggle(rX, startY, "HUD Font", PowerHudConfig.FONT_NAMES[PowerHudConfig.fontIndex], "Change global HUD font", b -> { PowerHudConfig.fontIndex = (PowerHudConfig.fontIndex + 1) % PowerHudConfig.FONT_NAMES.length; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + leading, "Shadow Level", PowerHudConfig.boxStyle.toString(), "HUD background style", b -> { PowerHudConfig.boxStyle = PowerHudConfig.BoxStyle.values()[(PowerHudConfig.boxStyle.ordinal() + 1) % PowerHudConfig.BoxStyle.values().length]; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + (leading * 2), "Soft Corners", PowerHudConfig.roundCorners ? "ON" : "OFF", "Adds corners to HUD boxes", b -> { PowerHudConfig.roundCorners = !PowerHudConfig.roundCorners; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + (leading * 3), "Data Color", PowerHudConfig.COLOR_NAMES[PowerHudConfig.themeIndex], "HUD data colour", b -> { PowerHudConfig.themeIndex = (PowerHudConfig.themeIndex + 1) % PowerHudConfig.COLORS.length; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + (leading * 4), "Title Color", PowerHudConfig.COLOR_NAMES[PowerHudConfig.titleColorIndex], "HUD title colour", b -> { PowerHudConfig.titleColorIndex = (PowerHudConfig.titleColorIndex + 1) % PowerHudConfig.COLORS.length; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + (leading * 5), "Bold Titles", PowerHudConfig.boldTitles ? "ON" : "OFF", "HUD title bold?", b -> { PowerHudConfig.boldTitles = !PowerHudConfig.boldTitles; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + (leading * 6), "Visual FX", PowerHudConfig.textEffect.toString(), "Applies HUD text effects", b -> { PowerHudConfig.textEffect = PowerHudConfig.TextEffect.values()[(PowerHudConfig.textEffect.ordinal() + 1) % PowerHudConfig.TextEffect.values().length]; clearAndInit(); }, btnW, btnH); break;
            case FPS_TWEAK: addToggle(rX, startY, "Mode", PowerHudConfig.fpsMode.toString(), "FPS Complexity", b -> { PowerHudConfig.fpsMode = PowerHudConfig.FpsMode.values()[(PowerHudConfig.fpsMode.ordinal() + 1) % 3]; clearAndInit(); }, btnW, btnH); addToggle(rX, startY + leading, "FPS Dot", PowerHudConfig.showFpsDot ? "ON" : "OFF", "Show Status Indicator", b -> { PowerHudConfig.showFpsDot = !PowerHudConfig.showFpsDot; clearAndInit(); }, btnW, btnH); addStepper(rX, startY + (leading * 2), "Red <", PowerHudConfig.redThresh, "Low FPS Threshold", val -> PowerHudConfig.setRed(val), btnW, btnH, 5); addStepper(rX, startY + (leading * 3), "Orange <", PowerHudConfig.orangeThresh, "Medium FPS Threshold", val -> PowerHudConfig.setOrange(val), btnW, btnH, 5); addStepper(rX, startY + (leading * 4), "Yellow <", PowerHudConfig.yellowThresh, "High FPS Threshold", val -> PowerHudConfig.setYellow(val), btnW, btnH, 5); break;
            case INV_TWEAK: addToggle(rX, startY, "Inv Style", PowerHudConfig.inventoryMode.toString(), "Percent, value or boxes", b -> { PowerHudConfig.inventoryMode = PowerHudConfig.InventoryMode.values()[(PowerHudConfig.inventoryMode.ordinal() + 1) % 3]; clearAndInit(); }, btnW, btnH); break;
            case ABOUT: break;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(mid - 50, height - 65, 100, 14).build());
    }
    @Override public void render(DrawContext dc, int mx, int my, float t) {
        super.render(dc, mx, my, t); dc.drawCenteredTextWithShadow(this.textRenderer, "PowerHUD Settings", this.width / 2, 15, 0xFFFFFFFF);
        if (currentCategory == Category.ABOUT) {
            int rX = this.width / 2 + 5, startY = 35, p = 8, boxW = 190, boxH = 160; dc.fill(rX, startY, rX + boxW, startY + boxH, 0xCC000000); 
            dc.drawTextWithShadow(this.textRenderer, "PowerHUD", rX + p, startY + p, 0xFFFFD700); dc.drawTextWithShadow(this.textRenderer, modVer, rX + p, startY + p + 11, 0xFFAAAAAA);
            String[] lines = {"PowerHud is customizable HUD", "providing extra info if required.", "Please support the developer scan", "QR code below to donate towards", "a coffee."};
            int ty = startY + p + 26; for (String line : lines) { int lineW = this.textRenderer.getWidth(line); dc.drawTextWithShadow(this.textRenderer, line, rX + (boxW / 2) - (lineW / 2), ty, 0xFFBBBBBB); ty += 11; }
            ty += 6; String sStr = "Support - watkins.steve@gmail.com"; int sW = this.textRenderer.getWidth(sStr); dc.drawTextWithShadow(this.textRenderer, sStr, rX + (boxW / 2) - (sW / 2), ty, 0xFF777777);
            Identifier qr = Identifier.of("powerhud", "textures/coffee_qr.png"); dc.drawTexture(RenderLayer::getGuiTextured, qr, rX + (boxW / 2) - 22, startY + boxH - 52, 0.0f, 0.0f, 44, 44, 44, 44);
        }
        for (TooltipArea tip : tooltips) { if (mx >= tip.x && mx <= tip.x + tip.w && my >= tip.y && my <= tip.y + tip.h) { dc.drawTooltip(this.textRenderer, Text.literal(tip.text), mx - 160, my - 10); } }
    }
    @Override protected void clearAndInit() { clearChildren(); init(); }
    private ButtonWidget addStepper(int x, int y, String l, int cur, String t, java.util.function.Consumer<Integer> a, int w, int h, int s) {
        int sw = 12, mw = w - (sw * 2) - 4; addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> { a.accept(cur - s); clearAndInit(); }).dimensions(x, y, sw, h).build());
        ButtonWidget mid = addDrawableChild(ButtonWidget.builder(Text.literal(l + ": " + cur), b -> {}).dimensions(x + sw + 2, y, mw, h).build()); mid.active = false;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> { a.accept(cur + s); clearAndInit(); }).dimensions(x + sw + mw + 4, y, sw, h).build()); return mid;
    }
    private ButtonWidget addToggle(int x, int y, String l, String v, String tip, ButtonWidget.PressAction a, int w, int h) { ButtonWidget b = ButtonWidget.builder(Text.literal(l + (v.isEmpty() ? "" : ": " + v)), a).dimensions(x, y, w, h).build(); addDrawableChild(b); tooltips.add(new TooltipArea(x, y, w, h, tip)); return b; }
    private void addTab(int x, int y, String label, Category cat) { ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> { currentCategory = cat; clearAndInit(); }).dimensions(x, y, 110, 16).build(); if (currentCategory == cat) btn.active = false; addDrawableChild(btn); }
    private void addBool(int x, int y, String l, boolean v, java.util.function.Consumer<Boolean> a, int w, int h, String tip) { addToggle(x, y, l, v ? "ON" : "OFF", tip, b -> { a.accept(!v); clearAndInit(); }, w, h); }
}
