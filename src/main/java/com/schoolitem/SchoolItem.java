package com.schoolitem;

import com.schoolitem.commands.MainCommand;
import com.schoolitem.listeners.DamageListener;
import com.schoolitem.listeners.BlockBreakListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SchoolItem extends JavaPlugin {
    private static SchoolItem instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Register command
        getCommand("si").setExecutor(new MainCommand());
        getCommand("si").setTabCompleter(new MainCommand());
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DamageListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        
        getLogger().info("§a=========================================");
        getLogger().info("§aSchoolItem plugin đã được kích hoạt!");
        getLogger().info("§aVersion: " + getDescription().getVersion());
        getLogger().info("§a=========================================");
        getLogger().info("§e/si add <ability> <value> - Thêm ability");
        getLogger().info("§e/si remove [ability] - Xóa ability");
        getLogger().info("§a=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cSchoolItem plugin đã được tắt!");
    }

    public static SchoolItem getInstance() {
        return instance;
    }
}