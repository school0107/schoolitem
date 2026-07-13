package com.schoolitem;

import com.schoolitem.commands.AddAbilityCommand;
import com.schoolitem.commands.RemoveAbilityCommand;
import com.schoolitem.listeners.DamageListener;
import com.schoolitem.listeners.BlockBreakListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SchoolItem extends JavaPlugin {
    private static SchoolItem instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Register commands
        getCommand("si").setExecutor(new AddAbilityCommand(this));
        getCommand("si").setExecutor(new RemoveAbilityCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        
        getLogger().info("§aSchoolItem plugin đã được kích hoạt!");
        getLogger().info("§eSử dụng /si add <ability> <value> để thêm ability");
        getLogger().info("§eSử dụng /si remove để xóa ability");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cSchoolItem plugin đã được tắt!");
    }

    public static SchoolItem getInstance() {
        return instance;
    }
}
