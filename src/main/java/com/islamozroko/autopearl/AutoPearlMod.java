package com.islamozroko.autopearl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;

public class AutoPearlMod implements ClientModInitializer {
    private static AutoPearlConfig config;
    private static int cooldown = 0;
    private static boolean isSwapping = false;
    private static int state = 0;
    private static int holdTicks = 0;
    private static int postPearlTicks = 0;
    private static float originalYaw = 0;
    private static float originalPitch = 0;
    private static float targetYaw = 0;

    @Override
    public void onInitializeClient() {
        config = AutoPearlConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || !config.enabled) return;
            
            if (isSwapping) {
                handleSwapProcess(client);
                return;
            }

            if (cooldown > 0) {
                cooldown--;
                return;
            }

            if (client.player.getHealth() > config.healthThreshold) return;

            int enemies = 0;
            Player nearestEnemy = null;
            double closestDist = 999;
            
            for (Player enemy : client.level.players()) {
                if (enemy != client.player && enemy.isAlive()) {
                    double dist = enemy.distanceTo(client.player);
                    if (dist <= config.enemyRadius) {
                        enemies++;
                        if (dist < closestDist) {
                            closestDist = dist;
                            nearestEnemy = enemy;
                        }
                    }
                }
            }
            
            if (enemies < config.enemyCount || nearestEnemy == null) return;

            ItemStack stack = client.player.getInventory().getItem(config.pearlSlot);
            if (stack.getItem() instanceof EnderpearlItem) {
                originalYaw = client.player.getYRot();
                originalPitch = client.player.getXRot();
                
                double dx = nearestEnemy.getX() - client.player.getX();
                double dz = nearestEnemy.getZ() - client.player.getZ();
                float yawToEnemy = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                
                targetYaw = yawToEnemy + 180.0f;
                
                isSwapping = true;
                state = 0;
            }
        });
    }

    private void handleSwapProcess(Minecraft client) {
        if (client.player == null) return;

        // Шаг 0: Выключаем килку и разворачиваемся
        if (state == 0) {
            setKeyPressed(config.auraKeyCode, true);
            setKeyPressed(config.auraKeyCode, false);
            
            client.player.setYRot(targetYaw);
            client.player.setXRot(-5.0f); // Угол 5 градусов (почти прямо, летит недалеко)
            
            holdTicks = 2;
            state = 1;
        } 
        // Шаг 1: Отправляем пакет перла
        else if (state == 1) {
            if (holdTicks > 0) {
                holdTicks--;
                client.player.setYRot(targetYaw);
                client.player.setXRot(-5.0f);
                return;
            }
            
            client.player.getInventory().setSelectedSlot(config.pearlSlot);
            
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, targetYaw, -5.0f));
            }
            
            postPearlTicks = 10;
            state = 2;
        }
        // Шаг 2: Возвращаем камеру и включаем килку
        else if (state == 2) {
            if (postPearlTicks > 0) {
                postPearlTicks--;
                return;
            }
            
            client.player.setYRot(originalYaw);
            client.player.setXRot(originalPitch);
            
            setKeyPressed(config.auraKeyCode, true);
            setKeyPressed(config.auraKeyCode, false);
            
            isSwapping = false;
            cooldown = 60;
        }
    }

    private void setKeyPressed(int keyCode, boolean pressed) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(keyCode), pressed);
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    public static AutoPearlConfig getConfig() {
        return config;
    }
}
