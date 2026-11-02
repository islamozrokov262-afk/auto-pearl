package com.islamozroko.autopearl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
                double dx = nearestEnemy.getX() - client.player.getX();
                double dz = nearestEnemy.getZ() - client.player.getZ();
                float yawToEnemy = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                
                targetYaw = yawToEnemy + 180.0f; // Разворот на 180
                
                isSwapping = true;
                state = 0;
            }
        });
    }

    private void handleSwapProcess(Minecraft client) {
        if (client.player == null || client.getConnection() == null) return;

        // Шаг 0: Отправляем серверу пакет, что мы развернулись (камера на клиенте не меняется!)
        if (state == 0) {
            // Отправляем фейковый пакет поворота
            client.getConnection().send(new ServerboundMovePlayerPacket.Rot(targetYaw, -5.0f, client.player.onGround()));
            
            // Свапаем слот
            client.player.getInventory().setSelectedSlot(config.pearlSlot);
            
            holdTicks = 1;
            state = 1;
        } 
        // Шаг 1: Кидаем перл
        else if (state == 1) {
            if (holdTicks > 0) {
                holdTicks--;
                return;
            }
            
            // Отправляем пакет использования предмета (ПКМ)
            client.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, targetYaw, -5.0f));
            
            // Возвращаем камеру серверу обратно
            client.getConnection().send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
            
            isSwapping = false;
            cooldown = 60;
        }
    }

    public static AutoPearlConfig getConfig() {
        return config;
    }
}
