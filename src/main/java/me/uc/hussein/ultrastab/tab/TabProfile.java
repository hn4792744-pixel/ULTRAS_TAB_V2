package me.uc.hussein.ultrastab.tab;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.util.Cfg;
import me.uc.hussein.ultrastab.util.TemplateSet;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One TAB design from tab.yml: which worlds use it, whether the list names are styled, and header/footer per language. */
public final class TabProfile {

    private final String name;
    private final Set<String> worlds;
    private final boolean enabled;
    private final boolean listEnabled;
    private final boolean headerEnabled;
    private final boolean footerEnabled;
    private final Map<PlayerLanguage, TemplateSet> header = new EnumMap<>(PlayerLanguage.class);
    private final Map<PlayerLanguage, TemplateSet> footer = new EnumMap<>(PlayerLanguage.class);

    public TabProfile(String name, ConfigurationSection s) {
        this.name = name;
        this.worlds = Set.copyOf(s.getStringList("worlds"));
        this.enabled = s.getBoolean("enabled", true);
        this.listEnabled = s.getBoolean("player-list.enabled", true);
        this.headerEnabled = s.getBoolean("header.enabled", true);
        this.footerEnabled = s.getBoolean("footer.enabled", true);
        parse(s, "header.lines", header);
        parse(s, "footer.lines", footer);
    }

    private static void parse(ConfigurationSection s, String path, Map<PlayerLanguage, TemplateSet> out) {
        int dot = path.indexOf('.');
        ConfigurationSection parent = s.getConfigurationSection(path.substring(0, dot));
        Map<PlayerLanguage, List<String>> lines = parent == null ? Map.of() : Cfg.lines(parent, path.substring(dot + 1));
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            out.put(lang, TemplateSet.of(lines.getOrDefault(lang, List.of())));
        }
    }

    public String name() {
        return name;
    }

    public Set<String> worlds() {
        return worlds;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean listEnabled() {
        return listEnabled;
    }

    public boolean headerEnabled() {
        return headerEnabled;
    }

    public boolean footerEnabled() {
        return footerEnabled;
    }

    public TemplateSet header(PlayerLanguage lang) {
        return header.get(lang);
    }

    public TemplateSet footer(PlayerLanguage lang) {
        return footer.get(lang);
    }
}
