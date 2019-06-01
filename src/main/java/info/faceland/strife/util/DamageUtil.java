package info.faceland.strife.util;

import static info.faceland.strife.attributes.StrifeAttribute.BLEED_CHANCE;
import static info.faceland.strife.attributes.StrifeAttribute.BLEED_DAMAGE;
import static info.faceland.strife.attributes.StrifeAttribute.BLEED_RESIST;
import static info.faceland.strife.attributes.StrifeAttribute.HP_ON_HIT;
import static info.faceland.strife.attributes.StrifeAttribute.PROJECTILE_DAMAGE;
import static info.faceland.strife.attributes.StrifeAttribute.PROJECTILE_REDUCTION;
import static info.faceland.strife.attributes.StrifeAttribute.TENACITY;
import static info.faceland.strife.util.StatUtil.getArmorMult;
import static info.faceland.strife.util.StatUtil.getFireResist;
import static info.faceland.strife.util.StatUtil.getIceResist;
import static info.faceland.strife.util.StatUtil.getLightningResist;
import static info.faceland.strife.util.StatUtil.getShadowResist;
import static info.faceland.strife.util.StatUtil.getWardingMult;

import com.tealcube.minecraft.bukkit.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import info.faceland.strife.StrifePlugin;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.AttributedEntity;
import info.faceland.strife.events.BlockEvent;
import info.faceland.strife.events.CriticalEvent;
import info.faceland.strife.events.EvadeEvent;
import info.faceland.strife.managers.BlockManager;
import info.faceland.strife.managers.DarknessManager;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DamageUtil {

  private static final String ATTACK_MISSED = TextUtils.color("&f&lMiss!");
  private static final String ATTACK_BLOCKED = TextUtils.color("&f&lBlocked!");
  private static final String ATTACK_DODGED = TextUtils.color("&f&lDodge!");
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  private static final DamageModifier[] MODIFIERS = EntityDamageEvent.DamageModifier.values();

  private static final double BLEED_PERCENT = 0.5;

  public static double dealDirectDamage(AttributedEntity attacker, AttributedEntity defender,
      double damage, DamageType damageType) {
    LogUtil.printDebug("[Pre-Mitigation] Dealing " + damage + " of type " + damageType);
    switch (damageType) {
      case PHYSICAL:
        damage *= getArmorMult(attacker, defender);
        break;
      case MAGICAL:
        damage *= getWardingMult(attacker, defender);
        break;
      case FIRE:
        damage *= 1 - getFireResist(defender) / 100;
        break;
      case ICE:
        damage *= 1 - getIceResist(defender) / 100;
        break;
      case LIGHTNING:
        damage *= 1 - getLightningResist(defender) / 100;
        break;
      case DARK:
        damage *= 1 - getShadowResist(defender) / 100;
        break;
    }
    damage = StrifePlugin.getInstance().getBarrierManager().damageBarrier(defender, damage);
    defender.getEntity().damage(damage);
    LogUtil.printDebug("[Post-Mitigation] Dealing " + damage + " of type " + damageType);
    return damage;
  }

  public static double attemptIgnite(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0 || rollDouble() >= attacker.getAttribute(StrifeAttribute.IGNITE_CHANCE) / 100) {
      return 0D;
    }
    double bonusDamage = defender.getFireTicks() > 0 ? damage : 1D;
    defender.setFireTicks(Math.max(60 + (int) damage, defender.getFireTicks()));
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1f);
    defender.getWorld()
        .spawnParticle(Particle.FLAME, defender.getEyeLocation(), 6 + (int) damage / 2,
            0.3, 0.3, 0.3, 0.03);
    return bonusDamage;
  }

  public static double attemptShock(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0 || rollDouble() >= attacker.getAttribute(StrifeAttribute.SHOCK_CHANCE) / 100) {
      return 0D;
    }
    double multiplier = 0.5;
    double percentHealth =
        defender.getHealth() / defender.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    if (percentHealth < 0.5) {
      multiplier = 1 / Math.max(0.16, percentHealth * 2);
    }
    double particles = damage * multiplier * 0.5;
    double particleRange = 0.8 + multiplier * 0.2;
    defender.getWorld()
        .playSound(defender.getEyeLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 2f);
    defender.getWorld()
        .spawnParticle(Particle.CRIT_MAGIC, defender.getEyeLocation(), 10 + (int) particles,
            particleRange, particleRange, particleRange, 0.12);
    if (defender instanceof Creeper) {
      ((Creeper) defender).setPowered(true);
    }
    return damage * multiplier;
  }

  public static double attemptFreeze(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0 || rollDouble() >= attacker.getAttribute(StrifeAttribute.FREEZE_CHANCE) / 100) {
      return 0D;
    }
    double multiplier = 0.25 + 0.25 * (StatUtil.getHealth(attacker) / 100);
    if (!defender.hasPotionEffect(PotionEffectType.SLOW)) {
      defender.getActivePotionEffects().add(new PotionEffect(PotionEffectType.SLOW, 30, 1));
    }
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.0f);
    defender.getWorld()
        .spawnParticle(Particle.SNOWBALL, defender.getEyeLocation(), 4 + (int) damage / 2,
            0.3, 0.3, 0.2, 0.0);
    return damage * multiplier;
  }

  public static double consumeEarthRunes(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0) {
      return 0;
    }
    int runes = getBlockManager().getEarthRunes(attacker.getEntity().getUniqueId());
    getBlockManager().setEarthRunes(attacker.getEntity().getUniqueId(), 0);
    if (runes == 0) {
      return 0;
    }
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
    defender.getWorld().spawnParticle(
        Particle.BLOCK_CRACK,
        defender.getEyeLocation().clone().add(0, -0.7, 0),
        20,
        0.0, 0.0, 0.0,
        new MaterialData(Material.DIRT)
    );
    return damage * 0.5 * runes;
  }

  public static double getLightBonus(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0) {
      return 0;
    }
    double light = attacker.getEntity().getLocation().getBlock().getLightLevel();
    double multiplier = (light - 4) / 10;
    if (multiplier >= 0.5) {
      defender.getWorld()
          .playSound(defender.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
      defender.getWorld().spawnParticle(
          Particle.FIREWORKS_SPARK,
          defender.getEyeLocation(),
          (int) (20 * multiplier),
          0.1, 0.1, 0.1,
          0.1
      );
    }
    return damage * multiplier;
  }

  public static boolean attemptCorrupt(double damage, AttributedEntity attacker,
      LivingEntity defender) {
    if (damage == 0
        || rollDouble() >= attacker.getAttribute(StrifeAttribute.CORRUPT_CHANCE) / 100) {
      return false;
    }
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7f, 2f);
    defender.getWorld()
        .spawnParticle(Particle.SMOKE_NORMAL, defender.getEyeLocation(), 10, 0.4, 0.4, 0.5, 0.1);
    getDarknessManager().applyCorruptionStacks(defender, damage);
    return true;
  }

  public static void doEvasion(LivingEntity attacker, LivingEntity defender) {
    callEvadeEvent(defender, attacker);
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 2f);
    if (defender instanceof Player) {
      MessageUtils.sendActionBar((Player) defender, ATTACK_DODGED);
    }
    if (attacker instanceof Player) {
      MessageUtils.sendActionBar((Player) attacker, ATTACK_MISSED);
    }
  }

  public static void doBlock(LivingEntity attacker, LivingEntity defender) {
    callBlockEvent(defender, attacker);
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
    String defenderBar = ATTACK_BLOCKED;
    int runes = getBlockManager().getEarthRunes(defender.getUniqueId());
    if (runes > 0) {
      StringBuilder sb = new StringBuilder(defenderBar);
      sb.append(TextUtils.color("&2 "));
      sb.append(IntStream.range(0, runes).mapToObj(i -> "▼").collect(Collectors.joining("")));
      defenderBar = sb.toString();
    }
    if (defender instanceof Player) {
      MessageUtils.sendActionBar((Player) defender, defenderBar);
    }
    if (attacker instanceof Player) {
      MessageUtils.sendActionBar((Player) attacker, ATTACK_BLOCKED);
    }
  }

  public static double getTenacityMult(AttributedEntity defender) {
    if (defender.getAttribute(TENACITY) < 1) {
      return 1.0D;
    }
    double percent = defender.getEntity().getHealth() / defender.getEntity().getMaxHealth();
    double maxReduction = 1 - Math.pow(0.5, defender.getAttribute(TENACITY) / 200);
    return 1 - (maxReduction * Math.pow(1 - percent, 1.5));
  }

  public static double getPotionMult(LivingEntity attacker, LivingEntity defender) {
    double potionMult = 1.0;
    Collection<PotionEffect> attackerEffects = attacker.getActivePotionEffects();
    Collection<PotionEffect> defenderEffects = defender.getActivePotionEffects();
    for (PotionEffect effect : attackerEffects) {
      if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
        potionMult += 0.1 * (effect.getAmplifier() + 1);
        continue;
      }
      if (effect.getType().equals(PotionEffectType.WEAKNESS)) {
        potionMult -= 0.1 * (effect.getAmplifier() + 1);
        continue;
      }
    }

    for (PotionEffect effect : defenderEffects) {
      if (effect.getType().equals(PotionEffectType.WITHER)) {
        potionMult += 0.15 * (effect.getAmplifier() + 1);
        continue;
      }
      if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
        potionMult -= 0.1 * (effect.getAmplifier() + 1);
        continue;
      }
    }
    return Math.max(0, potionMult);
  }

  public static double getResistPotionMult(LivingEntity defender) {
    double mult = 1.0;
    Collection<PotionEffect> defenderEffects = defender.getActivePotionEffects();
    for (PotionEffect effect : defenderEffects) {
      if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
        mult -= 0.1 * (effect.getAmplifier() + 1);
        continue;
      }
    }
    return mult;
  }

  public static double getProjectileMultiplier(AttributedEntity atk, AttributedEntity def) {
    return Math.max(0.05D,
        1 + (atk.getAttribute(PROJECTILE_DAMAGE) - def.getAttribute(PROJECTILE_REDUCTION)) / 100);
  }

  public static void applyLifeSteal(AttributedEntity attacker, double damage,
      double healMultiplier) {
    double lifeSteal = StatUtil.getLifestealPercentage(attacker);
    if (lifeSteal <= 0 || attacker.getEntity().getHealth() <= 0 || attacker.getEntity().isDead()) {
      return;
    }
    double lifeStolen = damage * lifeSteal;
    if (attacker instanceof Player) {
      lifeStolen *= Math.min(((Player) attacker).getFoodLevel() / 7.0D, 1.0D);
    }
    if (attacker.getEntity().hasPotionEffect(PotionEffectType.POISON)) {
      lifeStolen *= 0.3;
    }
    restoreHealth(attacker.getEntity(), lifeStolen * healMultiplier);
  }

  public static void applyHealthOnHit(AttributedEntity attacker, double attackMultiplier,
      double healMultiplier) {
    double health = attacker.getAttribute(HP_ON_HIT) * attackMultiplier;
    if (health <= 0 || attacker.getEntity().getHealth() <= 0 || attacker.getEntity().isDead()) {
      return;
    }
    if (attacker instanceof Player) {
      health *= Math.min(((Player) attacker).getFoodLevel() / 7.0D, 1.0D);
    }
    if (attacker.getEntity().hasPotionEffect(PotionEffectType.POISON)) {
      health *= 0.3;
    }
    restoreHealth(attacker.getEntity(), health * healMultiplier);
  }

  public static boolean attemptBleed(AttributedEntity attacker, AttributedEntity defender,
      double damage, double critMult, double attackMult) {
    if (StrifePlugin.getInstance().getBarrierManager().isBarrierUp(defender)) {
      return false;
    }
    if (defender.getAttribute(BLEED_RESIST) > 99) {
      return false;
    }
    if (attackMult * (attacker.getAttribute(BLEED_CHANCE) / 100) >= rollDouble()) {
      double amount = damage + damage * critMult;
      amount *= 1 + attacker.getAttribute(BLEED_DAMAGE) / 100;
      amount *= 1 - defender.getAttribute(BLEED_RESIST) / 100;
      amount *= BLEED_PERCENT;
      applyBleed(defender.getEntity(), amount);
      return true;
    }
    return false;
  }

  public static void applyBleed(LivingEntity defender, double amount) {
    StrifePlugin.getInstance().getBleedManager()
        .applyBleed(defender, amount);
    defender.getWorld()
        .playSound(defender.getEyeLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
  }

  public static void callCritEvent(LivingEntity attacker, LivingEntity victim) {
    CriticalEvent c = new CriticalEvent(attacker, victim);
    Bukkit.getPluginManager().callEvent(c);
  }

  public static void callEvadeEvent(LivingEntity evader, LivingEntity attacker) {
    EvadeEvent ev = new EvadeEvent(evader, attacker);
    Bukkit.getPluginManager().callEvent(ev);
  }

  public static void callBlockEvent(LivingEntity evader, LivingEntity attacker) {
    BlockEvent ev = new BlockEvent(evader, attacker);
    Bukkit.getPluginManager().callEvent(ev);
  }

  public static boolean hasLuck(LivingEntity entity) {
    return entity.hasPotionEffect(PotionEffectType.LUCK);
  }

  public static void restoreHealth(LivingEntity livingEntity, double amount) {
    livingEntity.setHealth(Math.min(livingEntity.getHealth() + amount,
        livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
  }

  public static void restoreBarrier(AttributedEntity attributedEntity, double amount) {
    StrifePlugin.getInstance().getBarrierManager().restoreBarrier(attributedEntity, amount);
  }

  public static void applyPotionEffect(LivingEntity entity, PotionEffectType type, int power,
      int duration) {
    if (entity == null || !entity.isValid()) {
      return;
    }
    Collection<PotionEffect> effects = entity.getActivePotionEffects();
    for (PotionEffect effect : effects) {
      if (type != effect.getType()) {
        continue;
      }
      if (power < effect.getAmplifier()) {
        return;
      }
      if (power == Math.abs(effect.getAmplifier()) && duration < effect.getDuration()) {
        return;
      }
      break;
    }
    entity.removePotionEffect(type);
    entity.addPotionEffect(new PotionEffect(type, duration, power));
  }

  public static AttackType getAttackType(EntityDamageByEntityEvent event) {
    if (event.getCause() == DamageCause.ENTITY_EXPLOSION) {
      return AttackType.EXPLOSION;
    } else if (event.getDamager() instanceof ShulkerBullet || event
        .getDamager() instanceof SmallFireball || event.getDamager() instanceof WitherSkull || event
        .getDamager() instanceof EvokerFangs) {
      return AttackType.MAGIC;
    } else if (event.getDamager() instanceof Projectile) {
      return AttackType.RANGED;
    }
    return AttackType.MELEE;
  }

  public static void removeDamageModifiers(EntityDamageEvent event) {
    for (DamageModifier modifier : MODIFIERS) {
      if (event.isApplicable(modifier)) {
        event.setDamage(modifier, 0D);
      }
    }
  }

  public static double rollDouble(boolean lucky) {
    return lucky ? Math.max(rollDouble(), rollDouble()) : rollDouble();
  }

  public static double rollDouble() {
    return RANDOM.nextDouble();
  }

  public static boolean rollBool(double chance, boolean lucky) {
    return lucky ? rollBool(chance) || rollBool(chance) : rollBool(chance);
  }

  public static boolean rollBool(double chance) {
    return RANDOM.nextDouble() <= chance;
  }

  private static BlockManager getBlockManager() {
    return StrifePlugin.getInstance().getBlockManager();
  }

  private static DarknessManager getDarknessManager() {
    return StrifePlugin.getInstance().getDarknessManager();
  }

  public enum DamageType {
    TRUE_DAMAGE,
    PHYSICAL,
    MAGICAL,
    FIRE,
    ICE,
    LIGHTNING,
    DARK
  }

  public enum AttackType {
    MELEE, RANGED, MAGIC, EXPLOSION, OTHER
  }
}