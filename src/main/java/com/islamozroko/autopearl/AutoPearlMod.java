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
            for (Player enemy : client.level.players()) {
                if (enemy != client.player && enemy.isAlive() && enemy.distanceTo(client.player) <= config.enemyRadius) {
                    enemies++;
                }
            }
            if (enemies < config.enemyCount) return;

            ItemStack stack = client.player.getInventory().getItem(config.pearlSlot);
            if (stack.getItem() instanceof EnderpearlItem) {
                isSwapping = true;
                state = 0;
            }
        });
    }

    private void handleSwapProcess(Minecraft client) {
        if (client.player == null) return;

        if (state == 0) {
            setKeyPressed(config.auraKeyCode, true);
            holdTicks = 2;
            state = 1;
        } 
        else if (state == 1) {
            if (holdTicks > 0) {
                holdTicks--;
                return;
            }
            setKeyPressed(config.auraKeyCode, false);
            
            client.player.getInventory().setSelectedSlot(config.pearlSlot);
            client.player.setXRot(90.0f); // Смотрим вниз
            state = 2;
        }
        else if (state == 2) {
            client.options.keyUse.setDown(true);
            state = 3;
        }
        else if (state == 3) {
            client.options.keyUse.setDown(false);
            
            setKeyPressed(config.auraKeyCode, true);
            holdTicks = 2;
            state = 4;
        }
        else if (state == 4) {
            if (holdTicks > 0) {
                holdTicks--;
                return;
            }
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
