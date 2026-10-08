package me.uc.hussein.ultrastab.util;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for config values that are either shared (a plain string/list) or per-language
 * (a section with "en" and "ar" keys). A missing language falls back to "en".
 */
public final class Cfg {

    private Cfg() {
    }

    public static Map<PlayerLanguage, String> text(ConfigurationSection s, String key) {
        Map<PlayerLanguage, String> out = new EnumMap<>(PlayerLanguage.class);
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            out.put(lang, text(s, key, lang));
        }
        return out;
    }

    public static String text(ConfigurationSection s, String key, PlayerLanguage lang) {
        ConfigurationSection sub = s.getConfigurationSection(key);
        if (sub != null) {
            return sub.getString(lang.code(), sub.getString("en", ""));
        }
        return s.getString(key, "");
    }

    public static Map<PlayerLanguage, List<String>> lines(ConfigurationSection s, String key) {
        Map<PlayerLanguage, List<String>> out = new EnumMap<>(PlayerLanguage.class);
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            out.put(lang, lines(s, key, lang));
        }
        return out;
    }

    public static List<String> lines(ConfigurationSection s, String key, PlayerLanguage lang) {
        if (s.isList(key)) {
            return s.getStringList(key);
        }
        ConfigurationSection sub = s.getConfigurationSection(key);
        if (sub == null) {
            return List.of();
        }
        return sub.isList(lang.code()) ? sub.getStringList(lang.code()) : sub.getStringList("en");
    }
}
