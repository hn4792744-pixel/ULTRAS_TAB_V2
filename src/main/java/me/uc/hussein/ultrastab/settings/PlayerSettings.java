package me.uc.hussein.ultrastab.settings;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One player's personal choices. All booleans default to ON; the language defaults to the server default. */
public final class PlayerSettings {

    /** Every boolean switch a player owns. The same keys are used by commands, GUIs and the save file. */
    public static final List<String> FLAGS = List.of(
            "tab", "scoreboard", "animations", "sounds", "messages",
            "tab-rank", "nametag-rank", "nametag-ranks", "nametag-name");

    public static final int COMPACT_LEVELS = 4;

    private final Map<String, Boolean> flags = new HashMap<>();
    private final Set<String> hiddenFields = new HashSet<>();
    private PlayerLanguage language;
    private int compact;

    public PlayerSettings(PlayerLanguage language) {
        this.language = language;
    }

    public boolean flag(String key) {
        return flags.getOrDefault(key, Boolean.TRUE);
    }

    public void flag(String key, boolean value) {
        flags.put(key, value);
    }

    public PlayerLanguage language() {
        return language;
    }

    public void language(PlayerLanguage language) {
        this.language = language;
    }

    public boolean hidden(String field) {
        return hiddenFields.contains(field);
    }

    /** Toggles a scoreboard field; returns true when the field is visible afterwards. */
    public boolean toggleField(String field) {
        if (!hiddenFields.remove(field)) {
            hiddenFields.add(field);
            return false;
        }
        return true;
    }

    public void setHidden(Collection<String> fields, boolean hidden) {
        if (hidden) {
            hiddenFields.addAll(fields);
        } else {
            hiddenFields.removeAll(fields);
        }
    }

    public int compact() {
        return compact;
    }

    public int cycleCompact() {
        compact = (compact + 1) % COMPACT_LEVELS;
        return compact;
    }

    public YamlConfiguration toYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("language", language.code());
        for (String key : FLAGS) {
            yaml.set("flags." + key, flag(key));
        }
        yaml.set("hidden-fields", new ArrayList<>(hiddenFields));
        yaml.set("compact", compact);
        return yaml;
    }

    public static PlayerSettings fromYaml(YamlConfiguration yaml, PlayerLanguage defaultLanguage) {
        PlayerSettings s = new PlayerSettings(PlayerLanguage.from(yaml.getString("language"), defaultLanguage));
        for (String key : FLAGS) {
            s.flags.put(key, yaml.getBoolean("flags." + key, true));
        }
        s.hiddenFields.addAll(yaml.getStringList("hidden-fields"));
        s.compact = Math.floorMod(yaml.getInt("compact", 0), COMPACT_LEVELS);
        return s;
    }
}
