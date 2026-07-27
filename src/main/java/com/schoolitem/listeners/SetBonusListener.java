package com.schoolitem.listeners;

import com.schoolitem.SchoolItem;
import com.schoolitem.config.PluginConfig;
import com.schoolitem.utils.ColorUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SetBonusListener implements Listener {
    private final SchoolItem plugin;
    private final PluginConfig config;
    private final Map<UUID, ActiveSet> activeSets = new HashMap<>();
    private final Map<UUID, Long> lastCheckTime = new HashMap<>();
    private final Map<String, SetBonusData> setDataMap = new HashMap<>();
    
    public SetBonusListener(SchoolItem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        loadSetData();
        
        // Kiểm tra định kỳ mỗi 3 giây
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkAndApplySetBonus(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 60L); // 3 giây
    }
    
    private void loadSetData() {
        ConfigurationSection setSection = config.getConfig().getConfigurationSection("setbonus");
        if (setSection == null) return;
        
        for (String setKey : setSection.getKeys(false)) {
            if (setKey.equals("enabled")) continue;
            
            String name = setSection.getString(setKey + ".name", setKey);
            String color = setSection.getString(setKey + ".color", "&f");
            String emoji = setSection.getString(setKey + ".emoji", "✦");
            int required = setSection.getInt(setKey + ".required", 4);
            String particle = setSection.getString(setKey + ".particle", "TOTEM");
            String sound = setSection.getString(setKey + ".sound", "ENTITY_PLAYER_LEVELUP");
            
            // Lấy buffs
            ConfigurationSection buffsSection = setSection.getConfigurationSection(setKey + ".buffs");
            Map<String, Double> buffs = new HashMap<>();
            if (buffsSection != null) {
                for (String buffKey : buffsSection.getKeys(false)) {
                    buffs.put(buffKey, buffsSection.getDouble(buffKey, 0.0));
                }
            }
            
            SetBonusData data = new SetBonusData(
                setKey, name, color, emoji, required, 
                buffs, particle, sound
            );
            setDataMap.put(setKey, data);
        }
        
        plugin.getLogger().info("§aĐã tải " + setDataMap.size() + " Set Bonus!");
    }
    
    public void reload() {
        setDataMap.clear();
        loadSetData();
        // Kiểm tra lại cho tất cả player
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkAndApplySetBonus(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delay 1 tick để load đầy đủ
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndApplySetBonus(event.getPlayer());
            }
        }.runTask(plugin);
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndApplySetBonus(event.getPlayer());
            }
        }.runTask(plugin);
    }
    
    private void checkAndApplySetBonus(Player player) {
        if (!config.isAbilityEnabled("setbonus")) return;
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Chống kiểm tra quá thường xuyên (0.5s)
        if (lastCheckTime.containsKey(playerId) && 
            (currentTime - lastCheckTime.get(playerId)) < 500) {
            return;
        }
        lastCheckTime.put(playerId, currentTime);
        
        // Đếm số item set bonus
        Map<String, Integer> setCounts = countSetBonusItems(player);
        
        // Tìm set đang kích hoạt
        String activeSetKey = null;
        ActiveSet activeSet = activeSets.get(playerId);
        
        for (Map.Entry<String, SetBonusData> entry : setDataMap.entrySet()) {
            String setKey = entry.getKey();
            SetBonusData data = entry.getValue();
            
            if (setCounts.containsKey(setKey) && setCounts.get(setKey) >= data.required) {
                activeSetKey = setKey;
                break;
            }
        }
        
        // Nếu có set mới hoặc set thay đổi
        if (activeSetKey != null) {
            SetBonusData data = setDataMap.get(activeSetKey);
            if (activeSet == null || !activeSet.setKey.equals(activeSetKey)) {
                // Hủy set cũ
                if (activeSet != null) {
                    deactivateSet(player, activeSet);
                }
                // Kích hoạt set mới
                activateSet(player, data, setCounts.get(activeSetKey));
                activeSets.put(playerId, new ActiveSet(activeSetKey, data, setCounts.get(activeSetKey)));
            }
        } else {
            // Hủy tất cả set
            if (activeSet != null) {
                deactivateSet(player, activeSet);
                activeSets.remove(playerId);
            }
        }
    }
    
    private Map<String, Integer> countSetBonusItems(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        
        // Kiểm tra tất cả item trong inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
                String setKey = getSetKeyFromItem(item);
                if (setKey != null) {
                    counts.put(setKey, counts.getOrDefault(setKey, 0) + 1);
                }
            }
        }
        
        // Kiểm tra giáp
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir() && armor.hasItemMeta()) {
                String setKey = getSetKeyFromItem(armor);
                if (setKey != null) {
                    counts.put(setKey, counts.getOrDefault(setKey, 0) + 1);
                }
            }
        }
        
        return counts;
    }
    
    private String getSetKeyFromItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return null;
        
        for (String line : meta.getLore()) {
            // Kiểm tra từng set
            for (String setKey : setDataMap.keySet()) {
                SetBonusData data = setDataMap.get(setKey);
                if (line.contains(data.displayName) || line.contains(data.name)) {
                    return setKey;
                }
            }
        }
        return null;
    }
    
    private void activateSet(Player player, SetBonusData data, int count) {
        // Hiển thị thông báo
        player.sendMessage(ColorUtils.colorize(
            "&6&m========================================"
        ));
        player.sendMessage(ColorUtils.colorize(
            config.getMessagePrefix() + data.color + data.emoji + " " + 
            data.name + " &ađã được kích hoạt! (&e" + count + "&a/" + data.required + " items)"
        ));
        
        // Hiển thị buffs
        for (Map.Entry<String, Double> buff : data.buffs.entrySet()) {
            String buffName = getBuffDisplayName(buff.getKey());
            player.sendMessage(ColorUtils.colorize(
                "  &7✦ &f" + buffName + ": &a+" + buff.getValue() + "%"
            ));
        }
        player.sendMessage(ColorUtils.colorize(
            "&6&m========================================"
        ));
        
        // Âm thanh
        try {
            player.playSound(player.getLocation(), Sound.valueOf(data.sound), 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        // Hiệu ứng hạt
        try {
            Particle particle = Particle.valueOf(data.particle);
            player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
            );
        } catch (Exception e) {
            player.getWorld().spawnParticle(
                Particle.TOTEM,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
            );
        }
        
        // Áp dụng Potion Effect nếu có Speed
        if (data.buffs.containsKey("speed") && data.buffs.get("speed") > 0) {
            int duration = 20 * 60 * 5; // 5 phút
            int amplifier = (int) Math.floor(data.buffs.get("speed") / 10) - 1;
            if (amplifier < 0) amplifier = 0;
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, duration, amplifier
            ));
        }
    }
    
    private void deactivateSet(Player player, ActiveSet activeSet) {
        player.sendMessage(ColorUtils.colorize(
            config.getMessagePrefix() + "&c" + activeSet.data.emoji + " " + 
            activeSet.data.name + " &cđã bị hủy!"
        ));
        
        // Xóa Potion Effect Speed
        if (activeSet.data.buffs.containsKey("speed")) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }
    
    public SetBonusData getActiveSetData(Player player) {
        ActiveSet active = activeSets.get(player.getUniqueId());
        return active != null ? active.data : null;
    }
    
    public String getActiveSetKey(Player player) {
        ActiveSet active = activeSets.get(player.getUniqueId());
        return active != null ? active.setKey : null;
    }
    
    public double getBuffValue(Player player, String buffKey) {
        SetBonusData data = getActiveSetData(player);
        if (data == null) return 0;
        return data.buffs.getOrDefault(buffKey, 0.0);
    }
    
    private String getBuffDisplayName(String key) {
        switch (key) {
            case "damage": return "Sát thương";
            case "health": return "Máu tối đa";
            case "thorns": return "Phản đòn";
            case "lifesteal": return "Hút máu";
            case "hungersteal": return "Hút thức ăn";
            case "speed": return "Tốc độ";
            default: return key;
        }
    }
    
    // Lớp dữ liệu Set Bonus
    public static class SetBonusData {
        public final String setKey;
        public final String name;
        public final String color;
        public final String emoji;
        public final String displayName;
        public final int required;
        public final Map<String, Double> buffs;
        public final String particle;
        public final String sound;
        
        public SetBonusData(String setKey, String name, String color, String emoji, 
                           int required, Map<String, Double> buffs, String particle, String sound) {
            this.setKey = setKey;
            this.name = ColorUtils.colorize(name);
            this.color = color;
            this.emoji = emoji;
            this.displayName = ColorUtils.colorize(color + emoji + " " + name);
            this.required = required;
            this.buffs = buffs;
            this.particle = particle;
            this.sound = sound;
        }
    }
    
    // Lớp Set đang active
    public static class ActiveSet {
        public final String setKey;
        public final SetBonusData data;
        public final int count;
        
        public ActiveSet(String setKey, SetBonusData data, int count) {
            this.setKey = setKey;
            this.data = data;
            this.count = count;
        }
    }
}