package info.faceland.strife.data;

public class AbilityCooldownContainer {

  private final String abilityId;
  private long startTime;
  private long endTime;
  private int spentCharges;

  public AbilityCooldownContainer(String abilityId, long endTime) {
    this.abilityId = abilityId;
    this.endTime = endTime;
    startTime = System.currentTimeMillis();
    spentCharges = 0;
  }

  public String getAbilityId() {
    return abilityId;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public int getSpentCharges() {
    return spentCharges;
  }

  public void setSpentCharges(int spentCharges) {
    this.spentCharges = spentCharges;
  }
}
