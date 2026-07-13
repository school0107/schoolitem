package com.schoolitem.commands;

import com.schoolitem.SchoolItem;
import com.schoolitem.utils.ItemUtils;
import net.md_5.bungee.api.ChatColor;
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
    private final SchoolItem plugin;
    
    public AddAbilityCommand(SchoolItem plugin) {
        this.plugin = plugin;
    }
    
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
            player.sendMessage("§eVí dụ: /si add pve 50 (giảm 50% sát thương)");
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
        
        // Add ability to item
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Check if ability already exists and remove it
        List<String> newLore = new ArrayList<>();
        boolean skip = false;
        for (String line : lore) {
            if (line.contains("&m--------------------------------")) {
                skip = true;
                continue;
            }
            if (skip) {
                skip = false;
                continue;
            }
            // Remove old ability line
            if (line.contains(getAbilityDisplayName(ability))) {
                continue;
            }
            newLore.add(line);
        }
        lore = newLore;
        
        // Add new ability lore
        String displayName = getAbilityDisplayName(ability);
        String color = getAbilityColor(ability);
        String emoji = getAbilityEmoji(ability);
        String unit = ability.equals("multiplierblock") ? "x" : "%";
        
        if (ability.equals("multiplierblock") && value < 1) {
            player.sendMessage("§cMultiplier block phải >= 1!");
            return true;
        }
        
        lore.add(ItemUtils.colorize("&7&m--------------------------------"));
        lore.add(ItemUtils.colorize(color + emoji + " " + displayName + " &fGiá trị: &e" + value + unit));
        if (!ability.equals("multiplierblock")) {
            lore.add(ItemUtils.colorize("&7✦ &fGiảm " + value + unit + " sát thương"));
        } else {
            lore.add(ItemUtils.colorize("&7✦ &fNhân " + value + "x số lượng block"));
        }
        lore.add(ItemUtils.colorize("&7&m--------------------------------"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage("§a✓ Đã thêm ability " + ability + " với giá trị " + value + unit + " vào item!");
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
            case "pve": return "&#FF6B6B";
            case "pvp": return "&#FFA94D";
            case "multiplierblock": return "&#69DB7C";
            default: return "&f";
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
