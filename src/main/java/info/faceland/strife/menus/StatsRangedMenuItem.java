/**
 * The MIT License
 * Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.faceland.strife.menus;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.Champion;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsRangedMenuItem extends MenuItem {

    private final StrifePlugin plugin;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");

    public StatsRangedMenuItem(StrifePlugin plugin) {
        super(ChatColor.WHITE + "Ranged Stats", new ItemStack(Material.BOW));
        this.plugin = plugin;
    }

    @Override
    public ItemStack getFinalIcon(Player player) {
        Champion champion = plugin.getChampionManager().getChampion(player.getUniqueId());
        Map<StrifeAttribute, Double> valueMap = champion.getAttributeValues();
        ItemStack itemStack = new ItemStack(Material.BOW);
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(itemStack.getType());
        itemMeta.setDisplayName(getDisplayName());
        List<String> lore = new ArrayList<>(getLore());
        lore.add(ChatColor.YELLOW + "Ranged Damage: " + ChatColor.WHITE + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.RANGED_DAMAGE)));
        if (valueMap.get(StrifeAttribute.ACCURACY) > 0) {
            lore.add(ChatColor.YELLOW + "Accuracy: " + ChatColor.WHITE + "+"+ DECIMAL_FORMAT.format(100*valueMap.get(
                StrifeAttribute.ACCURACY)) + "%");
        }
        if (valueMap.get(StrifeAttribute.SNARE_CHANCE) > 0) {
            lore.add(ChatColor.YELLOW + "Snare Chance: " + ChatColor.WHITE + DECIMAL_FORMAT.format(100*valueMap.get(
                StrifeAttribute.SNARE_CHANCE)) + "%");
        }
        if (valueMap.get(StrifeAttribute.CRITICAL_RATE) > 0) {
            lore.add(ChatColor.YELLOW + "Critical Strike: " + ChatColor.WHITE + DECIMAL_FORMAT.format(
                valueMap.get(StrifeAttribute.CRITICAL_RATE)*100) + "% " + ChatColor.GRAY + "(" + DECIMAL_FORMAT.format(
                valueMap.get(StrifeAttribute.CRITICAL_DAMAGE)*100) + "%)");
        }
        if (valueMap.get(StrifeAttribute.ARMOR_PENETRATION) > 0) {
            lore.add(ChatColor.YELLOW + "Armor Penetration: " + ChatColor.WHITE
                     + DECIMAL_FORMAT.format(valueMap.get(StrifeAttribute.ARMOR_PENETRATION) * 100) + "%");
        }
        if (valueMap.get(StrifeAttribute.FIRE_DAMAGE) > 0) {
            lore.add(ChatColor.YELLOW + "Fire/Ignite: " + ChatColor.WHITE + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.FIRE_DAMAGE)) + " / " + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.IGNITE_CHANCE) * 100) + "%)");
        }
        if (valueMap.get(StrifeAttribute.LIGHTNING_DAMAGE) > 0) {
            lore.add(ChatColor.YELLOW + "Lightning/Shock: " + ChatColor.WHITE + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.LIGHTNING_DAMAGE)) + " / " + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.SHOCK_CHANCE) * 100) + "%");
        }
        if (valueMap.get(StrifeAttribute.ICE_DAMAGE) > 0) {
            lore.add(ChatColor.YELLOW + "Ice/Freeze: " + ChatColor.WHITE + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.ICE_DAMAGE)) + " / " + DECIMAL_FORMAT.format(valueMap.get(
                StrifeAttribute.FREEZE_CHANCE) * 100) + "%");
        }
        if (valueMap.get(StrifeAttribute.LIFE_STEAL) > 0) {
            lore.add(
                ChatColor.YELLOW + "Life Steal: " + ChatColor.WHITE + DECIMAL_FORMAT.format(valueMap.get(StrifeAttribute.LIFE_STEAL) * 100)
                + "%");
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Override
    public void onItemClick(ItemClickEvent event) {
        super.onItemClick(event);
    }

}
