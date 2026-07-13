package com.schoolitem.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class DamageListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        
        Entity damager = event.getDamager();
        boolean isPVE = damager instanceof Monster;
        boolean isPVP = damager instanceof Player;
        
        if (!isPVE && !isPVP) return;
        
        String ability = isPVE ? "pve" : "pvp";
        double value = getAbilityValue(item, ability);
        
        if (value <= 0) return;
        if (value > 100) value = 100;
        
        double damage = event.getDamage();
        double reducedDamage = damage * (1 - value / 100.0);
        
        event.setDamage(reducedDamage);
    }
    
    private double getAbilityValue(ItemStack item, String ability) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        String displayName = ability.equals("pve") ? "Giảm Sát Thương PVE" : "Giảm Sát Thương PVP";
        
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