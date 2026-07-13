package com.schoolitem.config;

import com.schoolitem.SchoolItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final SchoolItem plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Các setting mặc định
    private boolean enablePve;
    private boolean enablePvp;
    private boolean enableMultiplierBlock;
    private boolean enableMessage;
    private String messagePrefix;
    private boolean enableParticleEffect;
    private boolean enableSoundEffect;
    private Map<String, Object> abilityLimits;
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
        
        enableMessage = config.getBoolean("settings.enable-message", true);
        messagePrefix = config.getString("settings.message-prefix", "&8[&6SchoolItem&8] &r");
        
        enableParticleEffect = config.getBoolean("settings.enable-particle-effect", true);
        enableSoundEffect = config.getBoolean("settings.enable-sound-effect", true);
        
        // Load ability limits
        abilityLimits.put("pve.min", config.getDouble("abilities.pve.min-value", 0.0));
        abilityLimits.put("pve.max", config.getDouble("abilities.pve.max-value", 100.0));
        abilityLimits.put("pvp.min", config.getDouble("abilities.pvp.min-value", 0.0));
        abilityLimits.put("pvp.max", config.getDouble("abilities.pvp.max-value", 100.0));
        abilityLimits.put("multiplierblock.min", config.getDouble("abilities.multiplierblock.min-value", 1.0));
        abilityLimits.put("multiplierblock.max", config.getDouble("abilities.multiplierblock.max-value", 100.0));
        
        disabledWorlds = config.getStringList("settings.disabled-worlds");
        
        // Save default config if needed
        if (config.get("settings") == null) {
            saveDefaultConfig();
        }
    }
    
    private void saveDefaultConfig() {
        config.set("abilities.pve.enabled", true);
        config.set("abilities.pve.min-value", 0.0);
        config.set("abilities.pve.max-value", 100.0);
        config.set("abilities.pve.display-name", "Giảm Sát Thương PVE");
        config.set("abilities.pve.color", "&#FF6B6B");
        config.set("abilities.pve.emoji", "⚔️");
        config.set("abilities.pve.unit", "%");
        
        config.set("abilities.pvp.enabled", true);
        config.set("abilities.pvp.min-value", 0.0);
        config.set("abilities.pvp.max-value", 100.0);
        config.set("abilities.pvp.display-name", "Giảm Sát Thương PVP");
        config.set("abilities.pvp.color", "&#FFA94D");
        config.set("abilities.pvp.emoji", "🛡️");
        config.set("abilities.pvp.unit", "%");
        
        config.set("abilities.multiplierblock.enabled", true);
        config.set("abilities.multiplierblock.min-value", 1.0);
        config.set("abilities.multiplierblock.max-value", 100.0);
        config.set("abilities.multiplierblock.display-name", "Nhân Block");
        config.set("abilities.multiplierblock.color", "&#69DB7C");
        config.set("abilities.multiplierblock.emoji", "⛏️");
        config.set("abilities.multiplierblock.unit", "x");
        
        config.set("settings.enable-message", true);
        config.set("settings.message-prefix", "&8[&6SchoolItem&8] &r");
        config.set("settings.enable-particle-effect", true);
        config.set("settings.enable-sound-effect", true);
        config.set("settings.disabled-worlds", List.of("world_nether", "world_the_end"));
        config.set("settings.lore-separator", "&7&m--------------------------------");
        config.set("settings.lore-format", "&{color}{emoji} {display-name} &fGiá trị: &e{value}{unit}");
        config.set("settings.lore-description", "&7✦ &f{description}");
        
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Không thể lưu config.yml!");
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
    
    public boolean isEnableMessage() {
        return enableMessage;
    }
    
    public String getMessagePrefix() {
        return messagePrefix;
    }
    
    public boolean isEnableParticleEffect() {
        return enableParticleEffect;
    }
    
    public boolean isEnableSoundEffect() {
        return enableSoundEffect;
    }
    
    public double getAbilityMin(String ability) {
        return (double) abilityLimits.getOrDefault(ability + ".min", 0.0);
    }
    
    public double getAbilityMax(String ability) {
        return (double) abilityLimits.getOrDefault(ability + ".max", 100.0);
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
