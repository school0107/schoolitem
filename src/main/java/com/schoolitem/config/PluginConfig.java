package com.schoolitem.config;

import com.schoolitem.SchoolItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final SchoolItem plugin;
    private FileConfiguration config;
    private File configFile;
    
    private boolean enablePve;
    private boolean enablePvp;
    private boolean enableMultiplierBlock;
    private String messagePrefix;
    private Map<String, Double> abilityLimits;
    private List<String> disabledWorlds;
    
    public PluginConfig(SchoolItem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.abilityLimits = new HashMap<>();
        reloadConfig();
    }
    
    public void reloadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load settings
        enablePve = config.getBoolean("abilities.pve.enabled", true);
        enablePvp = config.getBoolean("abilities.pvp.enabled", true);
        enableMultiplierBlock = config.getBoolean("abilities.multiplierblock.enabled", true);
        
        messagePrefix = config.getString("settings.message-prefix", "&8[&6SchoolItem&8] &r");
        
        abilityLimits.put("pve.min", config.getDouble("abilities.pve.min-value", 0.0));
        abilityLimits.put("pve.max", config.getDouble("abilities.pve.max-value", 100.0));
        abilityLimits.put("pvp.min", config.getDouble("abilities.pvp.min-value", 0.0));
        abilityLimits.put("pvp.max", config.getDouble("abilities.pvp.max-value", 100.0));
        abilityLimits.put("multiplierblock.min", config.getDouble("abilities.multiplierblock.min-value", 1.0));
        abilityLimits.put("multiplierblock.max", config.getDouble("abilities.multiplierblock.max-value", 100.0));
        
        disabledWorlds = config.getStringList("settings.disabled-worlds");
        if (disabledWorlds == null) {
            disabledWorlds = new ArrayList<>();
        }
    }
    
    public boolean isEnablePve() {
        return enablePve;
    }
    
    public boolean isEnablePvp() {
        return enablePvp;
    }
    
    public boolean isEnableMultiplierBlock() {
        return enableMultiplierBlock;
    }
    
    public String getMessagePrefix() {
        return messagePrefix;
    }
    
    public double getAbilityMin(String ability) {
        return abilityLimits.getOrDefault(ability + ".min", 0.0);
    }
    
    public double getAbilityMax(String ability) {
        return abilityLimits.getOrDefault(ability + ".max", 100.0);
    }
    
    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public String getAbilityDisplayName(String ability) {
        return config.getString("abilities." + ability + ".display-name", ability);
    }
    
    public String getAbilityColor(String ability) {
        return config.getString("abilities." + ability + ".color", "&f");
    }
    
    public String getAbilityEmoji(String ability) {
        return config.getString("abilities." + ability + ".emoji", "✦");
    }
    
    public String getAbilityUnit(String ability) {
        return config.getString("abilities." + ability + ".unit", "");
    }
    
    public String getLoreSeparator() {
        return config.getString("settings.lore-separator", "&7&m--------------------------------");
    }
    
    public String getLoreFormat() {
        return config.getString("settings.lore-format", "&{color}{emoji} {display-name} &fGiá trị: &e{value}{unit}");
    }
    
    public String getLoreDescription() {
        return config.getString("settings.lore-description", "&7✦ &f{description}");
    }
}