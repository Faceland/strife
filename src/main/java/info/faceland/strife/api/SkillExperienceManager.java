/**
 * The MIT License Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package info.faceland.strife.api;

import static info.faceland.strife.attributes.StrifeAttribute.SKILL_XP_GAIN;

import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import info.faceland.strife.StrifePlugin;
import info.faceland.strife.data.champion.Champion;
import info.faceland.strife.data.champion.ChampionSaveData;
import info.faceland.strife.data.champion.ChampionSaveData.LifeSkillType;
import info.faceland.strife.events.SkillExpGainEvent;
import info.faceland.strife.events.SkillLevelUpEvent;
import java.text.DecimalFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SkillExperienceManager {

  private final StrifePlugin plugin;
  private static final DecimalFormat FORMAT = new DecimalFormat("###,###,###");
  private static final String XP_AB = "{0}( &f&l{1} {0}/ &f&l{2} XP {0})";

  public SkillExperienceManager(StrifePlugin plugin) {
    this.plugin = plugin;
  }

  public void addExperience(Player player, LifeSkillType type, double amount, boolean exact) {
    addExperience(plugin.getChampionManager().getChampion(player), type, amount, exact);
  }

  public void addExperience(Champion champion, LifeSkillType type, double amount, boolean exact) {
    ChampionSaveData saveData = champion.getSaveData();
    if (amount < 0.001) {
      return;
    }
    if (saveData.getSkillLevel(type) >= plugin.getMaxSkillLevel()) {
      return;
    }
    if (!exact) {
      double statsMult = champion.getCombinedCache().getOrDefault(SKILL_XP_GAIN, 0D) / 100;
      amount *= 1 + statsMult;
    }

    SkillExpGainEvent xpEvent = new SkillExpGainEvent(champion, type, (float) amount);
    StrifePlugin.getInstance().getServer().getPluginManager().callEvent(xpEvent);

    double currentExp = saveData.getSkillExp(type) + xpEvent.getAmount();
    double maxExp = (double) getMaxExp(type, saveData.getSkillLevel(type));

    while (currentExp > maxExp) {
      currentExp -= maxExp;
      saveData.setSkillLevel(type, saveData.getSkillLevel(type) + 1);

      SkillLevelUpEvent levelUpEvent = new SkillLevelUpEvent(champion.getPlayer(), type,
          saveData.getSkillLevel(type));
      Bukkit.getPluginManager().callEvent(levelUpEvent);

      if (saveData.getSkillLevel(type) >= plugin.getMaxSkillLevel()) {
        break;
      }
      maxExp = (double) getMaxExp(type, saveData.getSkillLevel(type));
    }

    saveData.setSkillExp(type, (float) currentExp);
    String c = getSkillColor(type);
    String xpMsg = XP_AB.replace("{0}", c).replace("{1}", FORMAT.format((int) currentExp))
        .replace("{2}", FORMAT.format((int) maxExp));
    MessageUtils.sendActionBar(champion.getPlayer(), xpMsg);
  }

  public Integer getMaxExp(LifeSkillType type, int level) {
    switch (type) {
      case CRAFTING:
        return plugin.getCraftingRate().get(level);
      case ENCHANTING:
        return plugin.getEnchantRate().get(level);
      case FISHING:
        return plugin.getFishRate().get(level);
      case MINING:
        return plugin.getMiningRate().get(level);
      case SNEAK:
        return plugin.getSneakRate().get(level);
    }
    return -1;
  }

  public String getSkillColor(LifeSkillType type) {
    switch (type) {
      case CRAFTING:
        return "&e";
      case ENCHANTING:
        return "&d";
      case FISHING:
        return "&b";
      case MINING:
        return "&2";
      case SNEAK:
        return "&7";
    }
    return "";
  }

  public String getPrettySkillName(LifeSkillType type) {
    switch (type) {
      case CRAFTING:
        return "Crafting";
      case ENCHANTING:
        return "Enchanting";
      case FISHING:
        return "Fishing";
      case MINING:
        return "Mining";
      case SNEAK:
        return "Sneak";
    }
    return "";
  }

}
