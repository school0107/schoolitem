package com.schoolitem.listeners;

import com.schoolitem.SchoolItem;
import com.schoolitem.utils.ItemUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageListener implements Listener {
    private final SchoolItem plugin;
    
    public DamageListener(SchoolItem plugin) {
        this.plugin = plugin;
    }
    
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
        double value = ItemUtils.getAbilityValue(item, ability);
        
        if (value <= 0) return;
        
        // Limit reduction to 100%
        if (value > 100) value = 100;
        
        double damage = event.getDamage();
        double reducedDamage = damage * (1 - value / 100.0);
        
        event.setDamage(reducedDamage);
    }
}
