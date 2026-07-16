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
    private Map<String, Boolean> abilityEnabled;
    private Map<String, Double> abilityMin;
    private Map<String, Double> abilityMax;
    private Map<String, Double> abilityChance;
    private Map<String, Integer> abilityDuration;
    private Map<String, String> abilityColor;
    private String messagePrefix;
    private boolean soundEffects;
    private boolean particleEffects;
    private List<String> disabledWorlds;
    private String loreFormat;
    private Map<String, String> colors;
    private Map<String, Map<String, String>> sounds;

    public PluginConfig(SchoolItem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.abilityEnabled = new HashMap<>();
        this.abilityMin = new HashMap<>();
        this.abilityMax = new HashMap<>();
        this.abilityChance = new HashMap<>();
        this.abilityDuration = new HashMap<>();
        this.abilityColor = new HashMap<>();
        this.colors = new HashMap<>();
        this.sounds = new HashMap<>();
        reloadConfig();
    }

    public void reloadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load ability settings
        String[] abilities = {"pve", "pvp", "multiplierblock", "lifesteal", "thorns", "hungersteal", "wound"};
        
        for (String ability : abilities) {
            abilityEnabled.put(ability, config.getBoolean("abilities." + ability + ".enabled", true));
            abilityMin.put(ability, config.getDouble("abilities." + ability + ".min-value", 0.0));
            abilityMax.put(ability, config.getDouble("abilities." + ability + ".max-value", 100.0));
            abilityChance.put(ability, config.getDouble("abilities." + ability + ".chance", 100.0));
            abilityDuration.put(ability, config.getInt("abilities." + ability + ".duration", 10));
            abilityColor.put(ability, config.getString("abilities." + ability + ".color", "&f"));
        }

        // Load settings
        messagePrefix = config.getString("settings.message-prefix", "&#FFD700[SchoolItem] &r");
        soundEffects = config.getBoolean("settings.sound-effects", true);
        particleEffects = config.getBoolean("settings.particle-effects", true);
        disabledWorlds = config.getStringList("settings.disabled-worlds");
        
        // Load lore format
        loreFormat = config.getString("lore-format", "{emoji} {color}{display-name} &7| &fGiá trị: {color}{value}{unit}");

        // Load colors
        colors.put("primary", config.getString("colors.primary", "&#FFD700"));
        colors.put("secondary", config.getString("colors.secondary", "&#FF6B6B"));
        colors.put("success", config.getString("colors.success", "&#69DB7C"));
        colors.put("warning", config.getString("colors.warning", "&#FFA94D"));
        colors.put("error", config.getString("colors.error", "&#FF6B6B"));
        colors.put("info", config.getString("colors.info", "&#74C0FC"));
        colors.put("highlight", config.getString("colors.highlight", "&#DA77F2"));

        // Load sounds
        String[] soundAbilities = {"lifesteal", "thorns", "hungersteal", "wound"};
        for (String ability : soundAbilities) {
            Map<String, String> soundMap = new HashMap<>();
            soundMap.put("attacker", config.getString("sounds." + ability + ".attacker", ""));
            soundMap.put("target", config.getString("sounds." + ability + ".target", ""));
            sounds.put(ability, soundMap);
        }
    }

    public boolean isAbilityEnabled(String ability) {
        return abilityEnabled.getOrDefault(ability, true);
    }

    public double getAbilityMin(String ability) {
        return abilityMin.getOrDefault(ability, 0.0);
    }

    public double getAbilityMax(String ability) {
        return abilityMax.getOrDefault(ability, 100.0);
    }

    public double getAbilityChance(String ability) {
        return abilityChance.getOrDefault(ability, 100.0);
    }

    public int getAbilityDuration(String ability) {
        return abilityDuration.getOrDefault(ability, 0);
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public boolean isSoundEffects() {
        return soundEffects;
    }

    public boolean isParticleEffects() {
        return particleEffects;
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public String getLoreFormat() {
        return loreFormat;
    }

    public String getAbilityDisplayName(String ability) {
        return config.getString("abilities." + ability + ".display-name", ability);
    }

    public String getAbilityColor(String ability) {
        return abilityColor.getOrDefault(ability, "&f");
    }

    public String getAbilityEmoji(String ability) {
        return config.getString("abilities." + ability + ".emoji", "✦");
    }

    public String getAbilityUnit(String ability) {
        return config.getString("abilities." + ability + ".unit", "");
    }

    public String getColor(String key) {
        return colors.getOrDefault(key, "&f");
    }

    public String getSound(String ability, String type) {
        Map<String, String> soundMap = sounds.get(ability);
        if (soundMap == null) return "";
        return soundMap.getOrDefault(type, "");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}