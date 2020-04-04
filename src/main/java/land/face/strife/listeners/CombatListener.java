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
package land.face.strife.listeners;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ARMOR;
import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE;
import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BLOCKING;

import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import land.face.strife.StrifePlugin;
import land.face.strife.data.DamageModifiers;
import land.face.strife.data.StrifeMob;
import land.face.strife.events.StrifeDamageEvent;
import land.face.strife.stats.StrifeStat;
import land.face.strife.util.DamageUtil;
import land.face.strife.util.DamageUtil.AttackType;
import land.face.strife.util.DamageUtil.DamageType;
import land.face.strife.util.ItemUtil;
import land.face.strife.util.ProjectileUtil;
import land.face.strife.util.SpecialStatusUtil;
import land.face.strife.util.TargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class CombatListener implements Listener {

  private final StrifePlugin plugin;
  private static Set<Player> FRIENDLY_PLAYER_CHECKER = new HashSet<>();
  private static HashMap<UUID, Long> SLIME_HIT_MAP = new HashMap<>();

  public CombatListener(StrifePlugin plugin) {
    this.plugin = plugin;
  }

  public static void addPlayer(Player player) {
    FRIENDLY_PLAYER_CHECKER.add(player);
  }

  public static void removePlayer(Player player) {
    FRIENDLY_PLAYER_CHECKER.remove(player);
  }

  public static boolean hasFriendlyPlayer(Player player) {
    return FRIENDLY_PLAYER_CHECKER.contains(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handleTNT(EntityDamageByEntityEvent event) {
    if (event.isCancelled()) {
      return;
    }
    if (event.getEntity() instanceof LivingEntity && event.getDamager() instanceof TNTPrimed) {
      double distance = event.getDamager().getLocation().distance(event.getEntity().getLocation());
      double multiplier = Math.max(0.3, 4 / (distance + 3));
      DamageUtil.removeDamageModifiers(event);
      event.setDamage(multiplier * (10 + ((LivingEntity) event.getEntity()).getMaxHealth() * 0.4));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handleFireworks(EntityDamageByEntityEvent event) {
    if (event.isCancelled()) {
      return;
    }
    if (event.getDamager() instanceof Firework && SpecialStatusUtil
        .isNoDamage((Firework) event.getDamager())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handleNpcHits(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Projectile) {
      if (event.getEntity().hasMetadata("NPC")) {
        event.getDamager().remove();
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void strifeDamageHandler(EntityDamageByEntityEvent event) {
    if (event.isCancelled() || event.getCause() == DamageCause.CUSTOM) {
      return;
    }
    if (plugin.getDamageManager().isHandledDamage(event.getDamager())) {
      DamageUtil.removeDamageModifiers(event);
      event.setDamage(BASE, plugin.getDamageManager().getHandledDamage(event.getDamager()));
      return;
    }
    if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof ArmorStand) {
      return;
    }

    LivingEntity defendEntity = (LivingEntity) event.getEntity();
    LivingEntity attackEntity = DamageUtil.getAttacker(event.getDamager());

    if (attackEntity == null) {
      return;
    }
    if (attackEntity instanceof Slime && !canSlimeHit(attackEntity.getUniqueId())) {
      event.setCancelled(true);
      return;
    }

    boolean blocked = (event.isApplicable(BLOCKING) && event.getDamage(BLOCKING) != 0) || (
        defendEntity instanceof Shulker && event.isApplicable(ARMOR)
            && event.getDamage(ARMOR) != 0);
    DamageUtil.removeDamageModifiers(event);

    if (attackEntity instanceof Player && FRIENDLY_PLAYER_CHECKER.contains(attackEntity)) {
      FRIENDLY_PLAYER_CHECKER.remove(attackEntity);
      event.setCancelled(true);
      return;
    }

    if (event.getCause() == DamageCause.MAGIC) {
      event.setDamage(BASE, event.getDamage(BASE));
      return;
    }

    Projectile projectile = null;
    boolean isProjectile = false;
    boolean isMultishot = false;
    String[] extraEffects = null;

    if (event.getDamager() instanceof Projectile) {
      isProjectile = true;
      projectile = (Projectile) event.getDamager();
      String hitEffects = ProjectileUtil.getHitEffects(projectile);
      if (StringUtils.isNotBlank(hitEffects)) {
        extraEffects = hitEffects.split("~");
      }
      int shotId = ProjectileUtil.getShotId(projectile);
      if (shotId != 0) {
        String idKey = "SHOT_HIT_" + shotId;
        if (defendEntity.hasMetadata(idKey)) {
          isMultishot = true;
        } else {
          defendEntity.setMetadata(idKey, new FixedMetadataValue(StrifePlugin.getInstance(), true));
          Bukkit.getScheduler().runTaskLater(plugin,
              () -> defendEntity.removeMetadata(idKey, StrifePlugin.getInstance()), 1000L);
        }
      }
    }

    StrifeMob attacker = plugin.getStrifeMobManager().getStatMob(attackEntity);
    StrifeMob defender = plugin.getStrifeMobManager().getStatMob(defendEntity);

    if (TargetingUtil.isFriendly(attacker, defender)) {
      event.setCancelled(true);
      return;
    }

    float attackMultiplier = 1f;
    float healMultiplier = 1f;

    AttackType attackType = DamageUtil.getAttackType(event);

    if (isProjectile) {
      attackMultiplier = ProjectileUtil.getAttackMult(projectile);
    }

    if (attackType == AttackType.MELEE) {
      attackMultiplier = plugin.getAttackSpeedManager().getAttackMultiplier(attacker);
      if (ItemUtil.isWandOrStaff(attackEntity.getEquipment().getItemInMainHand())) {
        ProjectileUtil.shootWand(attacker, attackMultiplier);
        event.setCancelled(true);
        return;
      }
      attackMultiplier = (float) Math.pow(attackMultiplier, 1.25);
    } else if (attackType == AttackType.EXPLOSION) {
      double distance = event.getDamager().getLocation().distance(event.getEntity().getLocation());
      attackMultiplier *= Math.max(0.3, 4 / (distance + 3));
      healMultiplier = 0.3f;
    }

    if (isMultishot) {
      attackMultiplier *= 0.25;
    }

    if (attackMultiplier < 0.10 && extraEffects == null) {
      event.setCancelled(true);
      removeIfExisting(projectile);
      return;
    }

    Bukkit.getScheduler().runTaskLater(plugin, () -> defendEntity.setNoDamageTicks(0), 0L);

    boolean isSneakAttack = plugin.getStealthManager().isStealthed(attackEntity);
    boolean applyOnHit = attackMultiplier > 0.5f;

    putSlimeHit(attackEntity);

    if (attackEntity instanceof Player) {
      plugin.getStealthManager().unstealthPlayer((Player) attackEntity);
    }
    if (defendEntity instanceof Player) {
      plugin.getStealthManager().unstealthPlayer((Player) defendEntity);
    }

    DamageModifiers damageModifiers = new DamageModifiers();
    damageModifiers.setAttackType(attackType);
    damageModifiers.setAttackMultiplier(attackMultiplier);
    damageModifiers.setHealMultiplier(healMultiplier);
    damageModifiers.setApplyOnHitEffects(applyOnHit);
    damageModifiers.setSneakAttack(isSneakAttack);
    damageModifiers.setBlocking(blocked);

    boolean attackSuccess = DamageUtil.preDamage(attacker, defender, damageModifiers);
    if (!attackSuccess) {
      removeIfExisting(projectile);
      event.setCancelled(true);
      return;
    }

    Map<DamageType, Float> damage = DamageUtil.buildDamage(attacker, defender, damageModifiers);
    DamageUtil.reduceDamage(attacker, defender, damage, damageModifiers);
    float finalDamage = DamageUtil.damage(attacker, defender, damage, damageModifiers);

    StrifeDamageEvent strifeDamageEvent = new StrifeDamageEvent(attacker, defender,
        damageModifiers);
    strifeDamageEvent.setFinalDamage(finalDamage);
    Bukkit.getPluginManager().callEvent(strifeDamageEvent);

    if (strifeDamageEvent.isCancelled()) {
      event.setCancelled(true);
      return;
    }

    DamageUtil.postDamage(attacker, defender, damage, damageModifiers);

    DamageUtil.applyExtraEffects(attacker, defender, extraEffects);

    if (attackEntity instanceof Bee) {
      plugin.getDamageManager().dealDamage(attacker, defender, finalDamage);
      event.setCancelled(true);
      return;
    }

    event.setDamage(BASE, finalDamage);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void strifeEntityDeath(EntityDeathEvent event) {
    if (event.getEntity().getKiller() == null) {
      return;
    }
    StrifeMob killer = plugin.getStrifeMobManager().getStatMob(event.getEntity().getKiller());
    if (killer.getStat(StrifeStat.HP_ON_KILL) > 0.1) {
      DamageUtil.restoreHealthWithPenalties(event.getEntity().getKiller(), killer.getStat(
          StrifeStat.HP_ON_KILL));
    }
    if (killer.getStat(StrifeStat.RAGE_ON_KILL) > 0.1) {
      plugin.getRageManager().addRage(killer, killer.getStat(StrifeStat.RAGE_ON_KILL));
    }
  }

  private boolean canSlimeHit(UUID uuid) {
    return SLIME_HIT_MAP.getOrDefault(uuid, 0L) + 250 < System.currentTimeMillis();
  }

  public static void putSlimeHit(LivingEntity livingEntity) {
    if (livingEntity instanceof Slime) {
      SLIME_HIT_MAP.put(livingEntity.getUniqueId(), System.currentTimeMillis());
    }
  }

  private void removeIfExisting(Projectile projectile) {
    if (projectile == null) {
      return;
    }
    projectile.remove();
  }
}
