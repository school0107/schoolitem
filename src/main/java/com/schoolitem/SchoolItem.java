package com.schoolitem;

import com.schoolitem.commands.MainCommand;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.listeners.DamageListener;
import com.schoolitem.listeners.BlockBreakListener;
import com.schoolitem.listeners.SweepAttackListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SchoolItem extends JavaPlugin {
    private static SchoolItem instance;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load config
        pluginConfig = new PluginConfig(this);
        
        // Register command
        getCommand("si").setExecutor(new MainCommand(this));
        getCommand("si").setTabCompleter(new MainCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new SweepAttackListener(this), this);
        
        getLogger().info("§a=========================================");
        getLogger().info("§aSchoolItem plugin đã được kích hoạt!");
        getLogger().info("§aVersion: " + getDescription().getVersion());
        getLogger().info("§aLoaded " + pluginConfig.getConfig().getKeys(false).size() + " config sections");
        getLogger().info("§a=========================================");
        getLogger().info("§e/si add <ability> <value> - Thêm ability");
        getLogger().info("§e/si remove [ability] - Xóa ability");
        getLogger().info("§e/si reload - Reload config");
        getLogger().info("§a=========================================");
        getLogger().info("§b🌊 Sweep Attack: Chém không khí 10% tạo sóng sát thương");
        getLogger().info("§a=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cSchoolItem plugin đã được tắt!");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (pluginConfig != null) {
            pluginConfig.reloadConfig();
        }
        getLogger().info("§aConfig đã được reload!");
    }

    public static SchoolItem getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}