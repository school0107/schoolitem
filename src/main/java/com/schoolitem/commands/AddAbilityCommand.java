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

public class AddAbilityCommand implements CommandExecutor, TabCompleter {
    
    private final List<String> abilities = Arrays.asList("pve", "pvp", "multiplierblock");
    
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
        
        if (args.length < 3 || !args[0].equalsIgnoreCase("add")) {
            sender.sendMessage(ChatColor.RED + "Sử dụng: /si add <ability> <value>");
            sender.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
            sender.sendMessage(ChatColor.YELLOW + "Ví dụ: /si add pve 50");
            return true;
        }
        
        String ability = args[1].toLowerCase();
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Giá trị phải là số!");
            return true;
        }
        
        if (value < 0) {
            sender.sendMessage(ChatColor.RED + "Giá trị không được âm!");
            return true;
        }
        
        if (!abilities.contains(ability)) {
            sender.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
            sender.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Vui lòng cầm item trên tay!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Xóa ability cũ nếu có (bao gồm cả separator)
        List<String> newLore = new ArrayList<>();
        String abilityDisplay = getAbilityDisplay(ability);
        boolean skip = false;
        boolean found = false;
        
        for (String line : lore) {
            // Phát hiện separator
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                if (!skip) {
                    // Bắt đầu block ability
                    skip = true;
                    found = true;
                    continue;
                } else {
                    // Kết thúc block ability
                    skip = false;
                    continue;
                }
            }
            
            if (skip) {
                // Đang trong block ability, kiểm tra xem có phải ability cần xóa không
                if (line.contains(abilityDisplay)) {
                    // Đây là ability cần xóa, bỏ qua toàn bộ block
                    skip = false; // Kết thúc block
                    continue;
                } else {
                    // Không phải ability cần xóa, giữ lại dòng này
                    newLore.add(line);
                    continue;
                }
            }
            
            // Không trong block skip, thêm vào newLore
            newLore.add(line);
        }
        
        // Nếu không tìm thấy ability cũ, dùng lore cũ
        if (!found) {
            newLore = lore;
        }
        
        // Thêm ability mới
        String color = getAbilityColor(ability);
        String emoji = getAbilityEmoji(ability);
        String displayName = getAbilityDisplay(ability);
        String unit = ability.equals("multiplierblock") ? "x" : "%";
        String description = ability.equals("multiplierblock") ? 
            "Nhân " + value + "x số lượng block" : 
            "Giảm " + value + unit + " sát thương";
        
        newLore.add("§7§m--------------------------------");
        newLore.add(color + emoji + " " + displayName + " §fGiá trị: §e" + value + unit);
        newLore.add("§7✦ §f" + description);
        newLore.add("§7§m--------------------------------");
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        
        sender.sendMessage(ChatColor.GREEN + "✓ Đã thêm ability " + ability + " với giá trị " + value + unit);
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("add", "remove");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            for (String ability : abilities) {
                if (ability.startsWith(args[1].toLowerCase())) {
                    completions.add(ability);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            String ability = args[1].toLowerCase();
            if (ability.equals("pve") || ability.equals("pvp")) {
                completions.add("50");
                completions.add("75");
                completions.add("100");
            } else if (ability.equals("multiplierblock")) {
                completions.add("2");
                completions.add("3");
                completions.add("5");
                completions.add("10");
            }
        }
        
        return completions;
    }
}