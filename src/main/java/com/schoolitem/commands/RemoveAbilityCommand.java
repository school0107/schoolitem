package com.schoolitem.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveAbilityCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        if (!player.hasPermission("schoolitem.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length < 1 || !args[0].equalsIgnoreCase("remove")) {
            sender.sendMessage(ChatColor.RED + "Sử dụng: /si remove");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Vui lòng cầm item trên tay!");
            return true;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            sender.sendMessage(ChatColor.RED + "Item này không có ability!");
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
                } else {
                    skip = false;
                }
                continue;
            }
            if (skip) {
                skip = false;
                continue;
            }
            newLore.add(line);
        }
        
        if (!removed) {
            sender.sendMessage(ChatColor.RED + "Item này không có ability!");
            return true;
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        
        sender.sendMessage(ChatColor.GREEN + "✓ Đã xóa ability khỏi item!");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("remove");
        }
        return new ArrayList<>();
    }
}