package net.steve.powerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class PowerHUD implements ClientModInitializer {
    public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("powerhud", "hud");

    @Override
    public void onInitializeClient() {
        PowerHudConfig.load();
        HudRenderer.initKeys();
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.MISC_OVERLAYS,
            HUD_ID,
            HudRenderer::render
        );
        HudElementRegistry.replaceElement(
            VanillaHudElements.AIR_BAR,
            original -> (graphics, delta) -> {
                if (!PowerHudConfig.hideVanillaOxygen) {
                    original.extractRenderState(graphics, delta);
                }
            }
        );
    }
}
