package com.schoolitem.utils;

import com.schoolitem.SchoolItem;
import org.bukkit.ChatColor;

import java.io.InputStream;
import java.util.Properties;

public class VersionUtils {
    
    public static String getVersionInfo() {
        try {
            Properties props = new Properties();
            InputStream input = SchoolItem.getInstance().getResource("version.properties");
            if (input != null) {
                props.load(input);
                input.close();
                
                String version = props.getProperty("version", "1.0.0");
                String build = props.getProperty("build.number", "0");
                String time = props.getProperty("build.time", "Unknown");
                String commit = props.getProperty("commit.sha", "Unknown");
                String branch = props.getProperty("branch", "Unknown");
                
                return ChatColor.GREEN + "SchoolItem v" + version + 
                       ChatColor.GRAY + " (Build #" + build + ")" +
                       ChatColor.DARK_GRAY + "\n  ├─ " + ChatColor.GRAY + "Time: " + time +
                       ChatColor.DARK_GRAY + "\n  ├─ " + ChatColor.GRAY + "Branch: " + branch +
                       ChatColor.DARK_GRAY + "\n  └─ " + ChatColor.GRAY + "Commit: " + commit.substring(0, 7);
            }
        } catch (Exception e) {
            // Ignore
        }
        return ChatColor.RED + "SchoolItem - Version Unknown";
    }
}
