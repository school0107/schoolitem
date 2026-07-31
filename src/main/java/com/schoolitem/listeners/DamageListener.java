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
        try {
            if (event.isCancelled()) return;
            if (event.getEntity() == null || event.getDamager() == null) return;
            
            Entity damager = event.getDamager();
            Entity victim = event.getEntity();
            
            // ============================================
            // 0. DODGE - Né tránh (cho victim là Player)
            // ============================================
            if (victim instanceof Player player && config.isAbilityEnabled("dodge")) {
                double totalDodge = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalDodge += getAbilityValueFromItem(mainHand, "dodge");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalDodge += getAbilityValueFromItem(armorPiece, "dodge");
                            }
                        }
                    }
                    
                    if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalDodge += setBonusListener.getBuffValue(player, "dodge");
                    }
                    
                    if (totalDodge > 100) totalDodge = 100;
                    
                    if (totalDodge > 0 && random.nextDouble() * 100 < totalDodge) {
                        event.setCancelled(true);
                        
                        if (config.isSoundEffects()) {
                            try {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SWIM, 1.0f, 1.5f);
                                if (damager instanceof Player) {
                                    ((Player) damager).playSound(damager.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
                                }
                            } catch (Exception ignored) {}
                        }
                        
                        if (config.isParticleEffects() && player.getWorld() != null) {
                            try {
                                player.getWorld().spawnParticle(
                                    Particle.CLOUD,
                                    player.getLocation().add(0, 1, 0),
                                    15, 0.3, 0.3, 0.3, 0.1
                                );
                            } catch (Exception ignored) {}
                        }
                        
                        if (damager instanceof Player) {
                            ((Player) damager).sendMessage(ColorUtils.colorize(
                                config.getMessagePrefix() + "&b💨 " + player.getName() + " đã né tránh đòn tấn công của bạn!"
                            ));
                        }
                        return;
                    }
                } catch (Exception ignored) {}
            }
            
            // ============================================
            // 1. CRITICAL STRIKE - Đòn chí mạng
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("critical")) {
                double totalCritical = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalCritical += getAbilityValueFromItem(mainHand, "critical");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalCritical += getAbilityValueFromItem(armorPiece, "critical");
                            }
                        }
                    }
                    
                    if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalCritical += setBonusListener.getBuffValue(player, "critical");
                    }
                    
                    if (totalCritical > 100) totalCritical = 100;
                    
                    if (totalCritical > 0 && random.nextDouble() * 100 < totalCritical) {
                        double multiplier = config.getConfig().getDouble("abilities.critical.multiplier", 1.8);
                        double damage = event.getDamage();
                        double criticalDamage = damage * multiplier;
                        event.setDamage(criticalDamage);
                        
                        if (config.isSoundEffects()) {
                            try {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
                                if (victim instanceof LivingEntity) {
                                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                                }
                            } catch (Exception ignored) {}
                        }
                        
                        if (config.isParticleEffects() && victim.getWorld() != null) {
                            try {
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
                            } catch (Exception ignored) {}
                        }
                        
                        if (victim instanceof Player) {
                            try {
                                ((Player) victim).sendMessage(ColorUtils.colorize(
                                    config.getMessagePrefix() + "&c💥 Bạn đã bị đòn chí mạng từ " + 
                                    player.getName() + "! (+" + (int)((multiplier - 1) * 100) + "% sát thương)"
                                ));
                                player.sendMessage(ColorUtils.colorize(
                                    config.getMessagePrefix() + "&a💥 Đòn chí mạng! Gây " + 
                                    String.format("%.1f", criticalDamage) + " sát thương lên " + 
                                    ((Player) victim).getName() + "!"
                                ));
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // ============================================
            // 2. PRECISION - Chính xác
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("precision")) {
                double totalPrecision = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalPrecision += getAbilityValueFromItem(mainHand, "precision");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalPrecision += getAbilityValueFromItem(armorPiece, "precision");
                            }
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
                            try {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                            } catch (Exception ignored) {}
                        }
                        
                        if (config.isParticleEffects() && victim.getWorld() != null) {
                            try {
                                victim.getWorld().spawnParticle(
                                    Particle.CRIT,
                                    victim.getLocation().add(0, 1, 0),
                                    15, 0.3, 0.3, 0.3, 0.1
                                );
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // ============================================
            // 3. SET BONUS - Tăng sát thương
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("setbonus")) {
                try {
                    if (setBonusListener != null) {
                        double damageBonus = setBonusListener.getBuffValue(player, "damage");
                        if (damageBonus > 0) {
                            double damage = event.getDamage();
                            double bonusDamage = damage * (damageBonus / 100.0);
                            event.setDamage(damage + bonusDamage);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // ============================================
            // 4. PVE / PVP Damage Reduction
            // ============================================
            if (victim instanceof Player player) {
                double totalPve = 0;
                double totalPvp = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalPve += getAbilityValueFromItem(mainHand, "pve");
                        totalPvp += getAbilityValueFromItem(mainHand, "pvp");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalPve += getAbilityValueFromItem(armorPiece, "pve");
                                totalPvp += getAbilityValueFromItem(armorPiece, "pvp");
                            }
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
                } catch (Exception ignored) {}
                
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
                
                try {
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
                        if (armor != null) {
                            for (ItemStack armorPiece : armor) {
                                if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                    totalThorns += getAbilityValueFromItem(armorPiece, "thorns");
                                }
                            }
                        }
                    } else if (victim instanceof Mob) {
                        Mob mob = (Mob) victim;
                        if (mob.getEquipment() != null) {
                            ItemStack[] armor = mob.getEquipment().getArmorContents();
                            if (armor != null) {
                                for (ItemStack armorPiece : armor) {
                                    if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                        totalThorns += getAbilityValueFromItem(armorPiece, "thorns");
                                    }
                                }
                            }
                        }
                    }
                    
                    if (victim instanceof Player && setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalThorns += setBonusListener.getBuffValue((Player) victim, "thorns");
                    }
                } catch (Exception ignored) {}
                
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
                                try {
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
                                } catch (Exception ignored) {}
                            }
                            
                            if (config.isParticleEffects() && damager.getWorld() != null) {
                                try {
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
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            
            // ============================================
            // 6. LIFESTEAL PVP - Hút máu từ người chơi
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("lifesteal") && victim instanceof Player) {
                double totalLifesteal = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalLifesteal += getAbilityValueFromItem(mainHand, "lifesteal");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalLifesteal += getAbilityValueFromItem(armorPiece, "lifesteal");
                            }
                        }
                    }
                    
                    if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalLifesteal += setBonusListener.getBuffValue(player, "lifesteal");
                    }
                } catch (Exception ignored) {}
                
                if (totalLifesteal > 100) totalLifesteal = 100;
                
                if (totalLifesteal > 0) {
                    Player targetPlayer = (Player) victim;
                    double damage = event.getDamage();
                    double chance = config.getAbilityChance("lifesteal");
                    
                    if (random.nextDouble() * 100 < chance) {
                        double healMultiplier = getHealMultiplier(targetPlayer);
                        double healAmount = damage * (totalLifesteal / 100.0) * healMultiplier;
                        
                        if (healAmount > 0) {
                            try {
                                double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                                player.setHealth(newHealth);
                                
                                if (config.isSoundEffects()) {
                                    try {
                                        player.playSound(player.getLocation(), 
                                            Sound.valueOf(config.getSound("lifesteal", "attacker")), 1.0f, 1.5f);
                                        targetPlayer.playSound(targetPlayer.getLocation(), 
                                            Sound.valueOf(config.getSound("lifesteal", "target")), 0.5f, 1.0f);
                                    } catch (Exception ignored) {}
                                }
                                
                                if (config.isParticleEffects() && player.getWorld() != null) {
                                    try {
                                        player.getWorld().spawnParticle(
                                            Particle.HEART,
                                            player.getLocation().add(0, 1, 0),
                                            10, 0.3, 0.3, 0.3, 0.1
                                        );
                                    } catch (Exception ignored) {}
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            
            // ============================================
            // 7. LIFESTEAL PVE - Hút máu từ thực thể (không phải Player)
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("lifestealPve") && 
                victim instanceof LivingEntity && !(victim instanceof Player)) {
                
                double totalLifestealPve = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalLifestealPve += getAbilityValueFromItem(mainHand, "lifestealPve");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalLifestealPve += getAbilityValueFromItem(armorPiece, "lifestealPve");
                            }
                        }
                    }
                    
                    if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalLifestealPve += setBonusListener.getBuffValue(player, "lifestealPve");
                    }
                } catch (Exception ignored) {}
                
                if (totalLifestealPve > 100) totalLifestealPve = 100;
                
                if (totalLifestealPve > 0) {
                    LivingEntity target = (LivingEntity) victim;
                    double damage = event.getDamage();
                    double chance = config.getAbilityChance("lifestealPve");
                    
                    if (random.nextDouble() * 100 < chance) {
                        double healAmount = damage * (totalLifestealPve / 100.0);
                        
                        if (healAmount > 0) {
                            try {
                                double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                                player.setHealth(newHealth);
                                
                                if (config.isSoundEffects()) {
                                    try {
                                        player.playSound(player.getLocation(), 
                                            Sound.valueOf(config.getSound("lifestealPve", "attacker")), 1.0f, 1.5f);
                                        target.getWorld().playSound(target.getLocation(), 
                                            Sound.valueOf(config.getSound("lifestealPve", "target")), 0.5f, 1.0f);
                                    } catch (Exception ignored) {}
                                }
                                
                                if (config.isParticleEffects() && player.getWorld() != null) {
                                    try {
                                        player.getWorld().spawnParticle(
                                            Particle.HEART,
                                            player.getLocation().add(0, 1, 0),
                                            10, 0.3, 0.3, 0.3, 0.1
                                        );
                                    } catch (Exception ignored) {}
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            
            // ============================================
            // 8. HungerSteal - Hút thức ăn
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("hungersteal")) {
                double totalHunger = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalHunger += getAbilityValueFromItem(mainHand, "hungersteal");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalHunger += getAbilityValueFromItem(armorPiece, "hungersteal");
                            }
                        }
                    }
                    
                    if (setBonusListener != null && config.isAbilityEnabled("setbonus")) {
                        totalHunger += setBonusListener.getBuffValue(player, "hungersteal");
                    }
                } catch (Exception ignored) {}
                
                if (totalHunger > 100) totalHunger = 100;
                
                if (totalHunger > 0 && victim instanceof Player) {
                    Player targetPlayer = (Player) victim;
                    double chance = config.getAbilityChance("hungersteal");
                    
                    if (random.nextDouble() * 100 < chance) {
                        int foodToSteal = (int) Math.ceil(totalHunger / 10.0);
                        if (foodToSteal > 0) {
                            try {
                                int targetFood = targetPlayer.getFoodLevel();
                                int newTargetFood = Math.max(0, targetFood - foodToSteal);
                                targetPlayer.setFoodLevel(newTargetFood);
                                
                                int playerFood = player.getFoodLevel();
                                int newPlayerFood = Math.min(20, playerFood + foodToSteal);
                                player.setFoodLevel(newPlayerFood);
                                
                                if (config.isSoundEffects()) {
                                    try {
                                        player.playSound(player.getLocation(), 
                                            Sound.valueOf(config.getSound("hungersteal", "attacker")), 1.0f, 1.0f);
                                        targetPlayer.playSound(targetPlayer.getLocation(), 
                                            Sound.valueOf(config.getSound("hungersteal", "target")), 1.0f, 0.5f);
                                    } catch (Exception ignored) {}
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            
            // ============================================
            // 9. Wound - Vết thương
            // ============================================
            if (damager instanceof Player player && config.isAbilityEnabled("wound")) {
                double totalWound = 0;
                
                try {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                        totalWound += getAbilityValueFromItem(mainHand, "wound");
                    }
                    
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor != null) {
                        for (ItemStack armorPiece : armor) {
                            if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                                totalWound += getAbilityValueFromItem(armorPiece, "wound");
                            }
                        }
                    }
                } catch (Exception ignored) {}
                
                if (totalWound > 100) totalWound = 100;
                
                if (totalWound > 0 && victim instanceof Player) {
                    Player targetPlayer = (Player) victim;
                    double chance = config.getAbilityChance("wound");
                    int duration = config.getAbilityDuration("wound");
                    
                    if (random.nextDouble() * 100 < chance) {
                        UUID targetId = targetPlayer.getUniqueId();
                        woundEffect.put(targetId, System.currentTimeMillis() + duration * 1000);
                        
                        if (config.isSoundEffects()) {
                            try {
                                targetPlayer.playSound(targetPlayer.getLocation(), 
                                    Sound.valueOf(config.getSound("wound", "target")), 1.0f, 0.5f);
                                player.playSound(player.getLocation(), 
                                    Sound.valueOf(config.getSound("wound", "attacker")), 1.0f, 1.0f);
                            } catch (Exception ignored) {}
                        }
                        
                        try {
                            targetPlayer.sendMessage(ColorUtils.colorize(
                                config.getMessagePrefix() + "&c🩸 Bạn đã bị Vết Thương! Giảm " + 
                                totalWound + "% khả năng hồi máu trong " + duration + "s!"
                            ));
                            player.sendMessage(ColorUtils.colorize(
                                config.getMessagePrefix() + "&a🩸 Đã gây Vết Thương lên " + 
                                targetPlayer.getName() + "!"
                            ));
                        } catch (Exception ignored) {}
                    }
                }
            }
            
        } catch (Exception e) {
            // Log lỗi nhưng không crash server
            plugin.getLogger().warning("Error in DamageListener: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        try {
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
            try {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand != null && !mainHand.getType().isAir() && mainHand.hasItemMeta()) {
                    totalWound += getAbilityValueFromItem(mainHand, "wound");
                }
                
                ItemStack[] armor = player.getInventory().getArmorContents();
                if (armor != null) {
                    for (ItemStack armorPiece : armor) {
                        if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                            totalWound += getAbilityValueFromItem(armorPiece, "wound");
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            if (totalWound > 100) totalWound = 100;
            
            if (totalWound > 0) {
                double healAmount = event.getAmount();
                double reducedHeal = healAmount * (1 - totalWound / 100.0);
                event.setAmount(reducedHeal);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in onEntityRegainHealth: " + e.getMessage());
        }
    }
    
    private double getHealMultiplier(Player player) {
        try {
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
            if (armor != null) {
                for (ItemStack armorPiece : armor) {
                    if (armorPiece != null && !armorPiece.getType().isAir() && armorPiece.hasItemMeta()) {
                        totalWound += getAbilityValueFromItem(armorPiece, "wound");
                    }
                }
            }
            
            if (totalWound > 100) totalWound = 100;
            return 1.0 - (totalWound / 100.0);
        } catch (Exception e) {
            return 1.0;
        }
    }
    
    private double getAbilityValueFromItem(ItemStack item, String ability) {
        try {
            if (item == null || !item.hasItemMeta()) return 0;
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasLore()) return 0;
            
            String displayName = config.getAbilityDisplayName(ability);
            
            for (String line : meta.getLore()) {
                if (line == null) continue;
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
        } catch (Exception ignored) {}
        return 0;
    }
}