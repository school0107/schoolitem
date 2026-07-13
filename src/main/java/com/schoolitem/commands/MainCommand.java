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

public class MainCommand implements CommandExecutor, TabCompleter {
    
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
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("add")) {
            return handleAdd(player, args);
        } else if (subCommand.equals("remove")) {
            return handleRemove(player, args);
        } else {
            sendHelp(sender);
            return true;
        }
    }
    
    private boolean handleAdd(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Sử dụng: /si add <ability> <value>");
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
            player.sendMessage(ChatColor.YELLOW + "Ví dụ: /si add pve 50");
            return true;
        }
        
        String ability = args[1].toLowerCase();
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Giá trị phải là số!");
            return true;
        }
        
        if (value < 0) {
            player.sendMessage(ChatColor.RED + "Giá trị không được âm!");
            return true;
        }
        
        if (!abilities.contains(ability)) {
            player.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Vui lòng cầm item trên tay!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Xóa ability cũ nếu có
        List<String> newLore = new ArrayList<>();
        String abilityDisplay = getAbilityDisplay(ability);
        boolean inAbilityBlock = false;
        boolean skipBlock = false;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            
            // Kiểm tra separator
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                if (!inAbilityBlock) {
                    // Bắt đầu block ability mới
                    inAbilityBlock = true;
                    // Kiểm tra dòng tiếp theo có phải ability cần xóa không
                    if (i + 1 < lore.size() && lore.get(i + 1).contains(abilityDisplay)) {
                        skipBlock = true;
                        continue;
                    } else {
                        // Không phải ability cần xóa, giữ separator
                        newLore.add(line);
                        continue;
                    }
                } else {
                    // Kết thúc block ability
                    inAbilityBlock = false;
                    if (skipBlock) {
                        skipBlock = false;
                        continue;
                    }
                    newLore.add(line);
                    continue;
                }
            }
            
            // Nếu đang trong block và đang skip
            if (inAbilityBlock && skipBlock) {
                continue;
            }
            
            // Thêm dòng bình thường
            newLore.add(line);
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
        
        player.sendMessage(ChatColor.GREEN + "✓ Đã thêm ability " + ability + " với giá trị " + value + unit);
        return true;
    }
    
    private boolean handleRemove(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Vui lòng cầm item trên tay!");
            return true;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            player.sendMessage(ChatColor.RED + "Item này không có ability!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        // Xác định ability cần xóa
        String targetAbility = null;
        if (args.length >= 2) {
            targetAbility = args[1].toLowerCase();
            if (!abilities.contains(targetAbility)) {
                player.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
                player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock");
                return true;
            }
        }
        
        boolean removed = false;
        boolean inAbilityBlock = false;
        boolean skipBlock = false;
        String abilityDisplay = targetAbility != null ? getAbilityDisplay(targetAbility) : null;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            
            // Kiểm tra separator
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                if (!inAbilityBlock) {
                    // Bắt đầu block ability mới
                    inAbilityBlock = true;
                    // Kiểm tra dòng tiếp theo có phải ability cần xóa không
                    if (i + 1 < lore.size()) {
                        if (targetAbility == null) {
                            // Xóa tất cả ability
                            skipBlock = true;
                            removed = true;
                            continue;
                        } else if (lore.get(i + 1).contains(abilityDisplay)) {
                            // Xóa ability cụ thể
                            skipBlock = true;
                            removed = true;
                            continue;
                        }
                    }
                    // Không phải ability cần xóa, giữ separator
                    newLore.add(line);
                    continue;
                } else {
                    // Kết thúc block ability
                    inAbilityBlock = false;
                    if (skipBlock) {
                        skipBlock = false;
                        continue;
                    }
                    newLore.add(line);
                    continue;
                }
            }
            
            // Nếu đang trong block và đang skip
            if (inAbilityBlock && skipBlock) {
                continue;
            }
            
            // Thêm dòng bình thường
            newLore.add(line);
        }
        
        if (!removed) {
            if (targetAbility != null) {
                player.sendMessage(ChatColor.RED + "Item này không có ability " + targetAbility + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Item này không có ability!");
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
        
        if (finalLore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(finalLore);
        }
        
        item.setItemMeta(meta);
        
        if (targetAbility != null) {
            player.sendMessage(ChatColor.GREEN + "✓ Đã xóa ability " + targetAbility + " khỏi item!");
        } else {
            player.sendMessage(ChatColor.GREEN + "✓ Đã xóa tất cả ability khỏi item!");
        }
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== SchoolItem Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/si add <ability> <value> - Thêm ability");
        sender.sendMessage(ChatColor.YELLOW + "  ability: pve, pvp, multiplierblock");
        sender.sendMessage(ChatColor.YELLOW + "  Ví dụ: /si add pve 50");
        sender.sendMessage(ChatColor.YELLOW + "/si remove [ability] - Xóa ability");
        sender.sendMessage(ChatColor.YELLOW + "  Ví dụ: /si remove pve");
        sender.sendMessage(ChatColor.YELLOW + "  Hoặc: /si remove (xóa tất cả)");
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
            // Tab complete cho subcommand
            List<String> subCommands = Arrays.asList("add", "remove");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            // Tab complete cho ability
            for (String ability : abilities) {
                if (ability.startsWith(args[1].toLowerCase())) {
                    completions.add(ability);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Tab complete cho ability khi remove
            for (String ability : abilities) {
                if (ability.startsWith(args[1].toLowerCase())) {
                    completions.add(ability);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            // Tab complete cho giá trị
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