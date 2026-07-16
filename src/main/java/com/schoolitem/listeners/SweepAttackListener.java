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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
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
        
        // Chỉ xử lý khi click chuột trái (attack)
        if (!event.getAction().name().contains("LEFT_CLICK")) return;
        if (!config.isAbilityEnabled("sweepattack")) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        
        // Kiểm tra item có ability Sweep Attack không
        double sweepValue = getAbilityValueFromItem(item, "sweepattack");
        if (sweepValue <= 0) return;
        
        // Kiểm tra tỉ lệ kích hoạt (10%)
        double chance = config.getAbilityChance("sweepattack");
        if (random.nextDouble() * 100 > chance) return;
        
        // Kiểm tra cooldown
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
        
        // Kiểm tra đã swing chưa (tránh spam)
        if (lastSwingMap.containsKey(playerId)) {
            long lastSwing = lastSwingMap.get(playerId);
            if ((currentTime - lastSwing) < 200) return; // 0.2s debounce
        }
        lastSwingMap.put(playerId, currentTime);
        
        // Lấy thông số
        double damage = sweepValue;
        int range = config.getConfig().getInt("abilities.sweepattack.range", 20);
        
        // Kích hoạt Sweep Attack
        activateSweepAttack(player, damage, range);
        
        // Set cooldown
        cooldownMap.put(playerId, currentTime);
    }
    
    private void activateSweepAttack(Player player, double damage, int range) {
        // Lấy hướng nhìn của player
        Vector direction = player.getLocation().getDirection().normalize();
        Location startLoc = player.getEyeLocation().clone().add(direction.clone().multiply(0.5));
        Location endLoc = startLoc.clone().add(direction.clone().multiply(range));
        
        // Hiệu ứng âm thanh
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        
        // Hiệu ứng hạt - Vẽ đường sóng
        final int steps = range * 2;
        final double stepSize = 0.5;
        final double radius = 1.0;
        
        // Tạo hiệu ứng ánh sáng
        for (int i = 0; i < steps; i++) {
            double t = i * stepSize;
            if (t > range) break;
            
            Location point = startLoc.clone().add(direction.clone().multiply(t));
            
            // Vẽ vòng tròn xung quanh đường đi
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * radius * (1 - t / range * 0.7);
                double z = Math.sin(rad) * radius * (1 - t / range * 0.7);
                Location circlePoint = point.clone().add(x, 0, z);
                
                player.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    circlePoint,
                    1, 0, 0, 0, 0
                );
            }
            
            // Particle nước
            player.getWorld().spawnParticle(
                Particle.WATER_SPLASH,
                point,
                2, 0.1, 0.1, 0.1, 0.01
            );
            
            // Particle spark
            player.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                point,
                1, 0.05, 0.05, 0.05, 0.01
            );
        }
        
        // Phát hiện và gây sát thương cho các entity trên đường
        List<Entity> hitEntities = new ArrayList<>();
        for (int i = 0; i <= range * 2; i++) {
            double t = i * 0.5;
            if (t > range) break;
            
            Location checkPoint = startLoc.clone().add(direction.clone().multiply(t));
            
            // Kiểm tra entity trong phạm vi
            for (Entity entity : player.getWorld().getNearbyEntities(checkPoint, 1.5, 1.5, 1.5)) {
                if (entity.equals(player)) continue;
                if (!(entity instanceof LivingEntity)) continue;
                if (hitEntities.contains(entity)) continue;
                
                // Không gây sát thương cho người chơi cùng team (nếu có)
                if (entity instanceof Player) {
                    Player target = (Player) entity;
                    // Có thể thêm check team ở đây
                }
                
                // Gây sát thương
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player);
                
                // Hiệu ứng khi trúng
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
                
                // Particle khi trúng
                target.getWorld().spawnParticle(
                    Particle.EXPLOSION,
                    target.getLocation().add(0, 1, 0),
                    10, 0.3, 0.3, 0.3, 0.1
                );
                
                target.getWorld().spawnParticle(
                    Particle.CRIT,
                    target.getLocation().add(0, 1, 0),
                    20, 0.3, 0.3, 0.3, 0.1
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
        Location endParticle = endLoc.clone();
        player.getWorld().spawnParticle(
            Particle.EXPLOSION_LARGE,
            endParticle,
            5, 0.5, 0.5, 0.5, 0.1
        );
        
        // Hiệu ứng âm thanh cuối
        player.getWorld().playSound(endParticle, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
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
