package com.schoolitem.commands;

import com.schoolitem.SchoolItem;
import org.bukkit.ChatColor;
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
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length < 3 || !args[0].equalsIgnoreCase("add")) {
            player.sendMessage("§cSử dụng: /si add <ability> <value>");
            player.sendMessage("§eCác ability: pve, pvp, multiplierblock");
            return true;
        }
        
        String ability = args[1].toLowerCase();
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cGiá trị phải là số!");
            return true;
        }
        
        if (value < 0) {
            player.sendMessage("§cGiá trị không được âm!");
            return true;
        }
        
        if (!isValidAbility(ability)) {
            player.sendMessage("§cAbility không hợp lệ!");
            player.sendMessage("§eCác ability: pve, pvp, multiplierblock");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cVui lòng cầm item trên tay!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Remove old ability if exists
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            if (!line.contains(getAbilityDisplayName(ability))) {
                newLore.add(line);
            }
        }
        lore = newLore;
        
        // Add new ability
        String color = getAbilityColor(ability);
        String emoji = getAbilityEmoji(ability);
        String displayName = getAbilityDisplayName(ability);
        String unit = ability.equals("multiplierblock") ? "x" : "%";
        
        lore.add(color + "§m--------------------------------");
        lore.add(color + emoji + " " + displayName + " §fGiá trị: §e" + value + unit);
        if (!ability.equals("multiplierblock")) {
            lore.add("§7✦ §fGiảm " + value + unit + " sát thương");
        } else {
            lore.add("§7✦ §fNhân " + value + "x số lượng block");
        }
        lore.add(color + "§m--------------------------------");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage("§a✓ Đã thêm ability " + ability + " với giá trị " + value + unit);
        return true;
    }
    
    private boolean isValidAbility(String ability) {
        return ability.equals("pve") || ability.equals("pvp") || ability.equals("multiplierblock");
    }
    
    private String getAbilityDisplayName(String ability) {
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