package info.faceland.strife.managers;

import static info.faceland.strife.data.ability.Ability.TargetType.SINGLE_OTHER;

import com.tealcube.minecraft.bukkit.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import info.faceland.strife.StrifePlugin;
import info.faceland.strife.conditions.Condition;
import info.faceland.strife.data.AbilityIconData;
import info.faceland.strife.data.StrifeMob;
import info.faceland.strife.data.ability.Ability;
import info.faceland.strife.data.ability.Ability.TargetType;
import info.faceland.strife.data.ability.EntityAbilitySet;
import info.faceland.strife.data.ability.EntityAbilitySet.TriggerAbilityPhase;
import info.faceland.strife.data.ability.EntityAbilitySet.TriggerAbilityType;
import info.faceland.strife.data.champion.LifeSkillType;
import info.faceland.strife.data.champion.StrifeAttribute;
import info.faceland.strife.effects.Effect;
import info.faceland.strife.effects.Wait;
import info.faceland.strife.stats.AbilitySlot;
import info.faceland.strife.util.ItemUtil;
import info.faceland.strife.util.LogUtil;
import info.faceland.strife.util.PlayerDataUtil;
import info.faceland.strife.util.TargetingUtil;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class AbilityManager {

  private final StrifePlugin plugin;
  private final Map<String, Ability> loadedAbilities = new HashMap<>();
  private final Map<LivingEntity, Map<Ability, Integer>> coolingDownAbilities = new ConcurrentHashMap<>();
  private final Map<UUID, Map<Ability, Integer>> savedPlayerCooldowns = new ConcurrentHashMap<>();
  private final Map<UUID, Long> abilityGlobalCd = new HashMap<>();

  private static final String ON_COOLDOWN = TextUtils.color("&e&lAbility On Cooldown!");
  private static final String NO_TARGET = TextUtils.color("&e&lNo Ability Target Found!");
  private static final String NO_REQUIRE = TextUtils.color("&c&lAbility Requirements Not Met!");

  public AbilityManager(StrifePlugin plugin) {
    this.plugin = plugin;
  }

  public Ability getAbility(String name) {
    if (loadedAbilities.containsKey(name)) {
      return loadedAbilities.get(name);
    }
    LogUtil.printWarning("Attempted to get unknown ability '" + name + "'.");
    return null;
  }

  public void cooldownReduce(LivingEntity livingEntity, Ability ability, int ticks) {
    if (!coolingDownAbilities.containsKey(livingEntity)) {
      return;
    }
    int curTicks = coolingDownAbilities.get(livingEntity).getOrDefault(ability, 0);
    if (curTicks - ticks <= 0) {
      LogUtil.printDebug(" Cd Reduce - ability " + ability.getId() + " refreshed");
      coolingDownAbilities.get(livingEntity).remove(ability);
      updateIcons(livingEntity);
      return;
    }
    int newTicks = curTicks - ticks;
    LogUtil.printDebug(" Cd Reduce - ability " + ability.getId() + " reduced from " +
        curTicks + " to " + newTicks);
    coolingDownAbilities.get(livingEntity).put(ability, newTicks);
    updateIcons(livingEntity);
  }

  private void startAbilityCooldown(LivingEntity livingEntity, Ability ability) {
    if (!coolingDownAbilities.containsKey(livingEntity)) {
      coolingDownAbilities.put(livingEntity, new ConcurrentHashMap<>());
    }
    coolingDownAbilities.get(livingEntity).put(ability, ability.getCooldown() * 20);
  }

  public double getCooldownTicks(LivingEntity livingEntity, Ability ability) {
    if (!coolingDownAbilities.containsKey(livingEntity)) {
      return 0;
    }
    return coolingDownAbilities.get(livingEntity).getOrDefault(ability, 0);
  }

  public void tickAbilityCooldowns(int tickRate) {
    for (LivingEntity le : coolingDownAbilities.keySet()) {
      if (le == null || !le.isValid()) {
        coolingDownAbilities.remove(le);
        continue;
      }
      for (Ability ability : coolingDownAbilities.get(le).keySet()) {
        int ticks = coolingDownAbilities.get(le).get(ability);
        if (ticks <= tickRate) {
          coolingDownAbilities.get(le).remove(ability);
          continue;
        }
        coolingDownAbilities.get(le).put(ability, ticks - tickRate);
      }
    }
  }

  public void savePlayerCooldowns(Player player) {
    if (coolingDownAbilities.containsKey(player)) {
      savedPlayerCooldowns.put(player.getUniqueId(), coolingDownAbilities.get(player));
      coolingDownAbilities.remove(player);
    }
  }

  public void loadPlayerCooldowns(Player player) {
    coolingDownAbilities.put(player, new ConcurrentHashMap<>());
    if (savedPlayerCooldowns.containsKey(player.getUniqueId())) {
      coolingDownAbilities.put(player, savedPlayerCooldowns.get(player.getUniqueId()));
      savedPlayerCooldowns.remove(player.getUniqueId());
      updateIcons(player);
    }
  }

  public boolean isCooledDown(LivingEntity livingEntity, Ability ability) {
    if (!coolingDownAbilities.containsKey(livingEntity)) {
      coolingDownAbilities.put(livingEntity, new ConcurrentHashMap<>());
    }
    return !coolingDownAbilities.get(livingEntity).containsKey(ability);
  }

  public boolean execute(final Ability ability, final StrifeMob caster, LivingEntity target) {
    if (ability.getCooldown() != 0 && !isCooledDown(caster.getEntity(), ability)) {
      doOnCooldownPrompt(caster, ability);
      return false;
    }
    if (isCasterOnGlobalCooldown(caster.getEntity().getUniqueId())) {
      return false;
    }
    if (!PlayerDataUtil.areConditionsMet(caster, caster, ability.getConditions())) {
      doRequirementNotMetPrompt(caster, ability);
      return false;
    }
    Set<LivingEntity> targets = getTargets(caster, target, ability);
    if (targets == null) {
      throw new NullArgumentException("Null target list on ability " + ability.getId());
    }
    if (ability.getTargetType() == SINGLE_OTHER) {
      TargetingUtil.filterFriendlyEntities(targets, caster, ability.isFriendly());
      if (targets.isEmpty()) {
        doTargetNotFoundPrompt(caster, ability);
        return false;
      }
    }
    if (ability.getCooldown() != 0) {
      startAbilityCooldown(caster.getEntity(), ability);
    }
    if (caster.getChampion() != null && ability.getAbilityIconData() != null) {
      caster.getChampion().getDetailsContainer().addWeights(ability);
    }
    List<Effect> taskEffects = new ArrayList<>();
    int waitTicks = 0;
    for (Effect effect : ability.getEffects()) {
      if (effect instanceof Wait) {
        LogUtil.printDebug("Effects in this chunk: " + taskEffects.toString());
        runEffects(caster, targets, taskEffects, waitTicks);
        waitTicks += ((Wait) effect).getTickDelay();
        taskEffects = new ArrayList<>();
        continue;
      }
      taskEffects.add(effect);
      LogUtil.printDebug("Added effect " + effect.getId() + " to task list");
    }
    runEffects(caster, targets, taskEffects, waitTicks);
    return true;
  }

  public boolean execute(Ability ability, final StrifeMob caster) {
    return execute(ability, caster, null);
  }

  public void abilityCast(StrifeMob caster, TriggerAbilityType type) {
    EntityAbilitySet abilitySet = caster.getAbilitySet();
    if (abilitySet == null) {
      return;
    }
    checkPhaseChange(caster);
    TriggerAbilityPhase phase = abilitySet.getPhase();
    Map<TriggerAbilityPhase, Set<Ability>> abilitySection = abilitySet.getAbilities(type);
    if (abilitySection == null) {
      return;
    }
    Set<Ability> abilities = abilitySection.get(phase);
    if (abilities == null) {
      return;
    }
    for (Ability a : abilities) {
      execute(a, caster);
    }
  }

  private void checkPhaseChange(StrifeMob strifeMob) {
    if (strifeMob.getAbilitySet() == null) {
      return;
    }
    LogUtil.printDebug(" - Checking phase switch");
    TriggerAbilityPhase currentPhase = strifeMob.getAbilitySet().getPhase();
    LogUtil.printDebug(" - Current Phase: " + currentPhase);
    TriggerAbilityPhase newPhase = EntityAbilitySet.phaseFromEntityHealth(strifeMob.getEntity());
    if (newPhase.ordinal() > currentPhase.ordinal()) {
      strifeMob.getAbilitySet().setPhase(newPhase);
      LogUtil.printDebug(" - New Phase: " + newPhase);
      abilityCast(strifeMob, TriggerAbilityType.PHASE_SHIFT);
    }
  }

  private void runEffects(StrifeMob caster, Set<LivingEntity> targets, List<Effect> effectList,
      int delay) {
    Bukkit.getScheduler().runTaskLater(StrifePlugin.getInstance(), () -> {
      LogUtil.printDebug("Effect task (Location) started - " + effectList.toString());
      if (!caster.getEntity().isValid()) {
        LogUtil.printDebug(" - Task cancelled, caster is dead");
        return;
      }
      for (Effect effect : effectList) {
        LogUtil.printDebug(" - Executing effect " + effect.getId());
        plugin.getEffectManager().execute(effect, caster, targets);
      }
      LogUtil.printDebug(" - Completed effect task.");
    }, delay);
  }

  private Set<LivingEntity> getTargets(StrifeMob caster, LivingEntity target, Ability ability) {
    Set<LivingEntity> targets = new HashSet<>();
    switch (ability.getTargetType()) {
      case SELF:
      case PARTY:
        targets.add(caster.getEntity());
        return targets;
      case MASTER:
        if (caster.getMaster() != null) {
          targets.add(caster.getMaster());
        }
        return targets;
      case MINIONS:
        for (StrifeMob mob : caster.getMinions()) {
          targets.add(mob.getEntity());
        }
        return targets;
      case SINGLE_OTHER:
        if (target != null) {
          targets.add(target);
          return targets;
        }
        LivingEntity newTarget = TargetingUtil
            .selectFirstEntityInSight(caster.getEntity(), ability.getRange());
        if (newTarget != null) {
          targets.add(newTarget);
        }
        return targets;
      case AREA_LINE:
        targets = TargetingUtil.getEntitiesInLine(caster.getEntity(), ability.getRange());
        return targets;
      case TARGET_AREA:
        Location loc = TargetingUtil
            .getTargetLocation(caster.getEntity(), target, ability.getRange(), false);
        return TargetingUtil.getTempStandTargetList(loc, false);
      case TARGET_GROUND:
        Location loc2 = TargetingUtil
            .getTargetLocation(caster.getEntity(), target, ability.getRange(), true);
        return TargetingUtil.getTempStandTargetList(loc2, true);
    }
    return null;
  }

  private void updateIcons(LivingEntity livingEntity) {
    if (livingEntity instanceof Player) {
      plugin.getAbilityIconManager().updateAbilityIconDamageMeters((Player) livingEntity, true);
    }
  }

  private void doTargetNotFoundPrompt(StrifeMob caster, Ability ability) {
    LogUtil.printDebug("Failed. No target found for ability " + ability.getId());
    if (!(ability.isShowMessages() && caster.getEntity() instanceof Player)) {
      return;
    }
    MessageUtils.sendActionBar((Player) caster.getEntity(), NO_TARGET);
    ((Player) caster.getEntity()).playSound(
        caster.getEntity().getLocation(),
        Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
        1f,
        1f);
  }

  private void doRequirementNotMetPrompt(StrifeMob caster, Ability ability) {
    LogUtil.printDebug("Failed. Requirement not met for ability " + ability.getId());
    if (!(ability.isShowMessages() && caster.getEntity() instanceof Player)) {
      return;
    }
    MessageUtils.sendActionBar((Player) caster.getEntity(), NO_REQUIRE);
    ((Player) caster.getEntity()).playSound(
        caster.getEntity().getLocation(),
        Sound.BLOCK_LAVA_POP,
        1f,
        0.5f);
  }

  private void doOnCooldownPrompt(StrifeMob caster, Ability ability) {
    LogUtil.printDebug("Failed. Ability " + ability.getId() + " is on cooldown");
    if (!(ability.isShowMessages() && caster.getEntity() instanceof Player)) {
      return;
    }
    MessageUtils.sendActionBar((Player) caster.getEntity(), ON_COOLDOWN);
    ((Player) caster.getEntity()).playSound(
        caster.getEntity().getLocation(),
        Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
        1f,
        1.5f);
  }

  private boolean isCasterOnGlobalCooldown(UUID uuid) {
    if (abilityGlobalCd.containsKey(uuid) && abilityGlobalCd.get(uuid) + 50 >
        System.currentTimeMillis()) {
      return true;
    }
    abilityGlobalCd.put(uuid, System.currentTimeMillis());
    return false;
  }

  public void loadAbility(String key, ConfigurationSection cs) {
    String name = TextUtils.color(cs.getString("name", "ABILITY NOT NAMED"));
    TargetType targetType;
    try {
      targetType = TargetType.valueOf(cs.getString("target-type"));
    } catch (Exception e) {
      LogUtil.printWarning("Skipping load of ability " + key + " - Invalid target type.");
      return;
    }
    int cooldown = cs.getInt("cooldown", 0);
    int range = cs.getInt("range", 0);
    List<String> effectStrings = cs.getStringList("effects");
    if (effectStrings.isEmpty()) {
      LogUtil.printWarning("Skipping ability " + key + " - No effects.");
      return;
    }
    List<Effect> effects = new ArrayList<>();
    for (String s : effectStrings) {
      Effect effect = plugin.getEffectManager().getEffect(s);
      if (effect == null) {
        LogUtil.printWarning(" Failed to add unknown effect '" + s + "' to ability '" + key + "'");
        continue;
      }
      effects.add(effect);
      LogUtil.printDebug(" Added effect '" + s + "' to ability '" + key + "'");
    }
    boolean showMessages = cs.getBoolean("show-messages", false);
    List<String> conditionStrings = cs.getStringList("conditions");
    Set<Condition> conditions = new HashSet<>();
    for (String s : conditionStrings) {
      Condition condition = plugin.getEffectManager().getConditions().get(s);
      if (condition == null) {
        LogUtil.printWarning(" Invalid condition '" + s + "' for ability '" + key + "'. Skipping.");
        continue;
      }
      conditions.add(plugin.getEffectManager().getConditions().get(s));
    }
    AbilityIconData abilityIconData = buildIconData(key, cs.getConfigurationSection("icon"));
    boolean friendly = cs.getBoolean("friendly", false);
    loadedAbilities.put(key, new Ability(key, name, effects, targetType, range, cooldown,
        showMessages, conditions, friendly, abilityIconData));
    LogUtil.printDebug("Loaded ability " + key + " successfully.");
  }

  private AbilityIconData buildIconData(String key, ConfigurationSection iconSection) {
    if (iconSection == null) {
      return null;
    }
    LogUtil.printDebug("Ability " + key + " has icon!");
    String format = TextUtils.color(iconSection.getString("format", "&f&l"));
    Material material = Material.valueOf(iconSection.getString("material"));
    List<String> lore = TextUtils.color(iconSection.getStringList("lore"));
    ItemStack icon = new ItemStack(material);
    ItemStackExtensionsKt.setDisplayName(icon, format + AbilityIconManager.ABILITY_PREFIX + key);
    ItemStackExtensionsKt.setLore(icon, lore);
    ItemStackExtensionsKt.addItemFlags(icon, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
    ItemUtil.removeAttributes(icon);

    AbilityIconData data = new AbilityIconData(icon);

    data.setAbilitySlot(AbilitySlot.valueOf(iconSection.getString("trigger-slot")));
    data.setLevelRequirement(iconSection.getInt("level-requirement", 0));
    data.setBonusLevelRequirement(iconSection.getInt("bonus-level-requirement", 0));

    Map<StrifeAttribute, Integer> attrReqs = new HashMap<>();
    ConfigurationSection attrSection = iconSection
        .getConfigurationSection("attribute-requirements");
    if (attrSection != null) {
      for (String s : attrSection.getKeys(false)) {
        StrifeAttribute attr = plugin.getAttributeManager().getAttribute(s);
        int value = attrSection.getInt(s);
        attrReqs.put(attr, value);
      }
    }
    Map<LifeSkillType, Integer> skillReqs = new HashMap<>();
    ConfigurationSection skillSecion = iconSection.getConfigurationSection("skill-requirements");
    if (skillSecion != null) {
      for (String s : skillSecion.getKeys(false)) {
        LifeSkillType skill = LifeSkillType.valueOf(s);
        int value = skillSecion.getInt(s);
        skillReqs.put(skill, value);
      }
    }
    Map<LifeSkillType, Float> expWeight = new HashMap<>();
    ConfigurationSection weightSection = iconSection.getConfigurationSection("exp-weights");
    if (weightSection != null) {
      for (String s : weightSection.getKeys(false)) {
        LifeSkillType skill = LifeSkillType.valueOf(s);
        double value = weightSection.getDouble(s);
        expWeight.put(skill, (float) value);
      }
    }
    data.getAttributeRequirement().clear();
    data.getAttributeRequirement().putAll(attrReqs);
    data.getLifeSkillRequirements().clear();
    data.getLifeSkillRequirements().putAll(skillReqs);
    data.getExpWeights().clear();
    data.getExpWeights().putAll(expWeight);
    return data;
  }
}
