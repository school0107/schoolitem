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
        
        getCommand("si").setExecutor(new AddAbilityCommand());
        getCommand("si").setExecutor(new RemoveAbilityCommand());
        
        getServer().getPluginManager().registerEvents(new DamageListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        
        getLogger().info("§a=========================================");
        getLogger().info("§aSchoolItem plugin đã được kích hoạt!");
        getLogger().info("§aVersion: " + getDescription().getVersion());
        getLogger().info("§a=========================================");
        getLogger().info("§eSử dụng /si add <ability> <value> để thêm ability");
        getLogger().info("§eSử dụng /si remove để xóa ability");
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