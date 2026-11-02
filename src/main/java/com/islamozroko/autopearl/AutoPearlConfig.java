package com.islamozroko.autopearl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AutoPearlConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("autopearl.json").toFile();

    public boolean enabled = true;
    public float healthThreshold = 8.0f;
    public int enemyCount = 2;
    public double enemyRadius = 4.0;
    public int pearlSlot = 1;
    public int auraKeyCode = 82; // R

    public static AutoPearlConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                AutoPearlConfig config = GSON.fromJson(reader, AutoPearlConfig.class);
                if (config != null) return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        AutoPearlConfig config = new AutoPearlConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
