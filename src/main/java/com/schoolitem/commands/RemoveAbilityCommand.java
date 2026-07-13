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
        
        if (args.length < 1 || !args[0].equalsIgnoreCase("remove")) {
            sender.sendMessage(ChatColor.RED + "Sử dụng: /si remove [ability]");
            sender.sendMessage(ChatColor.YELLOW + "Ví dụ: /si remove pve (xóa ability PVE)");
            sender.sendMessage(ChatColor.YELLOW + "Hoặc: /si remove (xóa tất cả ability)");
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
        
        // Kiểm tra xem có chỉ định ability cụ thể không
        String targetAbility = null;
        if (args.length >= 2) {
            targetAbility = args[1].toLowerCase();
            if (!abilities.contains(targetAbility)) {
                sender.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
                sender.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
                return true;
            }
        }
        
        boolean removed = false;
        boolean skip = false;
        String abilityDisplay = targetAbility != null ? getAbilityDisplay(targetAbility) : null;
        
        for (String line : lore) {
            // Phát hiện separator
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                // Kiểm tra xem có phải là separator của ability cần xóa không
                if (!skip) {
                    skip = true;
                    removed = true;
                } else {
                    skip = false;
                }
                continue;
            }
            
            if (skip) {
                // Nếu đang skip, kiểm tra xem có cần skip dòng này không
                if (targetAbility != null && abilityDisplay != null) {
                    // Nếu là ability cụ thể, chỉ skip nếu dòng chứa ability đó
                    if (line.contains(abilityDisplay)) {
                        // Bỏ qua dòng này (xóa)
                        removed = true;
                        skip = false;
                        continue;
                    } else {
                        // Không phải ability cần xóa, giữ lại
                        newLore.add(line);
                        skip = false;
                        continue;
                    }
                } else {
                    // Xóa tất cả ability
                    continue;
                }
            }
            
            // Nếu không trong block skip, thêm vào newLore
            if (targetAbility != null && abilityDisplay != null && line.contains(abilityDisplay)) {
                // Nếu tìm thấy ability cần xóa nhưng không nằm trong block
                removed = true;
                continue;
            }
            
            newLore.add(line);
        }
        
        // Nếu không tìm thấy ability cần xóa
        if (!removed) {
            if (targetAbility != null) {
                sender.sendMessage(ChatColor.RED + "Item này không có ability " + targetAbility + "!");
            } else {
                sender.sendMessage(ChatColor.RED + "Item này không có ability!");
            }
            return true;
        }
        
        // Xóa các dòng trống thừa
        List<String> finalLore = new ArrayList<>();
        for (String line : newLore) {
            if (!line.trim().isEmpty()) {
                finalLore.add(line);
            }
        }
        
        // Nếu không còn lore nào, set lore thành null
        if (finalLore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(finalLore);
        }
        
        item.setItemMeta(meta);
        
        if (targetAbility != null) {
            sender.sendMessage(ChatColor.GREEN + "✓ Đã xóa ability " + targetAbility + " khỏi item!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "✓ Đã xóa tất cả ability khỏi item!");
        }
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete cho subcommand: remove
            if ("remove".startsWith(args[0].toLowerCase())) {
                completions.add("remove");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Tab complete cho ability
            for (String ability : abilities) {
                if (ability.startsWith(args[1].toLowerCase())) {
                    completions.add(ability);
                }
            }
        }
        
        return completions;
    }
}