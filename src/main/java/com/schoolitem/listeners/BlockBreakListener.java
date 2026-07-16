package com.schoolitem.listeners;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockBreakListener implements Listener {
    private final SchoolItem plugin;
    private final PluginConfig config;
    
    public BlockBreakListener() {
        this.plugin = SchoolItem.getInstance();
        this.config = plugin.getPluginConfig();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!config.isAbilityEnabled("multiplierblock")) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || !item.hasItemMeta()) return;
        
        double multiplier = getMultiplierValue(item);
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
    
    private double getMultiplierValue(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        String displayName = config.getAbilityDisplayName("multiplierblock");
        
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