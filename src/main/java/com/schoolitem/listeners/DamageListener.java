package com.schoolitem.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class DamageListener implements Listener {
    private final Random random = new Random();
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // ===== PVE / PVP Damage Reduction =====
        if (victim instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta()) {
                
                boolean isPVE = damager instanceof Monster;
                boolean isPVP = damager instanceof Player;
                
                if (isPVE || isPVP) {
                    String ability = isPVE ? "pve" : "pvp";
                    double value = getAbilityValueFromItem(item, ability);
                    
                    if (value > 0) {
                        if (value > 100) value = 100;
                        double damage = event.getDamage();
                        double reducedDamage = damage * (1 - value / 100.0);
                        event.setDamage(reducedDamage);
                    }
                }
                
                // ===== Thorns (Phản sát thương) =====
                double thornsValue = getAbilityValueFromItem(item, "thorns");
                if (thornsValue > 0 && damager instanceof LivingEntity) {
                    LivingEntity attacker = (LivingEntity) damager;
                    double damage = event.getDamage();
                    double reflectDamage = damage * (thornsValue / 100.0);
                    
                    if (reflectDamage > 0 && random.nextDouble() * 100 < thornsValue) {
                        attacker.damage(reflectDamage);
                        // Hiệu ứng âm thanh
                        if (attacker instanceof Player) {
                            ((Player) attacker).playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        }
                        if (victim instanceof Player) {
                            ((Player) victim).playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                        }
                    }
                }
            }
        }
        
        // ===== Lifesteal (Hút máu) & HungerSteal (Hút thức ăn) =====
        if (damager instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && victim instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) victim;
                double damage = event.getDamage();
                
                // Lifesteal
                double lifestealValue = getAbilityValueFromItem(item, "lifesteal");
                if (lifestealValue > 0 && random.nextDouble() * 100 < lifestealValue) {
                    double healAmount = damage * (lifestealValue / 100.0);
                    if (healAmount > 0) {
                        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                        player.setHealth(newHealth);
                        // Hiệu ứng âm thanh
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        player.playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5f, 1.0f);
                    }
                }
                
                // HungerSteal (Hút thanh thức ăn)
                double hungerValue = getAbilityValueFromItem(item, "hungersteal");
                if (hungerValue > 0 && random.nextDouble() * 100 < hungerValue) {
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        // Giảm thức ăn của đối phương
                        int foodToSteal = (int) Math.ceil(hungerValue / 10.0); // 10% = 1 food point
                        if (foodToSteal > 0) {
                            int targetFood = targetPlayer.getFoodLevel();
                            int newTargetFood = Math.max(0, targetFood - foodToSteal);
                            targetPlayer.setFoodLevel(newTargetFood);
                            
                            // Tăng thức ăn cho người dùng
                            int playerFood = player.getFoodLevel();
                            int newPlayerFood = Math.min(20, playerFood + foodToSteal);
                            player.setFoodLevel(newPlayerFood);
                            
                            // Hiệu ứng âm thanh
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                            
                            // Hiệu ứng hạt (particle effect) 
                            if (targetPlayer.getWorld() != null) {
                                targetPlayer.getWorld().spawnParticle(
                                    org.bukkit.Particle.ITEM_CRACK,
                                    targetPlayer.getLocation().add(0, 1, 0),
                                    10, 0.3, 0.3, 0.3, 0.1,
                                    org.bukkit.Material.COOKED_BEEF.createItemData()
                                );
                            }
                        }
                    }
                }
            }
        }
    }
    
    private double getAbilityValueFromItem(ItemStack item, String ability) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        String displayName = getAbilityDisplayName(ability);
        
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
    
    private String getAbilityDisplayName(String ability) {
        switch (ability) {
            case "pve": return "Giảm Sát Thương PVE";
            case "pvp": return "Giảm Sát Thương PVP";
            case "lifesteal": return "Hút Máu";
            case "thorns": return "Phản Sát Thương";
            case "hungersteal": return "Hút Thanh Thức Ăn";
            default: return ability;
        }
    }
}