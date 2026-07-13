package com.schoolitem;

import com.schoolitem.commands.AddAbilityCommand;
import com.schoolitem.commands.RemoveAbilityCommand;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.listeners.DamageListener;
import com.schoolitem.listeners.BlockBreakListener;
import com.schoolitem.utils.VersionUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class SchoolItem extends JavaPlugin {
    private static SchoolItem instance;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load config
        pluginConfig = new PluginConfig(this);
        
        // Register commands
        getCommand("si").setExecutor(new AddAbilityCommand(this));
        getCommand("si").setExecutor(new RemoveAbilityCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        
        // Show version info
        getLogger().info("§a=========================================");
        getLogger().info("§aSchoolItem plugin đã được kích hoạt!");
        getLogger().info(VersionUtils.getVersionInfo());
        getLogger().info("§a=========================================");
        getLogger().info("§eSử dụng /si add <ability> <value> để thêm ability");
        getLogger().info("§eSử dụng /si remove để xóa ability");
        getLogger().info("§a=========================================");
        getLogger().info("§7Config loaded: " + pluginConfig.getConfig().getKeys(false).size() + " sections");
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
