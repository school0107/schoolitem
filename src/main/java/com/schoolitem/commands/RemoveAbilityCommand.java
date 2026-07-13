package com.schoolitem.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RemoveAbilityCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        if (!player.hasPermission("schoolitem.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length < 1 || !args[0].equalsIgnoreCase("remove")) {
            player.sendMessage("§cSử dụng: /si remove");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cVui lòng cầm item trên tay!");
            return true;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            player.sendMessage("§cItem này không có ability!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        boolean removed = false;
        boolean skip = false;
        
        for (String line : lore) {
            if (line.contains("§m--------------------------------")) {
                if (!skip) {
                    skip = true;
                    removed = true;
                    continue;
                }
            }
            if (skip) {
                skip = false;
                continue;
            }
            newLore.add(line);
        }
        
        if (!removed) {
            player.sendMessage("§cItem này không có ability!");
            return true;
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        
        player.sendMessage("§a✓ Đã xóa ability khỏi item!");
        return true;
    }
}