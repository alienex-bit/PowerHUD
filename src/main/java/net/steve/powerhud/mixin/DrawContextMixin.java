package net.steve.powerhud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.steve.powerhud.PowerHudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Function;

@Mixin(DrawContext.class)
public class DrawContextMixin {
    
    // OPTIMIZATION: Pre-define the identifier constant to avoid string comparisons
    private static final Identifier AIR_TEXTURE = Identifier.ofVanilla("hud/air");
    private static final Identifier AIR_BURSTING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");

    @Inject(method = "drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V", at = @At("HEAD"), cancellable = true)
    private void onDrawGuiTexture(Function<Identifier, RenderLayer> renderLayers, Identifier texture, int x, int y, int width, int height, CallbackInfo ci) {
        // Optimization: Quick check first
        if (!PowerHudConfig.hideVanillaOxygen) return;

        // Optimization: Identity check is faster than string check
        if (texture.equals(AIR_TEXTURE) || texture.equals(AIR_BURSTING_TEXTURE)) {
            ci.cancel();
        }
    }
}
