package net.steve.powerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.BlockTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HudData {
    public static String fpsStr="", coordsStr="", dirStr="", biomeStr="", timeStr="", timeLabel="Time", vitStr="", blockStr="", invStr="", oxyStr="", oxyBarStr="", oxyOverlayStr="", oxyStatusStr="", targetType="Block", lightStr="", dayStr="", blockProps="", blockStatsStr = "", gamemodeStr = "";
    public static String cpuName = "Unknown CPU", gpuName = "Unknown GPU", displayInfo = "Unknown Display", entityCount = "-", particleCount = "-", chunkStats = "-", soundStats = "-", moveFlags = "-", effectList = "None";
    public static int fpsColor=0xFFFFFFFF, vitColor=0xFFFFFFFF, invColor=0xFFFFFFFF, invCount=0, oxyColor=0xFFFFFFFF, currentFps = 0, minFps = -1, maxFps = -1;
    public static float oxyPercent = 1.0f, avgFps = 0;
    public static boolean[] invSlots = new boolean[36]; // 27 inventory + 9 hotbar
    public static ItemStack toolStack = ItemStack.EMPTY;
    public static String toolStr = "";
    public static List<Integer> fpsGraph = new ArrayList<>();
    public static boolean sysInfoLoaded = false;
    public static int sessionMined = 0, sessionPlaced = 0;
    private static long frames = 0, totalFps = 0, lastSlowUpdate = 0;

    public static void resetFps() { minFps = -1; maxFps = -1; frames = 0; totalFps = 0; avgFps = 0; fpsGraph.clear(); }

    public static void update(Minecraft client) {
        if (client.player == null) return;
        if (!sysInfoLoaded) {
            try {
                cpuName = net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs.getCpuInfo();
            } catch (Throwable t) {
                cpuName = System.getProperty("os.arch", "Unknown CPU");
            }
            try {
                var dev = com.mojang.blaze3d.systems.RenderSystem.tryGetDevice();
                if (dev != null && dev.getDeviceInfo() != null) {
                    gpuName = dev.getDeviceInfo().name();
                } else {
                    gpuName = "Unknown GPU";
                }
            } catch (Throwable t) {
                gpuName = "Unknown GPU";
            }
            if (client.getWindow() != null) {
                displayInfo = client.getWindow().getWidth() + "x" + client.getWindow().getHeight();
            }
            sysInfoLoaded = true;
        }

        currentFps = client.getFps();
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

        // Inventory slots: 27 (3 rows of 9), hotbar: 9 (bottom row)
        invCount = 0;
        var items = client.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < 27; i++) {
            boolean has = (i + 9 < items.size()) && !items.get(i + 9).isEmpty();
            invSlots[i] = has;
            if (has) invCount++;
        }
        for (int i = 0; i < 9; i++) {
            boolean has = (i < items.size()) && !items.get(i).isEmpty();
            invSlots[27 + i] = has;
        }
        invStr = switch(PowerHudConfig.inventoryMode) {
            case PERCENT -> (int)((invCount / 27.0) * 100) + "%";
            case FRACTION -> invCount + "/27";
            default -> invCount + " Slots";
        };
        invColor = (invCount > 22) ? 0xFFFF5555 : (invCount > 15) ? 0xFFFFFF55 : 0xFF55FF55;

        BlockPos pos = client.player.blockPosition();
        coordsStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        Direction d = client.player.getDirection();
        dirStr = d.getName().substring(0, 1).toUpperCase() + d.getName().substring(1);

        long now = System.currentTimeMillis();
        if (now - lastSlowUpdate > 200) {
            updateSlow(client, pos, now);
            lastSlowUpdate = now;
        }
        updateVitality(client);
    }

    private static void updateVitality(Minecraft client) {
        float hp = client.player.getHealth(), max = client.player.getMaxHealth();
        int food = client.player.getFoodData().getFoodLevel();
        if (food == 0) vitStr = "Starving";
        else if (food <= 6) vitStr = "Drained";
        else if (food <= 17) vitStr = "Hungry";
        else vitStr = "Well Fed";
        vitColor = (hp < max * 0.3) ? 0xFFFF5555 : (hp < max * 0.6) ? 0xFFFFFF55 : 0xFF55FF55;
    }

    private static void updateSlow(Minecraft client, BlockPos pos, long now) {
        fpsGraph.add(currentFps);
        if (fpsGraph.size() > 260) fpsGraph.remove(0);

        if (client.level != null) {
            biomeStr = client.level.getBiome(pos).unwrapKey().map(k -> k.identifier().getPath().replace("_", " ")).orElse("Unknown");
        }

        if (client.getConnection() != null) {
            PlayerInfo e = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (e != null && e.getGameMode() != null) {
                String gm = e.getGameMode().getName();
                gamemodeStr = gm.substring(0, 1).toUpperCase() + gm.substring(1);
            }
        }

        if (client.level != null) {
            long worldTime = client.level.getDefaultClockTime() % 24000;
            long diff = ((worldTime < 12000) ? 12000 : 23000) - worldTime;
            if (diff < 0) diff += 24000;
            timeStr = String.format("%02d:%02d", (diff / 1200), (diff % 1200) / 20);
            timeLabel = (worldTime < 12000) ? "Sunset" : "Sunrise";
            blockStatsStr = "Mined: " + sessionMined + " Placed: " + sessionPlaced;

            if (client.levelExtractor != null) {
                entityCount = client.levelExtractor.entityStatistics();
                chunkStats = client.levelExtractor.sectionStatistics();
            }
            if (client.particleEngine != null) {
                particleCount = client.particleEngine.countParticles();
            }
            if (client.getSoundManager() != null) {
                try {
                    var counter = new net.minecraft.client.sounds.SoundBufferLibrary.DebugOutput.Counter();
                    client.getSoundManager().getSoundCacheDebugStats(counter);
                    soundStats = counter.totalCount() + " sounds";
                } catch (Throwable t) {
                    soundStats = "-";
                }
            }
        }

        StringBuilder flags = new StringBuilder();
        if (client.player.onGround()) flags.append("[Ground] ");
        if (client.player.isSprinting()) flags.append("[Sprint] ");
        if (client.player.isCrouching()) flags.append("[Sneak] ");
        if (client.player.isSwimming()) flags.append("[Swim] ");
        moveFlags = flags.toString().trim();
        if (moveFlags.isEmpty()) moveFlags = "-";

        Collection<MobEffectInstance> effects = client.player.getActiveEffects();
        if (effects.isEmpty()) {
            effectList = "None";
        } else {
            StringBuilder sb = new StringBuilder();
            int c = 0;
            for (MobEffectInstance effect : effects) {
                if (c++ > 0) sb.append(", ");
                var key = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                String name = key != null ? key.getPath() : "effect";
                sb.append(name).append(" (").append(effect.getAmplifier() + 1).append(")");
            }
            effectList = sb.toString();
        }

        boolean isSubmerged = client.player.isUnderWater();
        HitResult entityHit = client.hitResult;
        toolStr = "";
        toolStack = ItemStack.EMPTY;
        if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
            targetType = "Entity";
            blockStr = ((EntityHitResult)entityHit).getEntity().getName().getString();
            toolStr = "Sword";
            toolStack = Items.IRON_SWORD.getDefaultInstance();
        } else if (client.getCameraEntity() != null && client.level != null) {
            HitResult bPass = client.getCameraEntity().pick(4.5, 0.0f, false);
            if (bPass.getType() == HitResult.Type.BLOCK) {
                processBlock(client.level.getBlockState(((BlockHitResult)bPass).getBlockPos()));
            } else {
                HitResult lPass = client.getCameraEntity().pick(4.5, 0.0f, true);
                if (lPass.getType() == HitResult.Type.BLOCK) {
                    BlockPos bp = ((BlockHitResult)lPass).getBlockPos();
                    FluidState fl = client.level.getFluidState(bp);
                    if (!fl.isEmpty()) {
                        targetType = "Liquid";
                        blockStr = fl.getType().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                        toolStr = "Bucket";
                        toolStack = Items.BUCKET.getDefaultInstance();
                    } else {
                        processBlock(client.level.getBlockState(bp));
                    }
                } else {
                    targetType = "Block";
                    blockStr = "Air";
                }
            }
        }

        int air = client.player.getAirSupply();
        int maxAir = client.player.getMaxAirSupply();
        oxyPercent = maxAir > 0 ? (float)air / (float)maxAir : 1.0f;
        if (isSubmerged || air < maxAir) {
            String status;
            if (air >= maxAir * 0.8) {
                oxyColor = 0x6655FF55;
                status = "Safe";
            } else if (air >= maxAir * 0.5) {
                oxyColor = 0x66FFFF55;
                status = "Caution";
            } else if (air >= maxAir * 0.25) {
                oxyColor = 0x66FF8800;
                status = "Low";
            } else {
                oxyColor = 0x66FF5555;
                status = "CRITICAL";
            }

            int percent = Math.round(oxyPercent * 100.0f);
            oxyBarStr = "";
            oxyOverlayStr = percent + "%";
            oxyStatusStr = status;
            oxyStr = status;
        } else {
            oxyBarStr = "";
            oxyOverlayStr = "";
            oxyStatusStr = "";
            oxyStr = "";
        }
    }

    private static void processBlock(BlockState state) {
        targetType = "Block";
        blockStr = state.getBlock().getName().getString();
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            toolStr = "Pickaxe";
            toolStack = Items.IRON_PICKAXE.getDefaultInstance();
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            toolStr = "Axe";
            toolStack = Items.IRON_AXE.getDefaultInstance();
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            toolStr = "Shovel";
            toolStack = Items.IRON_SHOVEL.getDefaultInstance();
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            toolStr = "Hoe";
            toolStack = Items.IRON_HOE.getDefaultInstance();
        }
    }
}
