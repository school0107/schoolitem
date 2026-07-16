package com.schoolitem.commands;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.utils.ColorUtils;
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
    
    private final SchoolItem plugin;
    private final PluginConfig config;
    private final List<String> abilities = Arrays.asList(
        "pve", "pvp", "multiplierblock", 
        "lifesteal", "thorns", "hungersteal", "wound", "sweepattack"
    );
    
    public MainCommand(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        if (!player.hasPermission("schoolitem.admin")) {
            sender.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&cBạn không có quyền sử dụng lệnh này!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&a✓ Config đã được reload!"));
            return true;
        } else if (subCommand.equals("add")) {
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
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock, lifesteal, thorns, hungersteal, wound, sweepattack");
            player.sendMessage(ChatColor.YELLOW + "Ví dụ: /si add sweepattack 10");
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
        
        if (!config.isAbilityEnabled(ability)) {
            player.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&cAbility " + ability + " đã bị tắt trong config!"));
            return true;
        }
        
        double minValue = config.getAbilityMin(ability);
        double maxValue = config.getAbilityMax(ability);
        
        if (value < minValue || value > maxValue) {
            player.sendMessage(ChatColor.RED + "Giá trị phải từ " + minValue + " đến " + maxValue + "!");
            return true;
        }
        
        if (!abilities.contains(ability)) {
            player.sendMessage(ChatColor.RED + "Ability không hợp lệ!");
            player.sendMessage(ChatColor.YELLOW + "Các ability: pve, pvp, multiplierblock, lifesteal, thorns, hungersteal, wound, sweepattack");
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
        String abilityDisplay = config.getAbilityDisplayName(ability);
        boolean skip = false;
        
        for (String line : lore) {
            if (line.contains(abilityDisplay)) {
                skip = true;
                continue;
            }
            if (skip) {
                skip = false;
                continue;
            }
            newLore.add(line);
        }
        
        // Thêm ability mới với RGB color
        String color = config.getAbilityColor(ability);
        String emoji = config.getAbilityEmoji(ability);
        String displayName = config.getAbilityDisplayName(ability);
        String unit = config.getAbilityUnit(ability);
        
        // Tạo lore với RGB color
        String loreLine = config.getLoreFormat()
                .replace("{color}", color)
                .replace("{emoji}", emoji)
                .replace("{display-name}", displayName)
                .replace("{value}", String.valueOf(value))
                .replace("{unit}", unit);
        
        // Thêm tỉ lệ nếu có
        double chance = config.getAbilityChance(ability);
        if (chance < 100) {
            loreLine += " &7| &fTỉ lệ: " + color + (int) chance + "%";
        }
        
        // Thêm thời gian nếu có
        int duration = config.getAbilityDuration(ability);
        if (duration > 0 && (ability.equals("wound") || ability.equals("sweepattack"))) {
            loreLine += " &7| &fCooldown: " + color + duration + "s";
        }
        
        // Colorize với RGB
        newLore.add(ColorUtils.colorize(loreLine));
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        
        player.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&a✓ Đã thêm ability " + ability + " với giá trị " + value + unit));
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
                return true;
            }
        }
        
        boolean removed = false;
        String abilityDisplay = targetAbility != null ? config.getAbilityDisplayName(targetAbility) : null;
        
        for (String line : lore) {
            if (targetAbility == null) {
                removed = true;
                continue;
            } else if (line.contains(abilityDisplay)) {
                removed = true;
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
        
        if (newLore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(newLore);
        }
        
        item.setItemMeta(meta);
        
        if (targetAbility != null) {
            player.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&a✓ Đã xóa ability " + targetAbility + " khỏi item!"));
        } else {
            player.sendMessage(ColorUtils.colorize(config.getMessagePrefix() + "&a✓ Đã xóa tất cả ability khỏi item!"));
        }
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6===== SchoolItem Help ====="));
        sender.sendMessage(ColorUtils.colorize("&e/si add <ability> <value> - Thêm ability"));
        sender.sendMessage(ColorUtils.colorize("&e  ability: pve, pvp, multiplierblock"));
        sender.sendMessage(ColorUtils.colorize("&e           lifesteal, thorns, hungersteal, wound, sweepattack"));
        sender.sendMessage(ColorUtils.colorize("&e  Ví dụ: /si add sweepattack 10"));
        sender.sendMessage(ColorUtils.colorize("&e/si remove [ability] - Xóa ability"));
        sender.sendMessage(ColorUtils.colorize("&e  Ví dụ: /si remove sweepattack"));
        sender.sendMessage(ColorUtils.colorize("&e/si reload - Reload config"));
        sender.sendMessage(ColorUtils.colorize("&b🌊 Sweep Attack: Chém không khí 10% tạo sóng sát thương"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("add", "remove", "reload");
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
                       ability.equals("hungersteal") || ability.equals("wound") || 
                       ability.equals("sweepattack")) {
                completions.add("10");
                completions.add("20");
                completions.add("30");
                completions.add("50");
            }
        }
        
        return completions;
    }
}