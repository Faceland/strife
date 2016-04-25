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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;

public class StatsDefenseMenuItem extends MenuItem {

    private final StrifePlugin plugin;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
    private static final DecimalFormat REDUCER_FORMAT = new DecimalFormat("#.#");

    public StatsDefenseMenuItem(StrifePlugin plugin) {
        super(ChatColor.WHITE + "Defensive Stats", new ItemStack(Material.IRON_CHESTPLATE));
        this.plugin = plugin;
    }

    @Override
    public ItemStack getFinalIcon(Player player) {
        Champion champion = plugin.getChampionManager().getChampion(player.getUniqueId());
        ItemStack itemStack = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(itemStack.getType());
        itemMeta.setDisplayName(getDisplayName());
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore = new ArrayList<>(getLore());
        lore.add(ChatColor.BLUE + "Hitpoints: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache().getAttribute(StrifeAttribute.HEALTH)));
        if (champion.getCache().getAttribute(StrifeAttribute.REGENERATION) > 1) {
            lore.add(ChatColor.BLUE + "Regeneration: " + ChatColor.WHITE + champion.getCache().getAttribute(StrifeAttribute.REGENERATION));
        }
        double armor = 100 * (1 - (100 / (100 + (Math.pow((champion.getCache().getAttribute(StrifeAttribute.ARMOR) * 100), 1.2)))));
        lore.add(ChatColor.BLUE + "Armor: " + ChatColor.WHITE +
                DECIMAL_FORMAT.format(100 * champion.getCache().getAttribute(StrifeAttribute.ARMOR)) +
                ChatColor.GRAY + " (" + REDUCER_FORMAT.format(armor) + "%)");
        if (champion.getCache().getAttribute(StrifeAttribute.EVASION) > 0) {
            double evasion = 100 * (1 - (100 / (100 + (Math.pow((champion.getCache().getAttribute(StrifeAttribute.EVASION) * 100), 1.1)))));
            lore.add(ChatColor.BLUE + "Evasion: " + ChatColor.WHITE +
                    DECIMAL_FORMAT.format(100 * champion.getCache().getAttribute(StrifeAttribute.EVASION)) +
                    ChatColor.GRAY + " (" + REDUCER_FORMAT.format(evasion) + "%)");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.BLOCK) != 0.0) {
            if (champion.getCache().getAttribute(StrifeAttribute.BLOCK) <= 0.85) {
                lore.add(ChatColor.BLUE + "Block: " + ChatColor.WHITE + DECIMAL_FORMAT
                        .format(champion.getCache().getAttribute(StrifeAttribute.BLOCK) * 100) + "%");
            } else {
                lore.add(ChatColor.BLUE + "Block: " + ChatColor.WHITE + "85% " + ChatColor.GRAY + "(Max)");
            }
        }
        if (champion.getCache().getAttribute(StrifeAttribute.RESISTANCE) > 0.2) {
            lore.add(
                    ChatColor.BLUE + "Elemental Resist: " + ChatColor.WHITE + DECIMAL_FORMAT.format(100 * champion
                            .getCache().getAttribute(StrifeAttribute.RESISTANCE)) + "%");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.ABSORB_CHANCE) > 0) {
            if (champion.getCache().getAttribute(StrifeAttribute.ABSORB_CHANCE) < 0.35) {
                lore.add(ChatColor.BLUE + "Absorb Chance: " + ChatColor.WHITE + DECIMAL_FORMAT
                        .format(champion.getCache().getAttribute(StrifeAttribute.ABSORB_CHANCE) * 100) + "%");
            } else {
                lore.add(ChatColor.BLUE + "Absorb Chance: " + ChatColor.WHITE + "35% " + ChatColor.GRAY + "(Max)");
            }
        }
        if (champion.getCache().getAttribute(StrifeAttribute.PARRY) > 0) {
            if (champion.getCache().getAttribute(StrifeAttribute.PARRY) < 0.75) {
                lore.add(ChatColor.BLUE + "Parry Chance: " + ChatColor.WHITE + DECIMAL_FORMAT
                        .format(champion.getCache().getAttribute(StrifeAttribute.PARRY) * 100) + "%");
            } else {
                lore.add(ChatColor.BLUE + "Parry Chance: " + ChatColor.WHITE + "75% " + ChatColor.GRAY + "(Max)");
            }
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
