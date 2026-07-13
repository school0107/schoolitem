package com.schoolitem.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockBreakListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || !item.hasItemMeta()) return;
        
        double multiplier = getAbilityValue(item, "multiplierblock");
        if (multiplier <= 0) return;
        
        for (ItemStack drop : event.getBlock().getDrops(item)) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            
            int extraAmount = (int) Math.round(multiplier - 1);
            if (extraAmount > 0) {
                ItemStack copy = drop.clone();
                copy.setAmount(extraAmount * drop.getAmount());
                player.getInventory().addItem(copy);
            }
        }
    }
    
    private double getAbilityValue(ItemStack item, String ability) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        for (String line : meta.getLore()) {
            if (line.contains("Nhân Block")) {
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