package me.uc.hussein.ultrastab.tab;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.util.Cfg;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ordered groups from tab.yml. A player's group is the first one (top to bottom) for which they hold
 * "&lt;group-prefix&gt;&lt;key&gt;"; "default" is the fallback. The list order is the TAB sort order.
 */
public final class Ranks {

    private final List<Rank> ranks;
    private final Rank fallback;
    private final String permissionPrefix;

    private Ranks(List<Rank> ranks, Rank fallback, String permissionPrefix) {
        this.ranks = ranks;
        this.fallback = fallback;
        this.permissionPrefix = permissionPrefix;
    }

    public static Ranks load(ConfigurationSection groups, String permissionPrefix) {
        List<Rank> list = new ArrayList<>();
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                ConfigurationSection g = groups.getConfigurationSection(key);
                Map<PlayerLanguage, String> display = new EnumMap<>(PlayerLanguage.class);
                String color = "#FFFFFF";
                if (g != null) {
                    display.putAll(Cfg.text(g, "display"));
                    color = g.getString("color", color);
                }
                list.add(new Rank(key.toLowerCase(Locale.ROOT), list.size(), display, parseColor(color)));
            }
        }
        Rank fallback = list.stream().filter(r -> r.key().equals("default")).findFirst().orElse(null);
        if (fallback == null) {
            Map<PlayerLanguage, String> empty = new EnumMap<>(PlayerLanguage.class);
            for (PlayerLanguage l : PlayerLanguage.values()) {
                empty.put(l, "");
            }
            fallback = new Rank("default", list.size(), empty, NamedTextColor.WHITE);
            list.add(fallback);
        }
        return new Ranks(List.copyOf(list), fallback, permissionPrefix);
    }

    public Rank resolve(Player player) {
        for (Rank rank : ranks) {
            if (rank != fallback && player.hasPermission(permissionPrefix + rank.key())) {
                return rank;
            }
        }
        return fallback;
    }

    public Rank fallback() {
        return fallback;
    }

    public List<Rank> all() {
        return ranks;
    }

    private static TextColor parseColor(String raw) {
        if (raw != null) {
            String s = raw.trim();
            TextColor color = s.startsWith("#") ? TextColor.fromHexString(s) : NamedTextColor.NAMES.value(s.toLowerCase(Locale.ROOT));
            if (color != null) {
                return color;
            }
        }
        return NamedTextColor.WHITE;
    }
}
