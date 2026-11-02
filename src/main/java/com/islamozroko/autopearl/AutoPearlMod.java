package com.islamozroko.autopearl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
                
                // Вычисляем угол до врага
                double dx = nearestEnemy.getX() - client.player.getX();
                double dz = nearestEnemy.getZ() - client.player.getZ();
                float yawToEnemy = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                
                // Разворачиваем на 180 градусов и поднимаем камеру на 30 градусов
                client.player.setYRot(yawToEnemy + 180.0f);
                client.player.setXRot(-30.0f); 
                
                isSwapping = true;
                state = 0;
            }
        });
    }

    private void handleSwapProcess(Minecraft client) {
        if (client.player == null) return;

        // Шаг 0: Нажимаем R (выключаем килку)
        if (state == 0) {
            setKeyPressed(config.auraKeyCode, true);
            holdTicks = 2;
            state = 1;
        } 
        // Шаг 1: Отпускаем R, свапаем слот, кидаем перл
        else if (state == 1) {
            if (holdTicks > 0) {
                holdTicks--;
                return;
            }
            setKeyPressed(config.auraKeyCode, false);
            
            client.player.getInventory().setSelectedSlot(config.pearlSlot);
            client.options.keyUse.setDown(true);
            state = 2;
        }
        // Шаг 2: Отпускаем ПКМ, возвращаем камеру
        else if (state == 2) {
            client.options.keyUse.setDown(false);
            client.player.setYRot(originalYaw);
            client.player.setXRot(originalPitch);
            
            // Ждем 10 тиков (0.5 секунды), чтобы перл долетел
            postPearlTicks = 10; 
            state = 3;
        }
        // Шаг 3: Ждем, пока перл долетит, и только потом включаем килку
        else if (state == 3) {
            if (postPearlTicks > 0) {
                postPearlTicks--;
                return;
            }
            
            setKeyPressed(config.auraKeyCode, true);
            holdTicks = 2;
            state = 4;
        }
        // Шаг 4: Отпускаем R и завершаем
        else if (state == 4) {
            if (holdTicks > 0) {
                holdTicks--;
                return;
            }
            setKeyPressed(config.auraKeyCode, false);
            isSwapping = false;
            cooldown = 60; // Кулдаун 3 секунды
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
