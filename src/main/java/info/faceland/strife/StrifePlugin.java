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
package info.faceland.strife;

import com.comphenix.xp.lookup.LevelingRate;
import com.tealcube.minecraft.bukkit.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.logging.PluginLogger;
import com.tealcube.minecraft.bukkit.facecore.plugin.FacePlugin;
import com.tealcube.minecraft.bukkit.shade.objecthunter.exp4j.Expression;
import com.tealcube.minecraft.bukkit.shade.objecthunter.exp4j.ExpressionBuilder;
import info.faceland.strife.api.StrifeCraftExperienceManager;
import info.faceland.strife.api.StrifeEnchantExperienceManager;
import info.faceland.strife.api.StrifeExperienceManager;
import info.faceland.strife.api.StrifeFishExperienceManager;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.commands.AttributesCommand;
import info.faceland.strife.commands.LevelUpCommand;
import info.faceland.strife.commands.StrifeCommand;
import info.faceland.strife.commands.UniqueEntityCommand;
import info.faceland.strife.data.*;
import info.faceland.strife.listeners.*;
import info.faceland.strife.managers.*;
import info.faceland.strife.menus.LevelupMenu;
import info.faceland.strife.menus.StatsMenu;
import info.faceland.strife.stats.StrifeStat;
import info.faceland.strife.storage.DataStorage;
import info.faceland.strife.storage.JsonDataStorage;
import info.faceland.strife.tasks.*;
import io.pixeloutlaw.minecraft.spigot.config.MasterConfiguration;
import io.pixeloutlaw.minecraft.spigot.config.VersionedConfiguration;
import io.pixeloutlaw.minecraft.spigot.config.VersionedSmartYamlConfiguration;
import ninja.amp.ampmenus.MenuListener;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import se.ranzdo.bukkit.methodcommand.CommandHandler;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class StrifePlugin extends FacePlugin {

    private static StrifePlugin instance;

    private PluginLogger debugPrinter;
    private VersionedSmartYamlConfiguration configYAML;
    private VersionedSmartYamlConfiguration statsYAML;
    private VersionedSmartYamlConfiguration baseStatsYAML;
    private VersionedSmartYamlConfiguration uniqueEnemiesYAML;
    private VersionedSmartYamlConfiguration equipmentYAML;
    private StrifeStatManager statManager;
    private BarrierManager barrierManager;
    private BleedManager bleedManager;
    private MonsterManager monsterManager;
    private UniqueEntityManager uniqueEntityManager;
    private EntityEquipmentManager equipmentManager;
    private MultiplierManager multiplierManager;
    private DataStorage storage;
    private ChampionManager championManager;
    private StrifeExperienceManager experienceManager;
    private StrifeCraftExperienceManager craftExperienceManager;
    private StrifeEnchantExperienceManager enchantExperienceManager;
    private StrifeFishExperienceManager fishExperienceManager;
    private EntityStatCache entityStatCache;
    private SaveTask saveTask;
    private TrackedPruneTask trackedPruneTask;
    private HealthRegenTask regenTask;
    private BleedTask bleedTask;
    private BarrierTask barrierTask;
    private DarknessReductionTask darkTask;
    private AttackSpeedTask attackSpeedTask;
    private BlockTask blockTask;
    private UniquePruneTask uniquePruneTask;
    private UniqueParticleTask uniqueParticleTask;
    private CommandHandler commandHandler;
    private MasterConfiguration settings;
    private LevelingRate levelingRate;
    private LevelingRate craftingRate;
    private LevelingRate enchantRate;
    private LevelingRate fishRate;
    private LevelupMenu levelupMenu;
    private StatsMenu statsMenu;

    public static void setInstance(StrifePlugin plugin) {
        instance = plugin;
    }

    public static StrifePlugin getInstance() {
        return instance;
    }

    final private static long attackTickRate = 2L;

    @Override
    public void enable() {
        setInstance(this);
        debugPrinter = new PluginLogger(this);
        configYAML = new VersionedSmartYamlConfiguration(new File(getDataFolder(), "config.yml"),
                getResource("config.yml"), VersionedConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);
        statsYAML = new VersionedSmartYamlConfiguration(new File(getDataFolder(), "stats.yml"),
                getResource("stats.yml"), VersionedConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);
        baseStatsYAML = new VersionedSmartYamlConfiguration(new File(getDataFolder(), "base-entity-stats.yml"),
                getResource("base-entity-stats.yml"), VersionedConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);
        uniqueEnemiesYAML = new VersionedSmartYamlConfiguration(new File(getDataFolder(), "unique-enemies.yml"),
                getResource("unique-enemies.yml"), VersionedConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);
        equipmentYAML = new VersionedSmartYamlConfiguration(new File(getDataFolder(), "equipment.yml"),
                getResource("equipment.yml"), VersionedConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);

        statManager = new StrifeStatManager();
        barrierManager = new BarrierManager();
        bleedManager = new BleedManager();
        monsterManager = new MonsterManager(this);
        uniqueEntityManager = new UniqueEntityManager(this);
        equipmentManager = new EntityEquipmentManager();
        multiplierManager = new MultiplierManager();
        storage = new JsonDataStorage(this);
        championManager = new ChampionManager(this);
        experienceManager = new ExperienceManager(this);
        craftExperienceManager = new CraftExperienceManager(this);
        enchantExperienceManager = new EnchantExperienceManager(this);
        fishExperienceManager = new FishExperienceManager(this);
        entityStatCache = new EntityStatCache(this);
        commandHandler = new CommandHandler(this);

        MenuListener.getInstance().register(this);

        if (configYAML.update()) {
            getLogger().info("Updating config.yml");
        }
        if (statsYAML.update()) {
            getLogger().info("Updating stats.yml");
        }
        if (baseStatsYAML.update()) {
            getLogger().info("Updating base-entity-stats.yml");
        }
        if (uniqueEnemiesYAML.update()) {
            getLogger().info("Updating unique-enemies.yml");
        }
        if (equipmentYAML.update()) {
            getLogger().info("Updating equipment.yml");
        }

        settings = MasterConfiguration.loadFromFiles(configYAML);

        buildEquipment();
        buildLevelpointStats();
        buildBaseStats();
        buildUniqueEnemies();

        //Backup old loading from data.json
        //for (ChampionSaveData saveData : storage.oldLoad()) {
        //    championManager.addChampion(new Champion(saveData));
        //}
        //storage.saveAll();
        //championManager.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            ChampionSaveData saveData = storage.load(player.getUniqueId());
            championManager.addChampion(new Champion(saveData));
        }

        saveTask = new SaveTask(this);
        trackedPruneTask = new TrackedPruneTask(this);
        regenTask = new HealthRegenTask(this);
        bleedTask = new BleedTask(this);
        barrierTask = new BarrierTask(this);
        darkTask = new DarknessReductionTask();
        attackSpeedTask = new AttackSpeedTask(attackTickRate);
        blockTask = new BlockTask();
        uniquePruneTask = new UniquePruneTask(this);
        uniqueParticleTask = new UniqueParticleTask(this);

        commandHandler.registerCommands(new AttributesCommand(this));
        commandHandler.registerCommands(new LevelUpCommand(this));
        commandHandler.registerCommands(new StrifeCommand(this));
        commandHandler.registerCommands(new UniqueEntityCommand(this));

        levelingRate = new LevelingRate();
        Expression normalExpr = new ExpressionBuilder(settings.getString("config.leveling.formula",
            "(5+(2*LEVEL)+(LEVEL^1.2))*LEVEL")).variable("LEVEL").build();
        for (int i = 0; i < 200; i++) {
            levelingRate.put(i, i, (int) Math.round(normalExpr.setVariable("LEVEL", i).evaluate()));
        }

        craftingRate = new LevelingRate();
        Expression craftExpr = new ExpressionBuilder(settings.getString("config.leveling.crafting",
            "(5+(2*LEVEL)+(LEVEL^1.2))*LEVEL")).variable("LEVEL").build();
        for (int i = 0; i < 60; i++) {
            craftingRate.put(i, i, (int) Math.round(craftExpr.setVariable("LEVEL", i).evaluate()));
        }

        enchantRate = new LevelingRate();
        Expression enchantExpr = new ExpressionBuilder(settings.getString("config.leveling.enchanting",
            "(5+(2*LEVEL)+(LEVEL^1.2))*LEVEL")).variable("LEVEL").build();
        for (int i = 0; i < 60; i++) {
            enchantRate.put(i, i, (int) Math.round(enchantExpr.setVariable("LEVEL", i).evaluate()));
        }

        fishRate = new LevelingRate();
        Expression fishExpr = new ExpressionBuilder(settings.getString("config.leveling.fishing",
            "(5+(2*LEVEL)+(LEVEL^1.2))*LEVEL")).variable("LEVEL").build();
        for (int i = 0; i < 60; i++) {
            fishRate.put(i, i, (int) Math.round(fishExpr.setVariable("LEVEL", i).evaluate()));
        }

        trackedPruneTask.runTaskTimer(this,
            20L * 61, // Start save after 1 minute, 1 second cuz yolo
            20L * 60 // Run every 1 minute after that
        );
        saveTask.runTaskTimer(this,
            20L * 680, // Start save after 11 minutes, 20 seconds cuz yolo
            20L * 600 // Run every 10 minutes after that
        );
        regenTask.runTaskTimer(this,
            20L * 10, // Start timer after 10s
            20L * 2 // Run it every 2s after
        );
        bleedTask.runTaskTimer(this,
            20L * 10, // Start timer after 10s
           12L // Run it about every half second
        );
        barrierTask.runTaskTimer(this,
            201L, // Start timer after 10.05s
            4L // Run it every 1/5th of a second after
        );
        darkTask.runTaskTimer(this,
            20L * 10, // Start timer after 10s
            10L  // Run it every 0.5s after
        );
        attackSpeedTask.runTaskTimer(this, 5L, attackTickRate);
        blockTask.runTaskTimer(this, 5L, 5L);
        uniquePruneTask.runTaskTimer(this, 30 * 20L, 30 * 20L);
        uniqueParticleTask.runTaskTimer(this, 20* 20L, 2L);
        Bukkit.getPluginManager().registerEvents(new EndermanListener(), this);
        Bukkit.getPluginManager().registerEvents(new ExperienceListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HealthListener(), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DOTListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WandListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BowListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HeadDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DataListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AttributeUpdateListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("Bullion") != null) {
            Bukkit.getPluginManager().registerEvents(new BullionListener(this), this);
        }

        levelupMenu = new LevelupMenu(this, getStatManager().getStats());
        statsMenu = new StatsMenu(this);
        debug(Level.INFO, "v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void disable() {
        storage.saveAll();
        uniqueEntityManager.killAllSpawnedUniques();
        HandlerList.unregisterAll(this);

        saveTask.cancel();
        trackedPruneTask.cancel();
        regenTask.cancel();
        bleedTask.cancel();
        barrierTask.cancel();
        darkTask.cancel();
        attackSpeedTask.cancel();
        blockTask.cancel();

        configYAML = null;
        baseStatsYAML = null;
        statsYAML = null;

        craftingRate = null;
        enchantRate = null;
        fishRate = null;

        statManager = null;
        monsterManager = null;
        uniqueEntityManager = null;
        equipmentManager = null;
        bleedManager = null;
        barrierManager = null;
        multiplierManager = null;

        storage = null;
        championManager = null;
        experienceManager = null;
        craftExperienceManager = null;
        enchantExperienceManager = null;
        fishExperienceManager = null;
        entityStatCache = null;

        saveTask = null;
        trackedPruneTask = null;
        regenTask = null;
        bleedTask = null;
        darkTask = null;
        attackSpeedTask = null;
        blockTask = null;

        commandHandler = null;
        settings = null;

        debug(Level.INFO, "v" + getDescription().getVersion() + " disabled");
    }

    private void buildEquipment() {
        for (String itemStackKey : equipmentYAML.getKeys(false)) {
            if (!equipmentYAML.isConfigurationSection(itemStackKey)) {
                continue;
            }
            ConfigurationSection cs = equipmentYAML.getConfigurationSection(itemStackKey);

            Material material;
            String type = cs.getString("material");
            try {
                material = Material.getMaterial(type);
            } catch (Exception e) {
                getLogger().severe("Skipping item " + itemStackKey + " for invalid material");
                continue;
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();

            String name = cs.getString("name", "");
            if (StringUtils.isNotBlank(name)) {
                meta.setDisplayName(TextUtils.color(name));
            }

            List<String> lore = new ArrayList<>();
            for (String line : cs.getStringList("lore")) {
                lore.add(TextUtils.color(line));
            }
            meta.setLore(lore);

            itemStack.setItemMeta(meta);
            equipmentManager.getItemMap().put(itemStackKey, itemStack);
        }
    }

    private void buildLevelpointStats() {
        List<StrifeStat> stats = new ArrayList<>();
        List<String> loadedStats = new ArrayList<>();
        for (String key : statsYAML.getKeys(false)) {
            if (!statsYAML.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection cs = statsYAML.getConfigurationSection(key);
            StrifeStat stat = new StrifeStat(key);
            stat.setName(cs.getString("name"));
            stat.setDescription(cs.getStringList("description"));
            stat.setDyeColor(DyeColor.valueOf(cs.getString("dye-color", "WHITE")));
            stat.setSlot(cs.getInt("slot"));
            stat.setStartCap(cs.getInt("starting-cap", 0));
            stat.setMaxCap(cs.getInt("maximum-cap", 100));
            stat.setLevelsToRaiseCap(cs.getInt("levels-to-raise-cap", -1));
            Map<String, Integer> baseStatRequirements = new HashMap<>();
            if (cs.isConfigurationSection("base-attribute-requirements")) {
                ConfigurationSection reqs = cs.getConfigurationSection("base-attribute-requirements");
                for (String k : reqs.getKeys(false)) {
                    baseStatRequirements.put(k, reqs.getInt(k));
                }
            }
            Map<String, Integer> raiseStatCapAttributes = new HashMap<>();
            if (cs.isConfigurationSection("attributes-to-raise-cap")) {
                ConfigurationSection raiseReqs = cs.getConfigurationSection("attributes-to-raise-cap");
                for (String k : raiseReqs.getKeys(false)) {
                    raiseStatCapAttributes.put(k, raiseReqs.getInt(k));
                }
            }
            Map<StrifeAttribute, Double> attributeMap = new HashMap<>();
            if (cs.isConfigurationSection("attributes")) {
                ConfigurationSection attrCS = cs.getConfigurationSection("attributes");
                for (String k : attrCS.getKeys(false)) {
                    StrifeAttribute attr = StrifeAttribute.valueOf(k);
                    attributeMap.put(attr, attrCS.getDouble(k));
                }
            }
            stat.setStatIncreaseIncrements(raiseStatCapAttributes);
            stat.setBaseStatRequirements(baseStatRequirements);
            stat.setAttributeMap(attributeMap);
            stats.add(stat);
            loadedStats.add(stat.getKey());
        }
        for (StrifeStat stat : stats) {
            getStatManager().addStat(stat);
        }
        debug(Level.INFO, "Loaded stats: " + loadedStats.toString());
    }

    private void buildBaseStats() {
        for (String entityKey : baseStatsYAML.getKeys(false)) {
            if (!baseStatsYAML.isConfigurationSection(entityKey)) {
                continue;
            }
            EntityType entityType = EntityType.valueOf(entityKey);
            ConfigurationSection cs = baseStatsYAML.getConfigurationSection(entityKey);
            EntityStatData data = new EntityStatData();
            if (cs.isConfigurationSection("base-values")) {
                ConfigurationSection attrCS = cs.getConfigurationSection("base-values");
                for (String k : attrCS.getKeys(false)) {
                    StrifeAttribute attr = StrifeAttribute.valueOf(k);
                    data.putBaseValue(attr, attrCS.getDouble(k));
                }
            }
            if (cs.isConfigurationSection("per-level")) {
                ConfigurationSection attrCS = cs.getConfigurationSection("per-level");
                for (String k : attrCS.getKeys(false)) {
                    StrifeAttribute attr = StrifeAttribute.valueOf(k);
                    data.putPerLevel(attr, attrCS.getDouble(k));
                }
            }
            if (cs.isConfigurationSection("per-bonus-level")) {
                ConfigurationSection attrCS = cs.getConfigurationSection("per-bonus-level");
                for (String k : attrCS.getKeys(false)) {
                    StrifeAttribute attr = StrifeAttribute.valueOf(k);
                    data.putPerBonusLevel(attr, attrCS.getDouble(k));
                }
            }
            getMonsterManager().addEntityData(entityType, data);
        }
    }

    private void buildUniqueEnemies() {
        for (String entityNameKey : uniqueEnemiesYAML.getKeys(false)) {
            getLogger().info("Attempting to load unique: " + entityNameKey);
            if (!uniqueEnemiesYAML.isConfigurationSection(entityNameKey)) {
                continue;
            }
            ConfigurationSection cs = uniqueEnemiesYAML.getConfigurationSection(entityNameKey);

            UniqueEntity uniqueEntity = new UniqueEntity();

            String type = cs.getString("type");
            try {
                uniqueEntity.setType(EntityType.valueOf(type));
            } catch (Exception e) {
                getLogger().severe("Failed to parse entity " + entityNameKey + ". Invalid type: " + type);
                continue;
            }

            uniqueEntity.setName(TextUtils.color(cs.getString("name", "&fSET &cA &9NAME")));

            ConfigurationSection attrCS = cs.getConfigurationSection("attributes");
            Map<StrifeAttribute, Double> attributeMap = new HashMap<>();
            for (String k : attrCS.getKeys(false)) {
                getLogger().info("Setting attr " + k + " for unique " + entityNameKey);
                StrifeAttribute attr = StrifeAttribute.valueOf(k);
                attributeMap.put(attr, attrCS.getDouble(k));
            }
            uniqueEntity.setAttributeMap(attributeMap);

            ConfigurationSection equipmentCS = cs.getConfigurationSection("equipment");
            if (equipmentCS != null) {
                uniqueEntity.setMainHandItem(equipmentManager.getItem(equipmentCS.getString("main-hand", "")));
                uniqueEntity.setOffHandItem(equipmentManager.getItem(equipmentCS.getString("off-hand", "")));
                uniqueEntity.setHelmetItem(equipmentManager.getItem(equipmentCS.getString("helmet", "")));
                uniqueEntity.setChestItem(equipmentManager.getItem(equipmentCS.getString("chestplate", "")));
                uniqueEntity.setLegsItem(equipmentManager.getItem(equipmentCS.getString("leggings", "")));
                uniqueEntity.setBootsItem(equipmentManager.getItem(equipmentCS.getString("boots", "")));
            }

            ConfigurationSection particleCS = cs.getConfigurationSection("particle");
            if (particleCS != null) {
                try {
                    uniqueEntity.setParticle(Particle.valueOf(particleCS.getString("effect")));
                } catch (Exception e) {
                    getLogger().severe("Particle for " + entityNameKey + " is invalid. Setting to FLAME");
                    uniqueEntity.setParticle(Particle.FLAME);
                }
                uniqueEntity.setParticleCount(particleCS.getInt("count", 1));
                uniqueEntity.setParticleRadius((float) particleCS.getDouble("radius", 0));
            } else {
                uniqueEntity.setParticle(null);
            }

            getLogger().info("Loaded unique: " + entityNameKey);
            uniqueEntityManager.addUniqueEntity(entityNameKey, uniqueEntity);
        }
    }

    public AttackSpeedTask getAttackSpeedTask() {
        return attackSpeedTask;
    }

    public BlockTask getBlockTask() {
        return blockTask;
    }

    public StrifeStatManager getStatManager() {
        return statManager;
    }

    public BarrierManager getBarrierManager() {
      return barrierManager;
    }

    public BleedManager getBleedManager() {
    return bleedManager;
  }

    public MonsterManager getMonsterManager() {
        return monsterManager;
    }

    public UniqueEntityManager getUniqueEntityManager() {
        return uniqueEntityManager;
    }

    public MultiplierManager getMultiplierManager() {
        return multiplierManager;
    }

    public StrifeCraftExperienceManager getCraftExperienceManager() {
        return craftExperienceManager;
    }

    public StrifeEnchantExperienceManager getEnchantExperienceManager() {
        return enchantExperienceManager;
    }

    public StrifeFishExperienceManager getFishExperienceManager() {
        return fishExperienceManager;
    }

    public StrifeExperienceManager getExperienceManager() {
        return experienceManager;
    }

    public void debug(Level level, String... messages) {
        if (debugPrinter != null) {
            debugPrinter.log(level, Arrays.asList(messages));
        }
    }

    public DataStorage getStorage() {
        return storage;
    }

    public ChampionManager getChampionManager() {
        return championManager;
    }

    public EntityStatCache getEntityStatCache() {
        return entityStatCache;
    }

    public MasterConfiguration getSettings() {
        return settings;
    }

    public LevelupMenu getLevelupMenu() {
        return levelupMenu;
    }

    public StatsMenu getStatsMenu() { return statsMenu; }

    public LevelingRate getLevelingRate() {
        return levelingRate;
    }
    public LevelingRate getCraftingRate() {
        return craftingRate;
    }
    public LevelingRate getEnchantRate() {
        return enchantRate;
    }
    public LevelingRate getFishRate() {
        return fishRate;
    }
}
