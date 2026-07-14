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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DamageListener implements Listener {
    private final Random random = new Random();
    private final Map<UUID, Long> woundEffect = new HashMap<>();
    private final int WOUND_DURATION = 10; // 10 seconds
    
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
        
        // ===== Lifesteal, HungerSteal, Wound =====
        if (damager instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && victim instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) victim;
                double damage = event.getDamage();
                
                // Lifesteal
                double lifestealValue = getAbilityValueFromItem(item, "lifesteal");
                if (lifestealValue > 0 && random.nextDouble() * 100 < lifestealValue) {
                    // Kiểm tra target có đang bị Wound không
                    double healMultiplier = getHealMultiplier(target);
                    double healAmount = damage * (lifestealValue / 100.0) * healMultiplier;
                    
                    if (healAmount > 0) {
                        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                        player.setHealth(newHealth);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        player.playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5f, 1.0f);
                    }
                }
                
                // HungerSteal
                double hungerValue = getAbilityValueFromItem(item, "hungersteal");
                if (hungerValue > 0 && random.nextDouble() * 100 < hungerValue) {
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        int foodToSteal = (int) Math.ceil(hungerValue / 10.0);
                        if (foodToSteal > 0) {
                            int targetFood = targetPlayer.getFoodLevel();
                            int newTargetFood = Math.max(0, targetFood - foodToSteal);
                            targetPlayer.setFoodLevel(newTargetFood);
                            
                            int playerFood = player.getFoodLevel();
                            int newPlayerFood = Math.min(20, playerFood + foodToSteal);
                            player.setFoodLevel(newPlayerFood);
                            
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                        }
                    }
                }
                
                // ===== Wound (Vết thương) =====
                double woundValue = getAbilityValueFromItem(item, "wound");
                if (woundValue > 0 && random.nextDouble() * 100 < 10.0) { // 10% tỉ lệ kích hoạt
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        UUID targetId = targetPlayer.getUniqueId();
                        
                        // Áp dụng hiệu ứng Wound
                        woundEffect.put(targetId, System.currentTimeMillis() + WOUND_DURATION * 1000);
                        
                        // Hiệu ứng âm thanh
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                        
                        // Hiển thị thông báo
                        targetPlayer.sendMessage("§c🩸 Bạn đã bị Vết Thương! Giảm " + woundValue + "% khả năng hồi máu trong " + WOUND_DURATION + "s!");
                        player.sendMessage("§a🩸 Đã gây Vết Thương lên " + targetPlayer.getName() + "!");
                    }
                }
            }
        }
    }
    
    // ===== Ngăn hồi máu/hút máu khi đang bị Wound =====
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
        
        // Giảm lượng hồi máu
        double healAmount = event.getAmount();
        double woundValue = getWoundValue(player);
        if (woundValue > 0) {
            double reducedHeal = healAmount * (1 - woundValue / 100.0);
            event.setAmount(reducedHeal);
        }
    }
    
    private double getHealMultiplier(LivingEntity entity) {
        if (!(entity instanceof Player)) return 1.0;
        Player player = (Player) entity;
        UUID playerId = player.getUniqueId();
        
        if (!woundEffect.containsKey(playerId)) return 1.0;
        long expireTime = woundEffect.get(playerId);
        if (System.currentTimeMillis() > expireTime) {
            woundEffect.remove(playerId);
            return 1.0;
        }
        
        double woundValue = getWoundValue(player);
        return 1.0 - (woundValue / 100.0);
    }
    
    private double getWoundValue(Player player) {
        // Lấy giá trị Wound từ item đang cầm (nếu có)
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return 0;
        return getAbilityValueFromItem(item, "wound");
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
            case "wound": return "Vết Thương";
            default: return ability;
        }
    }
}