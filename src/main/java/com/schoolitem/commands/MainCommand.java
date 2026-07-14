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
    
    private final List<String> abilities = Arrays.asList(
        "pve", "pvp", "multiplierblock", 
        "lifesteal", "thorns", "hungersteal", "wound"
    );
    
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
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock, lifesteal, thorns, hungersteal, wound");
            player.sendMessage(ChatColor.YELLOW + "Ví dụ: /si add wound 30");
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
        
        // Giới hạn giá trị cho từng ability
        if (ability.equals("lifesteal") || ability.equals("thorns") || 
            ability.equals("hungersteal") || ability.equals("wound")) {
            if (value > 100) {
                player.sendMessage(ChatColor.RED + "Giá trị tối đa là 100%!");
                return true;
            }
        }
        
        if (!abilities.contains(ability)) {
            player.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock, lifesteal, thorns, hungersteal, wound");
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
            
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                if (!inAbilityBlock) {
                    inAbilityBlock = true;
                    if (i + 1 < lore.size() && lore.get(i + 1).contains(abilityDisplay)) {
                        skipBlock = true;
                        continue;
                    } else {
                        newLore.add(line);
                        continue;
                    }
                } else {
                    inAbilityBlock = false;
                    if (skipBlock) {
                        skipBlock = false;
                        continue;
                    }
                    newLore.add(line);
                    continue;
                }
            }
            
            if (inAbilityBlock && skipBlock) {
                continue;
            }
            
            newLore.add(line);
        }
        
        // Thêm ability mới
        String color = getAbilityColor(ability);
        String emoji = getAbilityEmoji(ability);
        String displayName = getAbilityDisplay(ability);
        String unit = ability.equals("multiplierblock") ? "x" : "%";
        String description = getAbilityDescription(ability, value);
        
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
        
        String targetAbility = null;
        if (args.length >= 2) {
            targetAbility = args[1].toLowerCase();
            if (!abilities.contains(targetAbility)) {
                player.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
                player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock, lifesteal, thorns, hungersteal, wound");
                return true;
            }
        }
        
        boolean removed = false;
        boolean inAbilityBlock = false;
        boolean skipBlock = false;
        String abilityDisplay = targetAbility != null ? getAbilityDisplay(targetAbility) : null;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            
            if (line.contains("§m--------------------------------") || line.contains("&7&m--------------------------------")) {
                if (!inAbilityBlock) {
                    inAbilityBlock = true;
                    if (i + 1 < lore.size()) {
                        if (targetAbility == null) {
                            skipBlock = true;
                            removed = true;
                            continue;
                        } else if (lore.get(i + 1).contains(abilityDisplay)) {
                            skipBlock = true;
                            removed = true;
                            continue;
                        }
                    }
                    newLore.add(line);
                    continue;
                } else {
                    inAbilityBlock = false;
                    if (skipBlock) {
                        skipBlock = false;
                        continue;
                    }
                    newLore.add(line);
                    continue;
                }
            }
            
            if (inAbilityBlock && skipBlock) {
                continue;
            }
            
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
        sender.sendMessage(ChatColor.YELLOW + "           lifesteal, thorns, hungersteal, wound");
        sender.sendMessage(ChatColor.YELLOW + "  Ví dụ: /si add wound 30");
        sender.sendMessage(ChatColor.YELLOW + "/si remove [ability] - Xóa ability");
        sender.sendMessage(ChatColor.YELLOW + "  Ví dụ: /si remove wound");
        sender.sendMessage(ChatColor.YELLOW + "  Hoặc: /si remove (xóa tất cả)");
    }
    
    private String getAbilityDisplay(String ability) {
        switch (ability) {
            case "pve": return "Giảm Sát Thương PVE";
            case "pvp": return "Giảm Sát Thương PVP";
            case "multiplierblock": return "Nhân Block";
            case "lifesteal": return "Hút Máu";
            case "thorns": return "Phản Sát Thương";
            case "hungersteal": return "Hút Thanh Thức Ăn";
            case "wound": return "Vết Thương";
            default: return ability;
        }
    }
    
    private String getAbilityColor(String ability) {
        switch (ability) {
            case "pve": return "§c";
            case "pvp": return "§6";
            case "multiplierblock": return "§a";
            case "lifesteal": return "§d";
            case "thorns": return "§4";
            case "hungersteal": return "§e";
            case "wound": return "§5";
            default: return "§f";
        }
    }
    
    private String getAbilityEmoji(String ability) {
        switch (ability) {
            case "pve": return "⚔️";
            case "pvp": return "🛡️";
            case "multiplierblock": return "⛏️";
            case "lifesteal": return "❤️";
            case "thorns": return "🌵";
            case "hungersteal": return "🍖";
            case "wound": return "🩸";
            default: return "✦";
        }
    }
    
    private String getAbilityDescription(String ability, double value) {
        switch (ability) {
            case "pve": return "Giảm " + value + "% sát thương từ quái vật";
            case "pvp": return "Giảm " + value + "% sát thương từ người chơi";
            case "multiplierblock": return "Nhân " + value + "x số lượng block";
            case "lifesteal": return "Hút " + value + "% sát thương gây ra thành máu";
            case "thorns": return "Phản " + value + "% sát thương nhận vào";
            case "hungersteal": return "Hút " + value + "% thanh thức ăn của đối phương";
            case "wound": return "Giảm " + value + "% khả năng hồi máu/hút máu của đối phương trong 10s (10% tỉ lệ)";
            default: return "";
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
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
            } else if (ability.equals("lifesteal") || ability.equals("thorns") || 
                       ability.equals("hungersteal") || ability.equals("wound")) {
                completions.add("20");
                completions.add("30");
                completions.add("50");
                completions.add("75");
            }
        }
        
        return completions;
    }
}