package land.face.strife.data.effects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import land.face.strife.data.StrifeMob;
import land.face.strife.data.conditions.Condition;
import land.face.strife.stats.StrifeStat;
import land.face.strife.timers.EndlessEffectTimer;

public class EndlessEffect extends Effect {

  private final List<Effect> runEffects = new ArrayList<>();
  private final List<Effect> expiryEffects = new ArrayList<>();
  private final List<Effect> cancelEffects = new ArrayList<>();
  private final Set<Condition> cancelConditions = new HashSet<>();
  private StrifeStat reducerStat;
  private float reducerValue;
  private int tickRate;
  private float maxDuration;
  private boolean strictDuration;

  private final Map<StrifeMob, EndlessEffectTimer> runningEffects = new ConcurrentHashMap<>();

  @Override
  public void apply(StrifeMob caster, StrifeMob target) {
    int newTickRate = tickRate;
    if (reducerStat != null) {
      float tickDivisor = 1 + (caster.getStat(reducerStat) / reducerValue);
      newTickRate = Math.max(1, (int) ((float) tickRate / tickDivisor));
    }
    float newDuration = maxDuration;
    if (!strictDuration) {
      newDuration *= 1 + caster.getStat(StrifeStat.EFFECT_DURATION) / 100;
    }
    newDuration = (newDuration * 20) / newTickRate;
    EndlessEffectTimer timer = new EndlessEffectTimer(this, target, newTickRate, (int) newDuration);
    runningEffects.put(target, timer);
  }

  public EndlessEffectTimer getEndlessTimer(StrifeMob target) {
    return runningEffects.getOrDefault(target, null);
  }

  public void removeEffectOnTarget(StrifeMob target) {
    if (runningEffects.containsKey(target)) {
      if (!runningEffects.get(target).isCancelled()) {
        runningEffects.get(target).cancel();
      }
      runningEffects.remove(target);
    }
  }

  public Set<Condition> getCancelConditions() {
    return cancelConditions;
  }

  public void setReducerStat(StrifeStat reducerStat) {
    this.reducerStat = reducerStat;
  }

  public void setReducerValue(float reducerValue) {
    this.reducerValue = reducerValue;
  }

  public List<Effect> getRunEffects() {
    return runEffects;
  }

  public List<Effect> getExpiryEffects() {
    return expiryEffects;
  }

  public List<Effect> getCancelEffects() {
    return cancelEffects;
  }

  public void setMaxDuration(float maxDuration) {
    this.maxDuration = maxDuration;
  }

  public void setTickRate(int tickRate) {
    this.tickRate = tickRate;
  }

  public void setStrictDuration(boolean strictDuration) {
    this.strictDuration = strictDuration;
  }
}