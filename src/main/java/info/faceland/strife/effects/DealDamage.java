package info.faceland.strife.effects;

import static info.faceland.strife.stats.StrifeStat.DAMAGE_MULT;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.data.StrifeMob;
import info.faceland.strife.stats.StrifeStat;
import info.faceland.strife.util.DamageUtil;
import info.faceland.strife.util.DamageUtil.AbilityMod;
import info.faceland.strife.util.DamageUtil.DamageType;
import info.faceland.strife.util.LogUtil;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

public class DealDamage extends Effect {

  private double amount;
  private double flatBonus;
  private DamageScale damageScale;
  private DamageType damageType;
  private final Map<AbilityMod, Double> abilityMods = new HashMap<>();
  private boolean canBeEvaded;
  private boolean canBeBlocked;

  private static double pvpMult = StrifePlugin.getInstance().getSettings()
      .getDouble("config.mechanics.pvp-damage", 0.50);

  @Override
  public void apply(StrifeMob caster, StrifeMob target) {
    double damage = amount;
    for (StrifeStat attr : getStatMults().keySet()) {
      damage *= 1 + (getStatMults().get(attr) * caster.getStat(attr));
    }
    LogUtil.printDebug("Damage Effect! " + damage + " | " + damageScale + " | " + damageType);
    switch (damageScale) {
      case FLAT:
        break;
      case CASTER_DAMAGE:
        damage *= DamageUtil.getRawDamage(caster, damageType);
        break;
      case TARGET_CURRENT_HEALTH:
        damage *= target.getEntity().getHealth() / target.getEntity().getMaxHealth();
        break;
      case CASTER_CURRENT_HEALTH:
        damage *= caster.getEntity().getHealth() / target.getEntity().getMaxHealth();
        break;
      case TARGET_MISSING_HEALTH:
        damage *= 1 - target.getEntity().getHealth() / target.getEntity().getMaxHealth();
        break;
      case CASTER_MISSING_HEALTH:
        damage *= 1 - caster.getEntity().getHealth() / caster.getEntity().getMaxHealth();
        break;
      case TARGET_MAX_HEALTH:
        damage *= target.getEntity().getMaxHealth();
        break;
      case CASTER_MAX_HEALTH:
        damage *= caster.getEntity().getMaxHealth();
        break;
    }
    damage += flatBonus;
    LogUtil.printDebug(" [Pre-Mitigation] Dealing " + damage + " of type " + damageType);
    damage *= DamageUtil.getDamageReduction(damageType, caster, target, abilityMods);
    if (damageType != DamageType.TRUE_DAMAGE) {
      damage *= DamageUtil.getPotionMult(caster.getEntity(), target.getEntity());
      damage *= 1 + (caster.getStat(DAMAGE_MULT) / 100);
    }
    if (caster != target && caster.getEntity() instanceof Player && target
        .getEntity() instanceof Player) {
      damage *= pvpMult;
    }
    LogUtil.printDebug(" [Post-Mitigation] Dealing " + damage + " of type " + damageType);
    LogUtil.printDebug(" [Pre-Damage] Target Health: " + target.getEntity().getHealth());
    DamageUtil.dealDirectDamage(caster, target, damage);
    LogUtil.printDebug(" [Post-Damage] Target Health: " + target.getEntity().getHealth());
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public void setFlatBonus(double flatBonus) {
    this.flatBonus = flatBonus;
  }

  public void setDamageScale(DamageScale damageScale) {
    this.damageScale = damageScale;
  }

  public void setDamageType(DamageType damageType) {
    this.damageType = damageType;
  }

  public Map<AbilityMod, Double> getAbilityMods() {
    return abilityMods;
  }

  public enum DamageScale {
    FLAT,
    CASTER_DAMAGE,
    TARGET_CURRENT_HEALTH,
    CASTER_CURRENT_HEALTH,
    TARGET_MISSING_HEALTH,
    CASTER_MISSING_HEALTH,
    TARGET_MAX_HEALTH,
    CASTER_MAX_HEALTH,
    TARGET_CURRENT_BARRIER,
    CASTER_CURRENT_BARRIER,
    TARGET_MISSING_BARRIER,
    CASTER_MISSING_BARRIER,
    TARGET_MAX_BARRIER,
    CASTER_MAX_BARRIER,
  }
}