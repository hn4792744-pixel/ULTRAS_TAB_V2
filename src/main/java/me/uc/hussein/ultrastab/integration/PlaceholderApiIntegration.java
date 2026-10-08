package me.uc.hussein.ultrastab.integration;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Optional PlaceholderAPI bridge, called through reflection so PlaceholderAPI is neither a build nor a
 * load-time dependency. If it is missing or fails, text is returned unchanged.
 */
public final class PlaceholderApiIntegration {

    private final UltrasTabPlugin plugin;
    private boolean enabled = true;
    private Method setPlaceholders;
    private boolean failed;

    public PlaceholderApiIntegration(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(boolean enabled) {
        this.enabled = enabled;
        this.setPlaceholders = null;
        this.failed = false;
    }

    public boolean available() {
        return enabled && !failed && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String apply(Player player, String text) {
        if (!available()) {
            return text;
        }
        try {
            if (setPlaceholders == null) {
                Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, plugin.getClass().getClassLoader());
                setPlaceholders = api.getMethod("setPlaceholders", Player.class, String.class);
            }
            Object result = setPlaceholders.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (ReflectiveOperationException | LinkageError ex) {
            failed = true;
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI integration disabled: " + ex.getMessage());
            return text;
        }
    }
}
