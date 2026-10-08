package me.uc.hussein.ultrastab.util;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/** Plays the quiet UI sounds. Respects the global switch and each player's own sound setting. */
public final class Sounds {

    private record Fx(String key, float volume, float pitch) {
    }

    private final UltrasTabPlugin plugin;
    private final Map<String, Fx> effects = new HashMap<>();
    private boolean enabled = true;

    public Sounds(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration config) {
        effects.clear();
        enabled = config.getBoolean("sounds.enabled", true);
        ConfigurationSection section = config.getConfigurationSection("sounds.effects");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            String[] parts = section.getString(name, "").split(":");
            if (parts[0].isBlank()) {
                continue;
            }
            try {
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                effects.put(name, new Fx(parts[0].trim(), volume, pitch));
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid sound definition for '" + name + "' in config.yml");
            }
        }
    }

    public void play(Player player, String effect) {
        if (!enabled || !plugin.settings().get(player).flag("sounds")) {
            return;
        }
        Fx fx = effects.get(effect);
        if (fx != null) {
            player.playSound(player.getLocation(), fx.key(), SoundCategory.MASTER, fx.volume(), fx.pitch());
        }
    }
}
