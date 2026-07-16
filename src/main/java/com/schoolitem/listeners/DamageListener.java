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
    
    public DamageListener(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() == null || event.getDamager() == null) return;
        
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // ============================================
        // 1. PVE / PVP Damage Reduction (Chỉ Player)
        // ============================================
        if (victim instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta()) {
                
                boolean isPVE = damager instanceof Monster;
                boolean isPVP = damager instanceof Player;
                
                if ((isPVE && config.isAbilityEnabled("pve")) || 
                    (isPVP && config.isAbilityEnabled("pvp"))) {
                    String ability = isPVE ? "pve" : "pvp";
                    double value = getAbilityValueFromItem(item, ability);
                    
                    if (value > 0) {
                        if (value > 100) value = 100;
                        double damage = event.getDamage();
                        double reducedDamage = damage * (1 - value / 100.0);
                        event.setDamage(reducedDamage);
                    }
                }
            }
        }
        
        // ============================================
        // 2. THORNS - Phản sát thương (FIXED)
        // Áp dụng cho cả Player và Monster
        // Tỉ lệ kích hoạt: 40% (config)
        // Phản lại theo giá trị % sát thương nhận vào
        // ============================================
        if (victim instanceof LivingEntity && config.isAbilityEnabled("thorns")) {
            LivingEntity victimEntity = (LivingEntity) victim;
            ItemStack item = null;
            
            // Lấy item từ tay của victim
            if (victim instanceof Player) {
                item = ((Player) victim).getInventory().getItemInMainHand();
            } else if (victim instanceof Mob) {
                Mob mob = (Mob) victim;
                if (mob.getEquipment() != null) {
                    item = mob.getEquipment().getItemInMainHand();
                }
            }
            
            if (item != null && item.hasItemMeta()) {
                double thornsValue = getAbilityValueFromItem(item, "thorns");
                double chance = config.getAbilityChance("thorns"); // 40% mặc định
                
                // Kiểm tra tỉ lệ kích hoạt (40%)
                if (thornsValue > 0 && random.nextDouble() * 100 < chance) {
                    // Tính sát thương phản lại theo giá trị %
                    double damage = event.getDamage();
                    double reflectDamage = damage * (thornsValue / 100.0);
                    
                    if (reflectDamage > 0 && damager instanceof LivingEntity) {
                        LivingEntity attacker = (LivingEntity) damager;
                        
                        // Gây sát thương phản lại
                        attacker.damage(reflectDamage);
                        
                        // Hiệu ứng âm thanh
                        if (config.isSoundEffects()) {
                            // Âm thanh cho attacker
                            if (attacker instanceof Player) {
                                ((Player) attacker).playSound(attacker.getLocation(), 
                                    Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            } else {
                                attacker.getWorld().playSound(attacker.getLocation(), 
                                    Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            }
                            
                            // Âm thanh cho victim
                            if (victim instanceof Player) {
                                ((Player) victim).playSound(victim.getLocation(), 
                                    Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                            } else {
                                victim.getWorld().playSound(victim.getLocation(), 
                                    Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                            }
                        }
                        
                        // Hiệu ứng hạt
                        if (config.isParticleEffects() && damager.getWorld() != null) {
                            damager.getWorld().spawnParticle(
                                Particle.CRIT,
                                damager.getLocation().add(0, 1, 0),
                                20, 0.3, 0.3, 0.3, 0.1
                            );
                            victim.getWorld().spawnParticle(
                                Particle.SWEEP_ATTACK,
                                victim.getLocation().add(0, 1, 0),
                                10, 0.3, 0.3, 0.3, 0.1
                            );
                        }
                        
                        // Thông báo cho Player (nếu là PVP)
                        if (damager instanceof Player && victim instanceof Player) {
                            ((Player) damager).sendMessage(ColorUtils.colorize(
                                config.getMessagePrefix() + "&c🌵 Bạn bị phản " + 
                                String.format("%.1f", reflectDamage) + " sát thương từ " + 
                                ((Player) victim).getName() + "!"
                            ));
                        }
                    }
                }
            }
        }
        
        // ============================================
        // 3. Lifesteal - Hút máu (Player tấn công)
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("lifesteal")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && victim instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) victim;
                double damage = event.getDamage();
                double lifestealValue = getAbilityValueFromItem(item, "lifesteal");
                double chance = config.getAbilityChance("lifesteal");
                
                if (lifestealValue > 0 && random.nextDouble() * 100 < chance) {
                    // Kiểm tra target có đang bị Wound không
                    double healMultiplier = 1.0;
                    if (target instanceof Player) {
                        healMultiplier = getHealMultiplier((Player) target);
                    }
                    
                    double healAmount = damage * (lifestealValue / 100.0) * healMultiplier;
                    
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
        // 4. HungerSteal - Hút thức ăn (Player tấn công Player)
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("hungersteal")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && victim instanceof Player) {
                Player targetPlayer = (Player) victim;
                double damage = event.getDamage();
                double hungerValue = getAbilityValueFromItem(item, "hungersteal");
                double chance = config.getAbilityChance("hungersteal");
                
                if (hungerValue > 0 && random.nextDouble() * 100 < chance) {
                    int foodToSteal = (int) Math.ceil(hungerValue / 10.0);
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
        // 5. Wound - Vết thương (Player tấn công Player)
        // ============================================
        if (damager instanceof Player player && config.isAbilityEnabled("wound")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && victim instanceof Player) {
                Player targetPlayer = (Player) victim;
                double woundValue = getAbilityValueFromItem(item, "wound");
                double chance = config.getAbilityChance("wound");
                int duration = config.getAbilityDuration("wound");
                
                if (woundValue > 0 && random.nextDouble() * 100 < chance) {
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
                        woundValue + "% khả năng hồi máu trong " + duration + "s!"
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
        
        // Lấy giá trị Wound từ item đang cầm
        double woundValue = getWoundValue(player);
        if (woundValue > 0) {
            double healAmount = event.getAmount();
            double reducedHeal = healAmount * (1 - woundValue / 100.0);
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
        
        double woundValue = getWoundValue(player);
        return 1.0 - (woundValue / 100.0);
    }
    
    private double getWoundValue(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return 0;
        return getAbilityValueFromItem(item, "wound");
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