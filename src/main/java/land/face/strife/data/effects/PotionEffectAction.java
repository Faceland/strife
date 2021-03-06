package land.face.strife.data.effects;

import land.face.strife.data.StrifeMob;
import land.face.strife.stats.StrifeStat;
import land.face.strife.util.LogUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectAction extends Effect {

  private PotionEffectType potionEffectType;
  private double duration;
  private int intensity;
  private boolean remove;
  private boolean bumpUpToIntensity;
  private boolean strictDuration;

  @Override
  public void apply(StrifeMob caster, StrifeMob target) {
    if (remove) {
      target.getEntity().removePotionEffect(potionEffectType);
      return;
    }
    double duration = this.duration;
    if (!strictDuration) {
      duration *= 1 + caster.getStat(StrifeStat.EFFECT_DURATION) / 100;
    }
    if (bumpUpToIntensity) {
      bumpPotionEffect(target.getEntity(), duration);
      return;
    }
    target.getEntity().addPotionEffect(new PotionEffect(potionEffectType, (int) duration, intensity));
  }

  private void bumpPotionEffect(LivingEntity target, double duration) {
    LogUtil.printDebug(" Bumping potion effect");
    int level = 0;
    if (target.hasPotionEffect(potionEffectType)) {
      level = target.getPotionEffect(potionEffectType).getAmplifier();
      level = Math.min(level + 1, intensity);
      LogUtil.printDebug(" Bumped from " + level + " to " + (level + 1) + ". MAX: " + intensity);
    } else {
      LogUtil.printDebug(" Target missing potion effect - adding at intensity 0");
    }
    target.addPotionEffect(new PotionEffect(potionEffectType, (int) duration, level));
  }

  public void setPotionEffectType(PotionEffectType potionEffectType) {
    this.potionEffectType = potionEffectType;
  }

  public void setDuration(double duration) {
    this.duration = duration;
  }

  public void setIntensity(int intensity) {
    this.intensity = intensity;
  }

  public boolean isRemove() {
    return remove;
  }

  public void setRemove(boolean remove) {
    this.remove = remove;
  }

  public void setStrictDuration(boolean strictDuration) {
    this.strictDuration = strictDuration;
  }

  public void setBumpUpToIntensity(boolean bumpUpToIntensity) {
    this.bumpUpToIntensity = bumpUpToIntensity;
  }
}
