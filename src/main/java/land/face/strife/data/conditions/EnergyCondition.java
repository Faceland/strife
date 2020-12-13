package land.face.strife.data.conditions;

import land.face.strife.data.StrifeMob;
import land.face.strife.util.PlayerDataUtil;
import land.face.strife.util.StatUtil;
import org.bukkit.entity.Player;

public class EnergyCondition extends Condition {

  private final boolean percentage;

  public EnergyCondition(boolean percentage) {
    this.percentage = percentage;
  }

  public boolean isMet(StrifeMob attacker, StrifeMob target) {
    StrifeMob trueTarget = getCompareTarget() == CompareTarget.SELF ? attacker : target;
    if (trueTarget == null || !(trueTarget.getEntity() instanceof Player)) {
      return false;
    }
    if (percentage) {
      float energy = trueTarget.getEnergy() / StatUtil.getMaximumEnergy(trueTarget);
      return PlayerDataUtil.conditionCompare(getComparison(), energy, getValue());
    } else {
      float energy = target.getEnergy();
      return PlayerDataUtil.conditionCompare(getComparison(), energy, getValue());
    }
  }
}
