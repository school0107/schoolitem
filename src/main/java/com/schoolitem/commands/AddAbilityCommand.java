package com.schoolitem.commands;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.utils.ItemUtils;
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
    private final PluginConfig config;
    
    public AddAbilityCommand(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        if (!player.hasPermission("schoolitem.admin")) {
            player.sendMessage(config.getMessagePrefix() + "§cBạn không có quyền sử dụng lệnh này!");
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
            player.sendMessage(config.getMessagePrefix() + "§cGiá trị phải là số!");
            return true;
        }
        
        // Check if ability is enabled
        if (!isAbilityEnabled(ability)) {
            player.sendMessage(config.getMessagePrefix() + "§cAbility " + ability + " đã bị tắt trong config!");
            return true;
        }
        
        // Check value limits from config
        double minValue = config.getAbilityMin(ability);
        double maxValue = config.getAbilityMax(ability);
        
        if (value < minValue || value > maxValue) {
            player.sendMessage(config.getMessagePrefix() + "§cGiá trị phải từ " + minValue + " đến " + maxValue + "!");
            return true;
        }
        
        // Check item filter
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(config.getMessagePrefix() + "§cVui lòng cầm item trên tay!");
            return true;
        }
        
        if (!isItemAllowed(item)) {
            player.sendMessage(config.getMessagePrefix() + "§cItem này không được phép thêm ability!");
            return true;
        }
        
        // Check disabled worlds
        if (isWorldDisabled(player)) {
            player.sendMessage(config.getMessagePrefix() + "§cWorld này không cho phép sử dụng ability!");
            return true;
        }
        
        // Add ability to item
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Remove old ability if exists
        List<String> newLore = new ArrayList<>();
        boolean skip = false;
        for (String line : lore) {
            if (line.contains(config.getLoreSeparator())) {
                skip = true;
                continue;
            }
            if (skip) {
                skip = false;
                continue;
            }
            if (line.contains(config.getAbilityDisplayName(ability))) {
                continue;
            }
            newLore.add(line);
        }
        lore = newLore;
        
        // Add new ability lore
        String displayName = config.getAbilityDisplayName(ability);
        String color = config.getAbilityColor(ability);
        String emoji = config.getAbilityEmoji(ability);
        String unit = config.getAbilityUnit(ability);
        String description = config.getConfig().getString("abilities." + ability + ".description", "");
        description = description.replace("{value}", String.valueOf(value));
        
        String loreFormat = config.getLoreFormat()
                .replace("{color}", color)
                .replace("{emoji}", emoji)
                .replace("{display-name}", displayName)
                .replace("{value}", String.valueOf(value))
                .replace("{unit}", unit);
        
        String loreDesc = config.getLoreDescription()
                .replace("{description}", description);
        
        lore.add(ItemUtils.colorize(config.getLoreSeparator()));
        lore.add(ItemUtils.colorize(loreFormat));
        lore.add(ItemUtils.colorize(loreDesc));
        lore.add(ItemUtils.colorize(config.getLoreSeparator()));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage(config.getMessagePrefix() + "§a✓ Đã thêm ability " + ability + " với giá trị " + value + unit + " vào item!");
        return true;
    }
    
    private boolean isAbilityEnabled(String ability) {
        switch (ability) {
            case "pve": return config.isEnablePve();
            case "pvp": return config.isEnablePvp();
            case "multiplierblock": return config.isEnableMultiplierBlock();
            default: return false;
        }
    }
    
    private boolean isItemAllowed(ItemStack item) {
        boolean whitelistMode = config.getConfig().getBoolean("item-filter.whitelist-mode", true);
        List<String> allowedItems = config.getConfig().getStringList("item-filter.allowed-items");
        List<String> blockedItems = config.getConfig().getStringList("item-filter.blocked-items");
        
        String itemName = item.getType().name();
        
        // Check blocked items first
        if (blockedItems.contains(itemName)) {
            return false;
        }
        
        // Check whitelist
        if (whitelistMode) {
            return allowedItems.contains(itemName);
        } else {
            return !allowedItems.contains(itemName);
        }
    }
    
    private boolean isWorldDisabled(Player player) {
        String worldName = player.getWorld().getName();
        return config.getDisabledWorlds().contains(worldName);
    }
}
