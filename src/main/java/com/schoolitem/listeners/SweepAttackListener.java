package com.schoolitem.listeners;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class SweepAttackListener implements Listener {
    private final SchoolItem plugin;
    private final PluginConfig config;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private final Map<UUID, Long> lastSwingMap = new HashMap<>();
    
    public SweepAttackListener(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!event.getAction().name().contains("LEFT_CLICK")) return;
        if (!config.isAbilityEnabled("sweepattack")) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        
        double sweepValue = getAbilityValueFromItem(item, "sweepattack");
        if (sweepValue <= 0) return;
        
        double chance = config.getAbilityChance("sweepattack");
        if (random.nextDouble() * 100 > chance) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        int cooldownSeconds = config.getAbilityDuration("sweepattack");
        if (cooldownSeconds <= 0) cooldownSeconds = 5;
        
        if (cooldownMap.containsKey(playerId)) {
            long lastUse = cooldownMap.get(playerId);
            if ((currentTime - lastUse) < cooldownSeconds * 1000) {
                long remaining = (cooldownSeconds * 1000 - (currentTime - lastUse)) / 1000;
                player.sendMessage(ColorUtils.colorize(
                    config.getMessagePrefix() + "&c⏳ Sweep Attack đang hồi chiêu! Còn " + 
                    remaining + "s"
                ));
                return;
            }
        }
        
        if (lastSwingMap.containsKey(playerId)) {
            long lastSwing = lastSwingMap.get(playerId);
            if ((currentTime - lastSwing) < 200) return;
        }
        lastSwingMap.put(playerId, currentTime);
        
        double damage = sweepValue;
        int range = config.getConfig().getInt("abilities.sweepattack.range", 20);
        
        activateSweepAttack(player, damage, range);
        cooldownMap.put(playerId, currentTime);
    }
    
    private void activateSweepAttack(Player player, double damage, int range) {
        Vector direction = player.getLocation().getDirection().normalize();
        Location startLoc = player.getEyeLocation().clone().add(direction.clone().multiply(0.5));
        
        // Hiệu ứng âm thanh
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        
        // Tạo hiệu ứng sóng
        List<Entity> hitEntities = new ArrayList<>();
        for (int i = 0; i <= range * 2; i++) {
            double t = i * 0.5;
            if (t > range) break;
            
            Location checkPoint = startLoc.clone().add(direction.clone().multiply(t));
            
            // Particle SWEEP_ATTACK - hiệu ứng quét
            player.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                checkPoint,
                2, 0.1, 0.1, 0.1, 0
            );
            
            // Particle CRIT - hiệu ứng ánh sáng
            if (i % 2 == 0) {
                player.getWorld().spawnParticle(
                    Particle.CRIT,
                    checkPoint,
                    3, 0.2, 0.2, 0.2, 0.05
                );
            }
            
            // Particle FIREWORK - hiệu ứng màu xanh
            if (i % 3 == 0) {
                player.getWorld().spawnParticle(
                    Particle.FIREWORK,
                    checkPoint,
                    2, 0.1, 0.1, 0.1, 0
                );
            }
            
            // Kiểm tra entity trong phạm vi
            for (Entity entity : player.getWorld().getNearbyEntities(checkPoint, 1.5, 1.5, 1.5)) {
                if (entity.equals(player)) continue;
                if (!(entity instanceof LivingEntity)) continue;
                if (hitEntities.contains(entity)) continue;
                
                LivingEntity target = (LivingEntity) entity;
                
                // Gây sát thương
                target.damage(damage, player);
                
                // Hiệu ứng khi trúng
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
                
                // Particle khi trúng
                target.getWorld().spawnParticle(
                    Particle.EXPLOSION,
                    target.getLocation().add(0, 1, 0),
                    5, 0.3, 0.3, 0.3, 0.1
                );
                
                target.getWorld().spawnParticle(
                    Particle.CRIT,
                    target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.3, 0.3, 0.1
                );
                
                // Đẩy entity
                Vector knockback = direction.clone().multiply(0.5);
                target.setVelocity(knockback);
                
                hitEntities.add(entity);
                
                // Thông báo
                player.sendMessage(ColorUtils.colorize(
                    config.getMessagePrefix() + "&a🌊 Sweep Attack đã trúng " + 
                    target.getName() + "! Gây " + String.format("%.1f", damage) + " sát thương!"
                ));
            }
        }
        
        // Thông báo nếu không trúng ai
        if (hitEntities.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(
                config.getMessagePrefix() + "&e🌊 Sweep Attack không trúng mục tiêu nào!"
            ));
        }
        
        // Hiệu ứng cuối đường
        Location endLoc = startLoc.clone().add(direction.clone().multiply(range));
        player.getWorld().spawnParticle(
            Particle.EXPLOSION,
            endLoc,
            3, 0.5, 0.5, 0.5, 0.1
        );
        player.getWorld().playSound(endLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
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