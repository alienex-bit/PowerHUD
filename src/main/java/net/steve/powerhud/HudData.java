package net.steve.powerhud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.fluid.FluidState;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.mojang.blaze3d.platform.GlDebugInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class HudData {
    public static String fpsStr="", coordsStr="", dirStr="", biomeStr="", timeStr="", timeLabel="Time", vitStr="", blockStr="", invStr="", oxyStr="", targetType="Block", lightStr="", dayStr="", blockProps="", blockStatsStr = "", gamemodeStr = "";
    public static String cpuName = "Unknown CPU", gpuName = "Unknown GPU", displayInfo = "Unknown Display", entityCount = "-", particleCount = "-", chunkStats = "-", soundStats = "-", moveFlags = "-", effectList = "None";
    public static int fpsColor=0xFFFFFFFF, vitColor=0xFFFFFFFF, invColor=0xFFFFFFFF, invCount=0, oxyColor=0xFFFFFFFF, currentFps = 0, minFps = -1, maxFps = -1;
    public static float oxyPercent = 1.0f, avgFps = 0;
    public static boolean[] invSlots = new boolean[27];
    public static ItemStack toolStack = ItemStack.EMPTY;
    public static String toolStr = "";
    public static List<Integer> fpsGraph = new ArrayList<>();
    public static boolean sysInfoLoaded = false;
    public static int sessionMined = 0, sessionPlaced = 0;
    private static long frames = 0, totalFps = 0, lastSlowUpdate = 0;

    public static void resetFps() { minFps = -1; maxFps = -1; frames = 0; totalFps = 0; avgFps = 0; fpsGraph.clear(); }
    public static void update(MinecraftClient client) {
        if (client.player == null) return;
        if (!sysInfoLoaded) { cpuName = GlDebugInfo.getCpuInfo(); gpuName = GlDebugInfo.getRenderer(); if (client.getWindow() != null) { displayInfo = client.getWindow().getWidth() + "x" + client.getWindow().getHeight(); } sysInfoLoaded = true; }
        currentFps = client.getCurrentFps();
        if (minFps == -1 || currentFps < minFps) minFps = currentFps;
        if (maxFps == -1 || currentFps > maxFps) maxFps = currentFps;
        frames++; totalFps += currentFps; avgFps = (float)totalFps / frames;
        float frameTimeMs = currentFps > 0 ? 1000.0f / currentFps : 0;
        fpsStr = switch(PowerHudConfig.fpsMode) {
            case MINIMAL -> "FPS:" + currentFps;
            case NORMAL -> "FPS:" + currentFps + " AVG:" + (int)avgFps + " MIN:" + minFps + " MAX:" + maxFps;
            case FULL -> "FPS:" + currentFps + " AVG:" + (int)avgFps + " MIN:" + minFps + " MAX:" + maxFps + " (" + String.format("%.1f", frameTimeMs) + "ms)";
        };
        fpsColor = (currentFps < PowerHudConfig.redThresh ? 0xFFFF5555 : (currentFps < PowerHudConfig.orangeThresh ? 0xFFFFAA00 : (currentFps < PowerHudConfig.yellowThresh ? 0xFFFFFF55 : 0xFF55FF55)));
        invCount = 0; for(int i=0; i<27; i++) { boolean has = !client.player.getInventory().main.get(i+9).isEmpty(); invSlots[i]=has; if(has) invCount++; }
        invStr = switch(PowerHudConfig.inventoryMode) { case PERCENT -> (int)((invCount / 27.0) * 100) + "%"; case FRACTION -> invCount + "/27"; default -> invCount + " Slots"; };
        invColor = (invCount > 22) ? 0xFFFF5555 : (invCount > 15) ? 0xFFFFFF55 : 0xFF55FF55;
        BlockPos pos = client.player.getBlockPos(); coordsStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        Direction d = client.player.getHorizontalFacing(); dirStr = d.getName().substring(0, 1).toUpperCase() + d.getName().substring(1);
        long now = System.currentTimeMillis(); if (now - lastSlowUpdate > 200) { updateSlow(client, pos, now); lastSlowUpdate = now; }
        updateVitality(client);
    }
    private static void updateVitality(MinecraftClient client) {
        float hp = client.player.getHealth(), max = client.player.getMaxHealth(); int food = client.player.getHungerManager().getFoodLevel();
        if (food == 0) vitStr = "Starving"; else if (food <= 6) vitStr = "Drained"; else if (food <= 17) vitStr = "Hungry"; else vitStr = "Well Fed";
        vitColor = (hp < max * 0.3) ? 0xFFFF5555 : (hp < max * 0.6) ? 0xFFFFFF55 : 0xFF55FF55;
    }
    private static void updateSlow(MinecraftClient client, BlockPos pos, long now) {
        fpsGraph.add(currentFps); if (fpsGraph.size() > 260) fpsGraph.remove(0);
        biomeStr = client.world.getBiome(pos).getKey().map(k -> k.getValue().getPath().replace("_", " ")).orElse("Unknown");
        if (client.getNetworkHandler() != null) { PlayerListEntry e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()); if (e != null && e.getGameMode() != null) { String gm = e.getGameMode().getName(); gamemodeStr = gm.substring(0, 1).toUpperCase() + gm.substring(1); } }
        if (client.world != null) {
            long worldTime = client.world.getTimeOfDay() % 24000; long diff = ((worldTime < 12000) ? 12000 : 23000) - worldTime; if (diff < 0) diff += 24000;
            timeStr = String.format("%02d:%02d", (diff / 1200), (diff % 1200) / 20); timeLabel = (worldTime < 12000) ? "Sunset" : "Sunrise";
            blockStatsStr = "Mined: " + sessionMined + " Placed: " + sessionPlaced;
            int entC = 0; for (Entity e : client.world.getEntities()) entC++; entityCount = String.valueOf(entC);
            particleCount = client.particleManager.getDebugString(); chunkStats = client.worldRenderer.getChunksDebugString();
            if (client.getSoundManager() != null) soundStats = client.getSoundManager().getDebugString().trim();
        }
        StringBuilder flags = new StringBuilder(); if (client.player.isOnGround()) flags.append("[Ground] "); if (client.player.isSprinting()) flags.append("[Sprint] "); if (client.player.isSneaking()) flags.append("[Sneak] "); if (client.player.isSwimming()) flags.append("[Swim] "); moveFlags = flags.toString().trim(); if (moveFlags.isEmpty()) moveFlags = "-";
        Collection<StatusEffectInstance> effects = client.player.getStatusEffects(); if (effects.isEmpty()) { effectList = "None"; } else { StringBuilder sb = new StringBuilder(); int c = 0; for (StatusEffectInstance effect : effects) { if (c++ > 0) sb.append(", "); sb.append(Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).getPath()).append(" (").append(effect.getAmplifier() + 1).append(")"); } effectList = sb.toString(); }

        boolean isSubmerged = client.player.isSubmergedInWater();
        HitResult entityHit = client.crosshairTarget; toolStr = ""; toolStack = ItemStack.EMPTY;
        if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) { targetType = "Entity"; blockStr = ((EntityHitResult)entityHit).getEntity().getName().getString(); toolStr = "Sword"; toolStack = Items.IRON_SWORD.getDefaultStack(); }
        else {
            HitResult bPass = client.cameraEntity.raycast(4.5, 0.0f, false);
            if (bPass.getType() == HitResult.Type.BLOCK) { processBlock(client.world.getBlockState(((BlockHitResult)bPass).getBlockPos())); }
            else {
                HitResult lPass = client.cameraEntity.raycast(4.5, 0.0f, true);
                if (lPass.getType() == HitResult.Type.BLOCK) {
                    BlockPos bp = ((BlockHitResult)lPass).getBlockPos(); FluidState fl = client.world.getFluidState(bp);
                    if (!fl.isEmpty()) { targetType = "Liquid"; blockStr = fl.getFluid().getDefaultState().getBlockState().getBlock().getName().getString(); toolStr = "Bucket"; toolStack = Items.BUCKET.getDefaultStack(); }
                    else processBlock(client.world.getBlockState(bp));
                } else { targetType = "Block"; blockStr = "Air"; }
            }
        }
        int air = client.player.getAir(); int maxAir = client.player.getMaxAir(); oxyPercent = (float)air / (float)maxAir;
        if (isSubmerged || air < maxAir) { if (air >= maxAir * 0.8) { oxyStr = "Holding Breath..."; oxyColor = 0xFF55FF55; } else if (air >= maxAir * 0.5) { oxyStr = "Air Depleting..."; oxyColor = 0xFFFFFF55; } else if (air >= maxAir * 0.25) { oxyStr = "Supply Low"; oxyColor = 0xFFFF5555; } else { oxyStr = "RISK OF DROWNING"; oxyColor = 0xFFFF5555; } } else { oxyStr = ""; }
    }
    private static void processBlock(BlockState state) {
        targetType = "Block"; blockStr = state.getBlock().getName().getString();
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) { toolStr = "Pickaxe"; toolStack = Items.IRON_PICKAXE.getDefaultStack(); }
        else if (state.isIn(BlockTags.AXE_MINEABLE)) { toolStr = "Axe"; toolStack = Items.IRON_AXE.getDefaultStack(); }
        else if (state.isIn(BlockTags.SHOVEL_MINEABLE)) { toolStr = "Shovel"; toolStack = Items.IRON_SHOVEL.getDefaultStack(); }
    }
}
