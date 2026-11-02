package com.islamozroko.autopearl;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoPearlConfigScreen {
    public static Screen create(Screen parent) {
        AutoPearlConfig config = AutoPearlMod.getConfig();
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("AutoPearl Настройки"))
                .setSavingRunnable(config::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("Основное"));
        
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Включить"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());
                
        general.addEntry(entryBuilder.startFloatField(Component.literal("Порог ХП"), config.healthThreshold)
                .setDefaultValue(8.0f)
                .setSaveConsumer(newValue -> config.healthThreshold = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.literal("Кол-во врагов"), config.enemyCount)
                .setDefaultValue(2)
                .setSaveConsumer(newValue -> config.enemyCount = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.literal("Радиус врагов"), config.enemyRadius)
                .setDefaultValue(4.0)
                .setSaveConsumer(newValue -> config.enemyRadius = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.literal("Слот перла (0-8)"), config.pearlSlot)
                .setDefaultValue(1)
                .setSaveConsumer(newValue -> config.pearlSlot = newValue)
                .build());
                
        general.addEntry(entryBuilder.startIntField(Component.literal("Код кнопки Aura (R=82)"), config.auraKeyCode)
                .setDefaultValue(82)
                .setSaveConsumer(newValue -> config.auraKeyCode = newValue)
                .build());
        
        return builder.build();
    }
}
