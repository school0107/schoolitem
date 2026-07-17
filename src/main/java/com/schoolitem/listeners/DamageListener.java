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
    private final Map<UUID, Long> lastThornsMessage = new HashMap<>();
    
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
        // 1. PVE / PVP Damage Reduction (Player)
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
        // 2. THORNS - Phản sát thương (CÓ TỈ LỆ 40%)
        // ============================================
        if (victim instanceof LivingEntity && config.isAbilityEnabled("thorns")) {
            double totalThorns = 0;
            
            // Lấy từ item trên tay
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
            
            // Lấy từ giáp
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
            
            if (totalThorns > 100) totalThorns = 100;
            
            if (totalThorns > 0 && damager instanceof LivingEntity) {
                double chance = config.getAbilityChance("thorns"); // 40% mặc định
                
                // CHỈ KÍCH HOẠT KHI RANDOM < CHANCE (40%)
                if (random.nextDouble() * 100 < chance) {
                    double damage = event.getDamage();
                    double reflectDamage = damage * (totalThorns / 100.0);
                    
                    if (reflectDamage > 0) {
                        LivingEntity attacker = (LivingEntity) damager;
                        attacker.damage(reflectDamage);
                        
                        // Hiệu ứng âm thanh
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
                        
                        // Hiệu ứng hạt
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
                        
                        // KHÔNG HIỂN THỊ THÔNG BÁO - ĐÃ XÓA
                        // Chỉ hiển thị cho PVP nếu muốn (nhưng đã tắt)
                    }
                }
            }
        }
        
        // ============================================
        // 3. Lifesteal - Hút máu (KHÔNG LOG)
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
        // 4. HungerSteal - Hút thức ăn (KHÔNG LOG)
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
        // 5. Wound - Vết thương (KHÔNG LOG)
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
                    
                    // Chỉ hiển thị thông báo Wound (quan trọng)
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