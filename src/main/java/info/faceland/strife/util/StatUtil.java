package info.faceland.strife.util;

import static info.faceland.strife.attributes.StrifeAttribute.*;
import static org.bukkit.potion.PotionEffectType.FAST_DIGGING;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.AttributedEntity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatUtil {

  private static final double BASE_ATTACK_SECONDS = 2.0D;
  private static final double BASE_EVASION_MULT = 0.8D;
  private static final double EVASION_ACCURACY_MULT = 0.6D;

  public static double getRegen(AttributedEntity ae) {
    return ae.getAttribute(REGENERATION) * (1 + ae.getAttribute(REGEN_MULT) / 100);
  }

  public static double getHealth(AttributedEntity ae) {
    return ae.getAttribute(HEALTH) * (1 + ae.getAttribute(HEALTH_MULT) / 100);
  }

  public static double getMaximumBarrier(AttributedEntity ae) {
    return ae.getAttribute(BARRIER);
  }

  public static double getBarrierPerSecond(AttributedEntity ae) {
    return (4 + (ae.getAttribute(BARRIER) * 0.08)) * (1 + (ae.getAttribute(BARRIER_SPEED) / 100));
  }

  public static double getDamageMult(AttributedEntity ae) {
    return 1 + ae.getAttribute(DAMAGE_MULT) / 100;
  }

  public static double getMeleeDamage(AttributedEntity ae) {
    return ae.getAttribute(MELEE_DAMAGE) * (1 + ae.getAttribute(MELEE_MULT) / 100);
  }

  public static double getRangedDamage(AttributedEntity ae) {
    return ae.getAttribute(RANGED_DAMAGE) * (1 + ae.getAttribute(RANGED_MULT) / 100);
  }

  public static double getMagicDamage(AttributedEntity ae) {
    return ae.getAttribute(MAGIC_DAMAGE) * (1 + ae.getAttribute(MAGIC_MULT) / 100);
  }

  public static double getBaseMeleeDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getMeleeDamage(attacker);
    return rawDamage * getArmorMult(attacker, defender);
  }

  public static double getBaseRangedDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getRangedDamage(attacker);
    return rawDamage * getArmorMult(attacker, defender);
  }

  public static double getBaseMagicDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getMagicDamage(attacker);
    return rawDamage * getWardingMult(attacker, defender);
  }

  public static double getBaseExplosionDamage(AttributedEntity attacker,
      AttributedEntity defender) {
    double rawDamage = getMagicDamage(attacker);
    return rawDamage * getArmorMult(attacker, defender);
  }

  public static double getAttackTime(AttributedEntity ae) {
    double attackTime = BASE_ATTACK_SECONDS;
    double attackBonus = ae.getAttribute(StrifeAttribute.ATTACK_SPEED);
    if (itemCanUseRage(ae.getEntity().getEquipment().getItemInMainHand())) {
      attackBonus += StrifePlugin.getInstance().getRageManager().getRage(ae.getEntity());
    }
    if (ae.getEntity().hasPotionEffect(FAST_DIGGING)) {
      attackBonus += 0.15 * (1 + ae.getEntity().getPotionEffect(FAST_DIGGING).getAmplifier());
    }
    if (attackBonus > 0) {
      attackTime /= 1 + attackBonus / 100;
    } else {
      attackTime *= 1 + Math.abs(attackBonus / 100);
    }
    return attackTime;
  }

  public static double getOverchargeMultiplier(AttributedEntity ae) {
    return 1 + (ae.getAttribute(OVERCHARGE) / 100);
  }

  public static double getCriticalMultiplier(AttributedEntity ae) {
    return 1 + (ae.getAttribute(CRITICAL_DAMAGE) / 100);
  }

  public static double getArmor(AttributedEntity ae) {
    return ae.getAttribute(ARMOR) * (1 + ae.getAttribute(ARMOR_MULT) / 100);
  }

  public static double getWarding(AttributedEntity ae) {
    return ae.getAttribute(WARDING) * (1 + ae.getAttribute(WARD_MULT) / 100);
  }

  public static double getMinimumEvasionMult(AttributedEntity ae) {
    return getFlatEvasion(ae) * (1 + ae.getAttribute(EVASION_MULT) / 100);
  }

  public static double getFlatEvasion(AttributedEntity ae) {
    return ae.getAttribute(EVASION);
  }

  public static double getArmorPen(AttributedEntity ae) {
    return ae.getAttribute(ARMOR_PENETRATION) * (1 + (ae.getAttribute(APEN_MULT) / 100));
  }

  public static double getWardPen(AttributedEntity ae) {
    return ae.getAttribute(WARD_PENETRATION) * (1 + (ae.getAttribute(WPEN_MULT) / 100));
  }

  public static double getAccuracy(AttributedEntity ae) {
    return ae.getAttribute(ACCURACY) * (1 + (ae.getAttribute(ACCURACY_MULT) / 100));
  }

  public static double getArmorMult(AttributedEntity attacker, AttributedEntity defender) {
    double adjustedArmor = Math.max(getArmor(defender) - getArmorPen(attacker), 1);
    return Math.min(1, 80 / (80 + adjustedArmor));
  }

  public static double getWardingMult(AttributedEntity attacker, AttributedEntity defender) {
    double adjustedWarding = Math.max(getWarding(defender) - getWardPen(attacker), 1);
    return Math.min(1, 80 / (80 + adjustedWarding));
  }

  public static double getMinimumEvasionMult(AttributedEntity attacker, AttributedEntity defender) {
    double evasion = getMinimumEvasionMult(defender);
    double accuracy = getAccuracy(attacker);
    double bonusMultiplier = EVASION_ACCURACY_MULT * ((evasion - accuracy) / (1 + accuracy));
    return Math.min(1, BASE_EVASION_MULT - bonusMultiplier);
  }

  public static double getFireResist(AttributedEntity ae) {
    double amount = ae.getAttribute(FIRE_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getIceResist(AttributedEntity ae) {
    double amount = ae.getAttribute(ICE_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getLightningResist(AttributedEntity ae) {
    double amount = ae.getAttribute(LIGHTNING_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getEarthResist(AttributedEntity ae) {
    double amount = ae.getAttribute(EARTH_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getLightResist(AttributedEntity ae) {
    double amount = ae.getAttribute(LIGHT_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getShadowResist(AttributedEntity ae) {
    double amount = ae.getAttribute(DARK_RESIST) + ae.getAttribute(ALL_RESIST);
    return Math.min(amount, ae.getEntity() instanceof Player ? 80 : 99);
  }

  public static double getLifestealPercentage(AttributedEntity attacker) {
    return attacker.getAttribute(LIFE_STEAL) / 100;
  }

  public static double getFireDamage(AttributedEntity attacker) {
    return attacker.getAttribute(FIRE_DAMAGE) * getElementalMult(attacker);
  }

  public static double getIceDamage(AttributedEntity attacker) {
    return attacker.getAttribute(ICE_DAMAGE) * getElementalMult(attacker);
  }

  public static double getLightningDamage(AttributedEntity attacker) {
    return attacker.getAttribute(LIGHTNING_DAMAGE) * getElementalMult(attacker);
  }

  public static double getEarthDamage(AttributedEntity attacker) {
    return attacker.getAttribute(EARTH_DAMAGE) * getElementalMult(attacker);
  }

  public static double getLightDamage(AttributedEntity attacker) {
    return attacker.getAttribute(LIGHT_DAMAGE) * getElementalMult(attacker);
  }

  public static double getShadowDamage(AttributedEntity attacker) {
    return attacker.getAttribute(DARK_DAMAGE) * getElementalMult(attacker);
  }

  public static double getBaseFireDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.FIRE_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getFireResist(defender) / 100;
    return damage;
  }

  public static double getBaseIceDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.ICE_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getIceResist(defender) / 100;
    return damage;
  }

  public static double getBaseLightningDamage(AttributedEntity attacker,
      AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.LIGHTNING_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getLightningResist(defender) / 100;
    return damage;
  }

  public static double getBaseEarthDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.EARTH_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getEarthResist(defender) / 100;
    return damage;
  }

  public static double getBaseLightDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.LIGHT_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getLightResist(defender) / 100;
    return damage;
  }

  public static double getBaseShadowDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.DARK_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getShadowResist(defender) / 100;
    return damage;
  }

  private static double getElementalMult(AttributedEntity pStats) {
    return 1 + (pStats.getAttribute(ELEMENTAL_MULT) / 100);
  }

  private static boolean itemCanUseRage(ItemStack item) {
    if (item.getType() == Material.BOW) {
      return false;
    }
    if (!ItemUtil.isMeleeWeapon(item.getType())) {
      return false;
    }
    if (ItemUtil.isWand(item)) {
      return false;
    }
    return true;
  }
}