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
package land.face.strife.commands;

import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import land.face.strife.StrifePlugin;
import org.bukkit.entity.Player;
import se.ranzdo.bukkit.methodcommand.Arg;
import se.ranzdo.bukkit.methodcommand.Command;

public class InspectCommand {

  private final StrifePlugin plugin;

  public InspectCommand(StrifePlugin plugin) {
    this.plugin = plugin;
  }

  @Command(identifier = "stats", permissions = "strife.command.stats")
  public void baseCommand(Player sender) {
    plugin.getChampionManager().updateEquipmentStats(sender);
    plugin.getChampionManager().update(sender);
    plugin.getStatUpdateManager().updateVanillaAttributes(sender);
    plugin.getStatsMenu().setTargetPlayer(sender);
    plugin.getStatsMenu().open(sender);
  }

  @Command(identifier = "inspect", permissions = "strife.command.inspect")
  public void inspectCommand(Player sender, @Arg(name = "target") Player target) {
    if (!target.isValid()) {
      MessageUtils.sendMessage(sender, "&eThis player is offline or doesn't exist!");
      return;
    }
    plugin.getStatsMenu().setTargetPlayer(target);
    plugin.getStatsMenu().open(sender);
  }

}
