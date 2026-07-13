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

public class AddAbilityCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        if (!player.hasPermission("schoolitem.admin")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length < 3 || !args[0].equalsIgnoreCase("add")) {
            sender.sendMessage("§cSử dụng: /si add <ability> <value>");
            sender.sendMessage("§eCác ability: pve, pvp, multiplierblock");
            sender.sendMessage("§eVí dụ: /si add pve 50");
            return true;
        }
        
        String ability = args[1].toLowerCase();
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cGiá trị phải là số!");
            return true;
        }
        
        if (value < 0) {
            sender.sendMessage("§cGiá trị không được âm!");
            return true;
        }
        
        if (!ability.equals("pve") && !ability.equals("pvp") && !ability.equals("multiplierblock")) {
            sender.sendMessage("§cAbility không hợp lệ!");
            sender.sendMessage("§eCác ability: pve, pvp, multiplierblock");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cVui lòng cầm item trên tay!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        List<String> newLore = new ArrayList<>();
        String abilityDisplay = getAbilityDisplay(ability);
        boolean skip = false;
        
        for (String line : lore) {
            if (line.contains("§m--------------------------------")) {
                skip = !skip;
                continue;
            }
            if (skip) {
                skip = false;
                continue;
            }
            if (line.contains(abilityDisplay)) {
                continue;
            }
            newLore.add(line);
        }
        lore = newLore;
        
        String color = getAbilityColor(ability);
        String emoji = getAbilityEmoji(ability);
        String displayName = getAbilityDisplay(ability);
        String unit = ability.equals("multiplierblock") ? "x" : "%";
        String description = ability.equals("multiplierblock") ? 
            "Nhân " + value + "x số lượng block" : 
            "Giảm " + value + unit + " sát thương";
        
        lore.add("§7§m--------------------------------");
        lore.add(color + emoji + " " + displayName + " §fGiá trị: §e" + value + unit);
        lore.add("§7✦ §f" + description);
        lore.add("§7§m--------------------------------");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        sender.sendMessage("§a✓ Đã thêm ability " + ability + " với giá trị " + value + unit);
        return true;
    }
    
    private String getAbilityDisplay(String ability) {
        switch (ability) {
            case "pve": return "Giảm Sát Thương PVE";
            case "pvp": return "Giảm Sát Thương PVP";
            case "multiplierblock": return "Nhân Block";
            default: return ability;
        }
    }
    
    private String getAbilityColor(String ability) {
        switch (ability) {
            case "pve": return "§c";
            case "pvp": return "§6";
            case "multiplierblock": return "§a";
            default: return "§f";
        }
    }
    
    private String getAbilityEmoji(String ability) {
        switch (ability) {
            case "pve": return "⚔️";
            case "pvp": return "🛡️";
            case "multiplierblock": return "⛏️";
            default: return "✦";
        }
    }
}