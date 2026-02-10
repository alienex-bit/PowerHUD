package net.steve.powerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class PowerHUD implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PowerHudConfig.load();
        HudRenderer.initKeys();
        HudRenderCallback.EVENT.register(new HudRenderer());
    }
}
