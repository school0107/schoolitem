package com.schoolitem.listeners;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.utils.ColorUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DamageListener implements Listener {
    private final SchoolItem plugin;
    private final PluginConfig config;
    private final Random random = new Random();
    private final Map<UUID, Long> woundEffect = new HashMap<>();
    private SetBonusListener setBonusListener;
    
    public DamageListener(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }
    
    public void setSetBonusListener(SetBonusListener listener) {
        this.setBonusListener = listener;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() == null || event.getDamager() == null) return;
        
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // ============================================
        // 0. DODGE - Né tránh (cho victim là Player)
        // ============================================
        if (victim instanceof Player player && config.isAbilityEnabled("dodge")) {
            double totalDodge = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalDodge += getAbilityValueFromItem(mainHand, "dodge");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalDodge += getAbilityValueFromItem(armorPiece, "dodge");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalDodge += setBonusListener.getBuffValue(player, "dodge");
            }
            
            if (totalDodge > 100) totalDodge = 100;
            
            if (totalDodge > 0 && random.nextDouble() * 100 < totalDodge) {
                event.setCancelled(true);
                
                if (config.isSoundEffects()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SWIM, 1.0f, 1.5f);
                    if (damager instanceof Player) {
                        ((Player) damager).playSound(damager.getLocation(), Sound.ENTITY_ARROW_MISS, 1.0f, 1.0f);
                    }
                }
                
                if (config.isParticleEffects() && player.getWorld() != null) {
                    player.getWorld().spawnParticle(
                        Particle.CLOUD,
                        player.getLocation().add(0, 1, 0),
                        15, 0.3, 0.3, 0.3, 0.1
                    );
                }
                
                if (damager instanceof Player) {
                    ((Player) damager).sendMessage(ColorUtils.colorize(
                        config.getMessagePrefix() + "&b💨 " + player.getName() + " đã né tránh đòn tấn công của bạn!"
                    ));
                }
                return;
            }
        }
        
        // ============================================
        // 1. CRITICAL STRIKE - Đòn chí mạng (cho damager là Player)
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("critical")) {
            double totalCritical = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalCritical += getAbilityValueFromItem(mainHand, "critical");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalCritical += getAbilityValueFromItem(armorPiece, "critical");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalCritical += setBonusListener.getBuffValue(player, "critical");
            }
            
            if (totalCritical > 100) totalCritical = 100;
            
            if (totalCritical > 0 && random.nextDouble() * 100 < totalCritical) {
                // Nhân sát thương x1.8
                double multiplier = config.getConfig().getDouble("abilities.critical.multiplier", 1.8);
                double damage = event.getDamage();
                double criticalDamage = damage * multiplier;
                event.setDamage(criticalDamage);
                
                // Hiệu ứng âm thanh
                if (config.isSoundEffects()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
                    if (victim instanceof LivingEntity) {
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                    }
                }
                
                // Hiệu ứng hạt
                if (config.isParticleEffects() && victim.getWorld() != null) {
                    victim.getWorld().spawnParticle(
                        Particle.CRIT,
                        victim.getLocation().add(0, 1, 0),
                        30, 0.3, 0.3, 0.3, 0.1
                    );
                    victim.getWorld().spawnParticle(
                        Particle.EXPLOSION,
                        victim.getLocation().add(0, 1, 0),
                        5, 0.3, 0.3, 0.3, 0.1
                    );
                }
                
                // Thông báo (chỉ PVP)
                if (victim instanceof Player) {
                    ((Player) victim).sendMessage(ColorUtils.colorize(
                        config.getMessagePrefix() + "&c💥 Bạn đã bị đòn chí mạng từ " + 
                        player.getName() + "! (+" + (int)((multiplier - 1) * 100) + "% sát thương)"
                    ));
                    player.sendMessage(ColorUtils.colorize(
                        config.getMessagePrefix() + "&a💥 Đòn chí mạng! Gây " + 
                        String.format("%.1f", criticalDamage) + " sát thương lên " + 
                        ((Player) victim).getName() + "!"
                    ));
                }
            }
        }
        
        // ============================================
        // 2. PRECISION - Chính xác
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("precision")) {
            double totalPrecision = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalPrecision += getAbilityValueFromItem(mainHand, "precision");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalPrecision += getAbilityValueFromItem(armorPiece, "precision");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalPrecision += setBonusListener.getBuffValue(player, "precision");
            }
            
            if (totalPrecision > 100) totalPrecision = 100;
            
            if (totalPrecision > 0 && random.nextDouble() * 100 < totalPrecision) {
                double damage = event.getDamage();
                double bonusDamage = damage * (totalPrecision / 100.0);
                event.setDamage(damage + bonusDamage);
                
                if (config.isSoundEffects()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                }
                
                if (config.isParticleEffects() && victim.getWorld() != null) {
                    victim.getWorld().spawnParticle(
                        Particle.CRIT,
                        victim.getLocation().add(0, 1, 0),
                        15, 0.3, 0.3, 0.3, 0.1
                    );
                }
            }
        }
        
        // ============================================
        // 3. SET BONUS - Tăng sát thương
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("setbonus")) {
            if (setBonusListener != null) {
                double damageBonus = setBonusListener.getBuffValue(player, "damage");
                if (damageBonus > 0) {
                    double damage = event.getDamage();
                    double bonusDamage = damage * (damageBonus / 100.0);
                    event.setDamage(damage + bonusDamage);
                }
            }
        }
        
        // ============================================
        // 4. PVE / PVP Damage Reduction
        // ============================================
        if (victim instanceof Player player) {
            double totalPve = 0;
            double totalPvp = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalPve += getAbilityValueFromItem(mainHand, "pve");
                totalPvp += getAbilityValueFromItem(mainHand, "pvp");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalPve += getAbilityValueFromItem(armorPiece, "pve");
                    totalPvp += getAbilityValueFromItem(armorPiece, "pvp");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                double healthBonus = setBonusListener.getBuffValue(player, "health");
                if (healthBonus > 0) {
                    double maxHealth = player.getMaxHealth();
                    double bonusHealth = maxHealth * (healthBonus / 100.0);
                    player.setMaxHealth(maxHealth + bonusHealth);
                }
            }
            
            boolean isPVE = damager instanceof Monster;
            boolean isPVP = damager instanceof Player;
            
            if (isPVE && config.isAbilityEnabled("pve") && totalPve > 0) {
                double damage = event.getDamage();
                double reducedDamage = damage * (1 - Math.min(totalPve, 100) / 100.0);
                event.setDamage(reducedDamage);
            }
            
            if (isPVP && config.isAbilityEnabled("pvp") && totalPvp > 0) {
                double damage = event.getDamage();
                double reducedDamage = damage * (1 - Math.min(totalPvp, 100) / 100.0);
                event.setDamage(reducedDamage);
            }
        }
        
        // ============================================
        // 5. THORNS - Phản sát thương
        // ============================================
        if (victim instanceof LivingEntity && config.isAbilityEnabled("thorns")) {
            double totalThorns = 0;
            
            ItemStack mainHand = null;
            if (victim instanceof Player) {
                mainHand = ((Player) victim).getInventory().getItemInMainHand();
            } else if (victim instanceof Mob) {
                Mob mob = (Mob) victim;
                if (mob.getEquipment() != null) {
                    mainHand = mob.getEquipment().getItemInMainHand();
                }
            }
            
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalThorns += getAbilityValueFromItem(mainHand, "thorns");
            }
            
            if (victim instanceof Player) {
                Player player = (Player) victim;
                ItemStack[] armor = player.getInventory().getArmorContents();
                for (ItemStack armorPiece : armor) {
                    if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                        totalThorns += getAbilityValueFromItem(armorPiece, "thorns");
                    }
                }
            } else if (victim instanceof Mob) {
                Mob mob = (Mob) victim;
                if (mob.getEquipment() != null) {
                    ItemStack[] armor = mob.getEquipment().getArmorContents();
                    for (ItemStack armorPiece : armor) {
                        if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                            totalThorns += getAbilityValueFromItem(armorPiece, "thorns");
                        }
                    }
                }
            }
            
            if (victim instanceof Player && setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalThorns += setBonusListener.getBuffValue((Player) victim, "thorns");
            }
            
            if (totalThorns > 100) totalThorns = 100;
            
            if (totalThorns > 0 && damager instanceof LivingEntity) {
                double chance = config.getAbilityChance("thorns");
                
                if (random.nextDouble() * 100 < chance) {
                    double damage = event.getDamage();
                    double reflectDamage = damage * (totalThorns / 100.0);
                    
                    if (reflectDamage > 0) {
                        LivingEntity attacker = (LivingEntity) damager;
                        attacker.damage(reflectDamage);
                        
                        if (config.isSoundEffects()) {
                            if (attacker instanceof Player) {
                                ((Player) attacker).playSound(attacker.getLocation(), 
                                    Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            } else {
                                attacker.getWorld().playSound(attacker.getLocation(), 
                                    Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            }
                            
                            if (victim instanceof Player) {
                                ((Player) victim).playSound(victim.getLocation(), 
                                    Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                            } else {
                                victim.getWorld().playSound(victim.getLocation(), 
                                    Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                            }
                        }
                        
                        if (config.isParticleEffects() && damager.getWorld() != null) {
                            damager.getWorld().spawnParticle(
                                Particle.CRIT,
                                damager.getLocation().add(0, 1, 0),
                                15, 0.3, 0.3, 0.3, 0.1
                            );
                            victim.getWorld().spawnParticle(
                                Particle.SWEEP_ATTACK,
                                victim.getLocation().add(0, 1, 0),
                                10, 0.3, 0.3, 0.3, 0.1
                            );
                        }
                    }
                }
            }
        }
        
        // ============================================
        // 6. Lifesteal - Hút máu
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("lifesteal")) {
            double totalLifesteal = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalLifesteal += getAbilityValueFromItem(mainHand, "lifesteal");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalLifesteal += getAbilityValueFromItem(armorPiece, "lifesteal");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalLifesteal += setBonusListener.getBuffValue(player, "lifesteal");
            }
            
            if (totalLifesteal > 100) totalLifesteal = 100;
            
            if (totalLifesteal > 0 && victim instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) victim;
                double damage = event.getDamage();
                double chance = config.getAbilityChance("lifesteal");
                
                if (random.nextDouble() * 100 < chance) {
                    double healMultiplier = 1.0;
                    if (target instanceof Player) {
                        healMultiplier = getHealMultiplier((Player) target);
                    }
                    
                    double healAmount = damage * (totalLifesteal / 100.0) * healMultiplier;
                    
                    if (healAmount > 0) {
                        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                        player.setHealth(newHealth);
                        
                        if (config.isSoundEffects()) {
                            player.playSound(player.getLocation(), 
                                Sound.valueOf(config.getSound("lifesteal", "attacker")), 1.0f, 1.5f);
                            player.playSound(target.getLocation(), 
                                Sound.valueOf(config.getSound("lifesteal", "target")), 0.5f, 1.0f);
                        }
                        
                        if (config.isParticleEffects() && player.getWorld() != null) {
                            player.getWorld().spawnParticle(
                                Particle.HEART,
                                player.getLocation().add(0, 1, 0),
                                10, 0.3, 0.3, 0.3, 0.1
                            );
                        }
                    }
                }
            }
        }
        
        // ============================================
        // 7. HungerSteal - Hút thức ăn
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("hungersteal")) {
            double totalHunger = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalHunger += getAbilityValueFromItem(mainHand, "hungersteal");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalHunger += getAbilityValueFromItem(armorPiece, "hungersteal");
                }
            }
            
            if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                totalHunger += setBonusListener.getBuffValue(player, "hungersteal");
            }
            
            if (totalHunger > 100) totalHunger = 100;
            
            if (totalHunger > 0 && victim instanceof Player) {
                Player targetPlayer = (Player) victim;
                double chance = config.getAbilityChance("hungersteal");
                
                if (random.nextDouble() * 100 < chance) {
                    int foodToSteal = (int) Math.ceil(totalHunger / 10.0);
                    if (foodToSteal > 0) {
                        int targetFood = targetPlayer.getFoodLevel();
                        int newTargetFood = Math.max(0, targetFood - foodToSteal);
                        targetPlayer.setFoodLevel(newTargetFood);
                        
                        int playerFood = player.getFoodLevel();
                        int newPlayerFood = Math.min(20, playerFood + foodToSteal);
                        player.setFoodLevel(newPlayerFood);
                        
                        if (config.isSoundEffects()) {
                            player.playSound(player.getLocation(), 
                                Sound.valueOf(config.getSound("hungersteal", "attacker")), 1.0f, 1.0f);
                            targetPlayer.playSound(targetPlayer.getLocation(), 
                                Sound.valueOf(config.getSound("hungersteal", "target")), 1.0f, 0.5f);
                        }
                    }
                }
            }
        }
        
        // ============================================
        // 8. Wound - Vết thương
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("wound")) {
            double totalWound = 0;
            
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                totalWound += getAbilityValueFromItem(mainHand, "wound");
            }
            
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack armorPiece : armor) {
                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                    totalWound += getAbilityValueFromItem(armorPiece, "wound");
                }
            }
            
            if (totalWound > 100) totalWound = 100;
            
            if (totalWound > 0 && victim instanceof Player) {
                Player targetPlayer = (Player) victim;
                double chance = config.getAbilityChance("wound");
                int duration = config.getAbilityDuration("wound");
                
                if (random.nextDouble() * 100 < chance) {
                    UUID targetId = targetPlayer.getUniqueId();
                    woundEffect.put(targetId, System.currentTimeMillis() + duration * 1000);
                    
                    if (config.isSoundEffects()) {
                        targetPlayer.playSound(targetPlayer.getLocation(), 
                            Sound.valueOf(config.getSound("wound", "target")), 1.0f, 0.5f);
                        player.playSound(player.getLocation(), 
                            Sound.valueOf(config.getSound("wound", "attacker")), 1.0f, 1.0f);
                    }
                    
                    targetPlayer.sendMessage(ColorUtils.colorize(
                        config.getMessagePrefix() + "&c🩸 Bạn đã bị Vết Thương! Giảm " + 
                        totalWound + "% khả năng hồi máu trong " + duration + "s!"
                    ));
                    player.sendMessage(ColorUtils.colorize(
                        config.getMessagePrefix() + "&a🩸 Đã gây Vết Thương lên " + 
                        targetPlayer.getName() + "!"
                    ));
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCancelled()) return;
        
        UUID playerId = player.getUniqueId();
        if (!woundEffect.containsKey(playerId)) return;
        
        long expireTime = woundEffect.get(playerId);
        if (System.currentTimeMillis() > expireTime) {
            woundEffect.remove(playerId);
            return;
        }
        
        double totalWound = 0;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
            totalWound += getAbilityValueFromItem(mainHand, "wound");
        }
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armor) {
            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                totalWound += getAbilityValueFromItem(armorPiece, "wound");
            }
        }
        
        if (totalWound > 100) totalWound = 100;
        
        if (totalWound > 0) {
            double healAmount = event.getAmount();
            double reducedHeal = healAmount * (1 - totalWound / 100.0);
            event.setAmount(reducedHeal);
        }
    }
    
    private double getHealMultiplier(Player player) {
        UUID playerId = player.getUniqueId();
        if (!woundEffect.containsKey(playerId)) return 1.0;
        
        long expireTime = woundEffect.get(playerId);
        if (System.currentTimeMillis() > expireTime) {
            woundEffect.remove(playerId);
            return 1.0;
        }
        
        double totalWound = 0;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
            totalWound += getAbilityValueFromItem(mainHand, "wound");
        }
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armor) {
            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                totalWound += getAbilityValueFromItem(armorPiece, "wound");
            }
        }
        
        if (totalWound > 100) totalWound = 100;
        return 1.0 - (totalWound / 100.0);
    }
    
    private double getAbilityValueFromItem(ItemStack item, String ability) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        String displayName = config.getAbilityDisplayName(ability);
        
        for (String line : meta.getLore()) {
            if (line.contains(displayName)) {
                String[] parts = line.split(" ");
                for (String part : parts) {
                    try {
                        String numStr = part.replaceAll("[^0-9.]", "");
                        if (!numStr.isEmpty()) {
                            return Double.parseDouble(numStr);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }
}