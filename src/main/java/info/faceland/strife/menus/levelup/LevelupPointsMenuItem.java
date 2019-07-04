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
package info.faceland.strife.menus.levelup;

import com.tealcube.minecraft.bukkit.TextUtils;
import info.faceland.strife.StrifePlugin;
import info.faceland.strife.data.champion.Champion;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.List;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LevelupPointsMenuItem extends MenuItem {

  private static final String DISPLAY_NAME = "&f&nUnused Levelpoints";
  private static final ItemStack DISPLAY_ICON = new ItemStack(Material.NETHER_STAR);
  private static final String[] DISPLAY_LORE = {
      ChatColor.GRAY + "Click an attribute to upgrade!"
  };
  private static final String CLICK_TO_SAVE_TEXT = TextUtils.color("&e&lClick to apply changes!");

  private final StrifePlugin plugin;

  LevelupPointsMenuItem(StrifePlugin plugin) {
    super(TextUtils.color(DISPLAY_NAME), DISPLAY_ICON, DISPLAY_LORE);
    this.plugin = plugin;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    ItemStack itemStack = super.getFinalIcon(player);
    List<String> lore = ItemStackExtensionsKt.getLore(itemStack);

    Champion champion = plugin.getChampionManager().getChampion(player);
    int stacks = champion.getPendingUnusedStatPoints();
    String name = TextUtils.color("&f&nUnused Levelpoints (" + stacks + ")");

    if (champion.getPendingLevelMap().size() > 0) {
      lore.add(CLICK_TO_SAVE_TEXT);
    }

    ItemStackExtensionsKt.setDisplayName(itemStack, name);
    ItemStackExtensionsKt.setLore(itemStack, lore);

    stacks = Math.min(stacks, 64);
    itemStack.setAmount(Math.max(1, stacks));
    return itemStack;
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(true);
    if (plugin.getChampionManager().hasPendingChanges(event.getPlayer())) {
      Bukkit.getScheduler().runTaskLater(plugin, () ->
          plugin.getConfirmationMenu().open(event.getPlayer()), 1L);
    }
  }

}
