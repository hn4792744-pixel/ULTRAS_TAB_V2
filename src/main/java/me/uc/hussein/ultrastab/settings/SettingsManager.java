package me.uc.hussein.ultrastab.settings;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.LanguageManager;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Per-player settings cache (main thread only) with YAML persistence under data/&lt;uuid&gt;.yml.
 * Changes mark the player dirty; dirty files are written asynchronously by the shared scheduler
 * every few seconds, and synchronously on quit/disable so nothing is lost.
 * Also hosts the shared "change a setting" actions used by both commands and GUIs.
 */
public final class SettingsManager {

    private final UltrasTabPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerSettings> cache = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public SettingsManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data");
    }

    // ---------------------------------------------------------------- storage

    public PlayerSettings get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerSettings get(UUID id) {
        PlayerSettings cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        File file = new File(dir, id + ".yml");
        PlayerSettings loaded = file.isFile()
                ? PlayerSettings.fromYaml(YamlConfiguration.loadConfiguration(file), plugin.defaultLanguage())
                : new PlayerSettings(plugin.defaultLanguage());
        cache.put(id, loaded);
        return loaded;
    }

    private void markDirty(Player player) {
        dirty.add(player.getUniqueId());
    }

    /** Writes all dirty players asynchronously. Called from the shared scheduler. */
    public void flush() {
        if (dirty.isEmpty()) {
            return;
        }
        Map<UUID, String> snapshots = new HashMap<>();
        for (UUID id : dirty) {
            PlayerSettings s = cache.get(id);
            if (s != null) {
                snapshots.put(id, s.toYaml().saveToString());
            }
        }
        dirty.clear();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> snapshots.forEach(this::write));
    }

    /** Saves (synchronously) and forgets a player. */
    public void unload(UUID id) {
        PlayerSettings s = cache.remove(id);
        if (s != null && dirty.remove(id)) {
            write(id, s.toYaml().saveToString());
        }
    }

    public void saveAllNow() {
        for (UUID id : new ArrayList<>(dirty)) {
            PlayerSettings s = cache.get(id);
            if (s != null) {
                write(id, s.toYaml().saveToString());
            }
        }
        dirty.clear();
    }

    private void write(UUID id, String data) {
        try {
            Files.createDirectories(dir.toPath());
            Files.writeString(new File(dir, id + ".yml").toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save settings for " + id, ex);
        }
    }

    public void reset(Player player) {
        cache.put(player.getUniqueId(), new PlayerSettings(plugin.defaultLanguage()));
        markDirty(player);
        plugin.applyChange(player, "language");
        plugin.applyChange(player, "scoreboard-field");
        plugin.applyChange(player, "tab");
        plugin.applyChange(player, "nametag-rank");
        plugin.applyChange(player, "nametag-ranks");
    }

    // ---------------------------------------------------------------- shared actions

    public void toggle(Player player, String key) {
        PlayerSettings s = get(player);
        boolean value = !s.flag(key);
        s.flag(key, value);
        markDirty(player);
        plugin.applyChange(player, key);
        plugin.sounds().play(player, value ? "on" : "off");
        LanguageManager lang = plugin.lang();
        lang.sendToggle(player, lang.rawOr(s.language(), "label." + key, key), value, key.equals("messages"));
    }

    public void toggleField(Player player, String field) {
        PlayerSettings s = get(player);
        boolean visible = s.toggleField(field);
        markDirty(player);
        plugin.applyChange(player, "scoreboard-field");
        plugin.sounds().play(player, visible ? "on" : "off");
        LanguageManager lang = plugin.lang();
        lang.sendToggle(player, lang.rawOr(s.language(), "field." + field, field.toUpperCase()), visible, false);
    }

    /** Shows every scoreboard field if any is hidden, otherwise hides them all. */
    public void toggleAllFields(Player player) {
        PlayerSettings s = get(player);
        List<String> ids = new ArrayList<>(plugin.scoreboard().fieldIds());
        boolean allVisible = ids.stream().noneMatch(s::hidden);
        s.setHidden(ids, allVisible);
        markDirty(player);
        plugin.applyChange(player, "scoreboard-field");
        plugin.sounds().play(player, allVisible ? "off" : "on");
        LanguageManager lang = plugin.lang();
        lang.sendToggle(player, lang.raw(s.language(), "label.fields-all"), !allVisible, false);
    }

    public void cycleCompact(Player player) {
        PlayerSettings s = get(player);
        int level = s.cycleCompact();
        markDirty(player);
        plugin.applyChange(player, "compact");
        plugin.sounds().play(player, "setting");
        LanguageManager lang = plugin.lang();
        lang.send(player, "compact.set", "mode", lang.raw(s.language(), "compact.mode." + level));
    }

    public void setLanguage(Player player, PlayerLanguage language) {
        PlayerSettings s = get(player);
        s.language(language);
        markDirty(player);
        plugin.applyChange(player, "language");
        plugin.sounds().play(player, "language");
        LanguageManager lang = plugin.lang();
        lang.sendAlways(player, "language.changed", "language", lang.raw(language, "language.name." + language.code()));
    }
}
