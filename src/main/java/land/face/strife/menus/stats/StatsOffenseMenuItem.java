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
package land.face.strife.menus.stats;

import static land.face.strife.menus.stats.StatsMenu.INT_FORMAT;
import static land.face.strife.menus.stats.StatsMenu.TWO_DECIMAL;
import static land.face.strife.menus.stats.StatsMenu.breakLine;

import com.tealcube.minecraft.bukkit.TextUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import land.face.strife.StrifePlugin;
import land.face.strife.data.StrifeMob;
import land.face.strife.stats.StrifeStat;
import land.face.strife.util.ItemUtil;
import land.face.strife.util.StatUtil;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StatsOffenseMenuItem extends MenuItem {

  StatsOffenseMenuItem() {
    super(TextUtils.color("&c&lOffensive Stats"), new ItemStack(Material.IRON_SWORD));
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    StrifeMob pStats = StrifePlugin.getInstance().getStrifeMobManager().getStatMob(player);
    Map<StrifeStat, Float> bases = StrifePlugin.getInstance().getMonsterManager()
        .getBaseStats(player, player.getLevel());

    Material material;
    double physicalDamage;
    double magicalDamage = StatUtil.getMagicDamage(pStats);
    if (player.getEquipment().getItemInMainHand().getType() == Material.BOW) {
      material = Material.BOW;
      physicalDamage = StatUtil.getRangedDamage(pStats);
    } else if (ItemUtil.isWandOrStaff(player.getEquipment().getItemInMainHand())) {
      material = Material.BLAZE_ROD;
      physicalDamage = pStats.getStat(StrifeStat.PHYSICAL_DAMAGE);
    } else {
      material = Material.IRON_SWORD;
      physicalDamage = StatUtil.getMeleeDamage(pStats);
    }
    ItemStack itemStack = new ItemStack(material);
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(getDisplayName());
    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    List<String> lore = new ArrayList<>();

    lore.add(breakLine);
    if (material == Material.BLAZE_ROD) {
      lore.add(addStat("Magical Damage: ", magicalDamage, INT_FORMAT));
      if (physicalDamage > 3.1) {
        lore.add(addStat("Physical Damage: ", physicalDamage, INT_FORMAT));
      }
    } else {
      lore.add(addStat("Physical Damage: ", physicalDamage, INT_FORMAT));
      if (Math.round(magicalDamage) > 0) {
        lore.add(addStat("Magical Damage: ", magicalDamage, INT_FORMAT));
      }
    }
    double acc = 100 + pStats.getStat(StrifeStat.ACCURACY) - bases.getOrDefault(StrifeStat.ACCURACY, 0f);
    lore.add(addStat("Accuracy Rating: ", acc, INT_FORMAT));
    lore.add(addStat("Attack Speed: ", StatUtil.getAttackTime(pStats), "s", TWO_DECIMAL));
    lore.add(breakLine);
    lore.add(addStat("Overcharge Multiplier: ", StatUtil.getOverchargeMultiplier(pStats), "x",
        TWO_DECIMAL));
    if (pStats.getStat(StrifeStat.MULTISHOT) > 0) {
      if (pStats.getStat(StrifeStat.DOGE) > 0) {
        lore.add(addStat("MultiTHOT: ", pStats.getStat(StrifeStat.MULTISHOT), "%", INT_FORMAT));
      } else {
        lore.add(addStat("Multishot: ", pStats.getStat(StrifeStat.MULTISHOT), "%", INT_FORMAT));
      }
    }
    lore.add(addStat("Critical Rate: ", pStats.getStat(StrifeStat.CRITICAL_RATE), "%", INT_FORMAT));
    lore.add(
        addStat("Critical Multiplier: ", StatUtil.getCriticalMultiplier(pStats), "x", TWO_DECIMAL));
    double aPen =
        pStats.getStat(StrifeStat.ARMOR_PENETRATION) - bases.getOrDefault(
            StrifeStat.ARMOR_PENETRATION, 0f);
    if (aPen != 0) {
      lore.add(addStat("Armor Penetration: " + ChatColor.WHITE + plus(aPen), aPen, INT_FORMAT));
    }
    double wPen = pStats.getStat(StrifeStat.WARD_PENETRATION) - bases.getOrDefault(
        StrifeStat.WARD_PENETRATION, 0f);
    if (wPen != 0) {
      lore.add(addStat("Ward Penetration: " + ChatColor.WHITE + plus(wPen), wPen, INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.BLEED_CHANCE) > 0) {
      lore.add(addStat("Bleed Chance: ", pStats.getStat(StrifeStat.BLEED_CHANCE), "%", INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.BLEED_DAMAGE) > 0) {
      lore.add(addStat("Bleed Damage: " + ChatColor.WHITE + "+",
          pStats.getStat(StrifeStat.BLEED_DAMAGE), "%", INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.MAXIMUM_RAGE) > 0 &&
        pStats.getStat(StrifeStat.RAGE_ON_HIT) > 0 ||
        pStats.getStat(StrifeStat.RAGE_ON_KILL) > 0) {
      lore.add(breakLine);
      lore.add(
          addStat("Maximum Rage: ", pStats.getStat(StrifeStat.MAXIMUM_RAGE), INT_FORMAT));
      lore.add(
          addStat("Rage On Hit: ", pStats.getStat(StrifeStat.RAGE_ON_HIT), INT_FORMAT));
      lore.add(
          addStat("Rage On Kill: ", pStats.getStat(StrifeStat.RAGE_ON_KILL), INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.HP_ON_HIT) > 0 || pStats.getStat(StrifeStat.LIFE_STEAL) > 0
        || pStats.getStat(StrifeStat.HP_ON_KILL) > 0) {
      lore.add(breakLine);
      if (pStats.getStat(StrifeStat.LIFE_STEAL) > 0) {
        lore.add(addStat("Life Steal: ", pStats.getStat(StrifeStat.LIFE_STEAL), "%", INT_FORMAT));
      }
      if (pStats.getStat(StrifeStat.HP_ON_HIT) > 0) {
        lore.add(addStat("Health On Hit: ", pStats.getStat(StrifeStat.HP_ON_HIT), INT_FORMAT));
      }
      if (pStats.getStat(StrifeStat.HP_ON_KILL) > 0) {
        lore.add(addStat("Health On Kill: ", pStats.getStat(StrifeStat.HP_ON_KILL), INT_FORMAT));
      }
    }
    lore.add(breakLine);
    lore.add(addStat("Fire Damage: ", StatUtil.getFireDamage(pStats), INT_FORMAT));
    lore.add(addStat("Ignite Chance: ", pStats.getStat(StrifeStat.IGNITE_CHANCE), "%", INT_FORMAT));
    if (pStats.getStat(StrifeStat.ICE_DAMAGE) > 0) {
      lore.add(addStat("Ice Damage: ", StatUtil.getIceDamage(pStats), INT_FORMAT));
      lore.add(addStat("Freeze Chance: ", pStats.getStat(StrifeStat.FREEZE_CHANCE), "%", INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.LIGHTNING_DAMAGE) > 0) {
      lore.add(addStat("Lightning Damage: ", StatUtil.getLightningDamage(pStats), INT_FORMAT));
      lore.add(addStat("Shock Chance: ", pStats.getStat(StrifeStat.SHOCK_CHANCE), "%", INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.EARTH_DAMAGE) > 0) {
      lore.add(addStat("Earth Damage: ", StatUtil.getEarthDamage(pStats), INT_FORMAT));
      lore.add(addStat("Maximum Earth Runes: ", pStats.getStat(StrifeStat.MAX_EARTH_RUNES), INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.LIGHT_DAMAGE) > 0) {
      lore.add(addStat("Light Damage: ", StatUtil.getLightDamage(pStats), INT_FORMAT));
    }
    if (pStats.getStat(StrifeStat.DARK_DAMAGE) > 0) {
      lore.add(addStat("Shadow Damage: ", StatUtil.getShadowDamage(pStats), INT_FORMAT));
      lore.add(addStat("Corrupt Chance: ", pStats.getStat(StrifeStat.CORRUPT_CHANCE), "%", INT_FORMAT));
    }
    lore.add(breakLine);
    lore.add(TextUtils.color("&8&oUse &7&o/help stats &8&ofor info!"));
    itemMeta.setLore(lore);
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
  }

  private String addStat(String name, double value, DecimalFormat format) {
    return ChatColor.RED + name + ChatColor.WHITE + format.format(value);
  }

  private String addStat(String name, double value, String extra, DecimalFormat format) {
    return ChatColor.RED + name + ChatColor.WHITE + format.format(value) + extra;
  }

  private String plus(double num) {
    return num >= 0 ? "+" : "";
  }
}