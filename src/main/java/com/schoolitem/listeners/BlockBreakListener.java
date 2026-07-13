package com.schoolitem.listeners;

import com.schoolitem.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || !item.hasItemMeta()) return;
        
        double multiplier = ItemUtils.getAbilityValue(item, "multiplierblock");
        if (multiplier <= 0) return;
        
        // Get drops
        for (ItemStack drop : event.getBlock().getDrops(item)) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            
            // Add multiplied drops directly to inventory
            int extraAmount = (int) Math.round(multiplier - 1);
            if (extraAmount > 0) {
                ItemStack copy = drop.clone();
                copy.setAmount(extraAmount * drop.getAmount());
                player.getInventory().addItem(copy);
            }
        }
    }
}
