/**
 * The MIT License
 * Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.faceland.strife.storage;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.data.Champion;
import info.faceland.strife.data.ChampionSaveData;
import info.faceland.strife.stats.StrifeStat;
import io.pixeloutlaw.minecraft.spigot.config.SmartYamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class JsonDataStorage implements DataStorage {

    private final StrifePlugin plugin;
    private SmartYamlConfiguration configuration;
    private long lastLoaded;

    public JsonDataStorage(StrifePlugin plugin) {
        this.plugin = plugin;
        this.configuration = new SmartYamlConfiguration(new File(plugin.getDataFolder(), "data.json"));
        this.lastLoaded = System.currentTimeMillis();
    }

    @Override
    public void init() {
        // do nothing
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    @Override
    public void save(ChampionSaveData champion) {
        for (Map.Entry<StrifeStat, Integer> entry : champion.getLevelMap().entrySet()) {
            configuration.set(
                champion.getUniqueId().toString() + ".stats." + entry.getKey().getKey(),
                entry.getValue()
            );
        }
        configuration.set(
            champion.getUniqueId().toString() + ".unused-stat-points",
            champion.getUnusedStatPoints()
        );
        configuration.set(
            champion.getUniqueId().toString() + ".highest-reached-level",
            champion.getHighestReachedLevel()
        );
        configuration.set(
            champion.getUniqueId().toString() + ".bonus-levels",
            champion.getBonusLevels()
        );
        configuration.save();
    }

    @Override
    public void save(Collection<ChampionSaveData> champions) {
        for (ChampionSaveData champ : champions) {
            for (Map.Entry<StrifeStat, Integer> entry : champ.getLevelMap().entrySet()) {
                configuration.set(
                    champ.getUniqueId().toString() + ".stats." + entry.getKey().getKey(),
                    entry.getValue()
                );
            }
            configuration.set(
                champ.getUniqueId().toString() + ".unused-stat-points",
                champ.getUnusedStatPoints()
            );
            configuration.set(
                champ.getUniqueId().toString() + ".highest-reached-level",
                champ.getHighestReachedLevel()
            );
            configuration.set(
                champ.getUniqueId().toString() + ".bonus-levels",
                champ.getBonusLevels()
            );
        }
        configuration.save();
    }

    @Override
    public Collection<ChampionSaveData> load() {
        if (loadIfAble()) {
            plugin.debug(Level.FINE, "Loading data.json");
        }
        Collection<ChampionSaveData> collection = new HashSet<>();
        for (String key : configuration.getKeys(false)) {
            if (!configuration.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection section = configuration.getConfigurationSection(key);
            UUID uuid = UUID.fromString(key);
            ChampionSaveData saveData = new ChampionSaveData(uuid);
            boolean hadReset = checkResetAndSetLevels(section, saveData, true);
            saveData.setHighestReachedLevel(section.getInt("highest-reached-level"));
            saveData.setBonusLevels(section.getInt("bonus-levels"));
            if (hadReset) {
                saveData.setUnusedStatPoints(saveData.getHighestReachedLevel());
            } else {
                saveData.setUnusedStatPoints(section.getInt("unused-stat-points"));
            }
            collection.add(saveData);
        }
        return collection;
    }

    @Override
    public ChampionSaveData load(UUID uuid) {
        if (loadIfAble()) {
            plugin.debug(Level.FINE, "Loading data.json");
        }
        if (!configuration.isConfigurationSection(uuid.toString())) {
            plugin.debug(Level.FINER, "Unable to find player with UUID " + uuid.toString());
            return null;
        }
        String key = uuid.toString();
        ConfigurationSection section = configuration.getConfigurationSection(key);
        ChampionSaveData saveData = new ChampionSaveData(uuid);
        boolean hadReset = checkResetAndSetLevels(section, saveData, true);
        saveData.setHighestReachedLevel(section.getInt("highest-reached-level"));
        saveData.setBonusLevels(section.getInt("bonus-levels"));
        if (hadReset) {
            saveData.setUnusedStatPoints(saveData.getHighestReachedLevel());
        } else {
            saveData.setUnusedStatPoints(section.getInt("unused-stat-points"));
        }
        return saveData;
    }

    private boolean checkResetAndSetLevels(ConfigurationSection section, ChampionSaveData champion, boolean hadReset) {
        if (section.isConfigurationSection("stats")) {
            ConfigurationSection statsSection = section.getConfigurationSection("stats");
            for (String k : statsSection.getKeys(false)) {
                StrifeStat stat = plugin.getStatManager().getStat(k);
                if (stat == null) {
                    continue;
                }
                champion.setLevel(stat, statsSection.getInt(k));
            }
            hadReset = false;
        }
        return hadReset;
    }

    private boolean loadIfAble() {
        long now = System.currentTimeMillis();
        long diff = now - lastLoaded;
        plugin.debug(Level.FINER, "Loading data.json...");
        if (diff >= plugin.getSettings().getInt("config.configuration-load-period", 10)) {
            configuration.load();
            lastLoaded = now;
            return true;
        }
        return false;
    }

}
