package me.uc.hussein.ultrastab.integration;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.tab.Rank;
import me.uc.hussein.ultrastab.util.Template;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves placeholders in this order: animations, built-in placeholders (work without any other
 * plugin), PlaceholderAPI (if installed), then replaces anything still unresolved with a fallback.
 */
public final class PlaceholderManager {

    private static final Pattern TOKEN = Pattern.compile("%([A-Za-z0-9_:.]+)%");

    private final UltrasTabPlugin plugin;
    private final PlaceholderApiIntegration papi;
    private final VaultIntegration vault;
    private final Statistic playTime = findPlayTime();
    private String unresolved = "-";

    public PlaceholderManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
        this.papi = new PlaceholderApiIntegration(plugin);
        this.vault = new VaultIntegration(plugin);
    }

    public void load(FileConfiguration config) {
        unresolved = config.getString("placeholders.unresolved", "-");
        papi.load(config.getBoolean("integrations.placeholderapi", true));
        vault.load(config.getBoolean("integrations.vault", true));
    }

    /** Full pipeline for one configured line. */
    public String render(Template template, Player player, PlayerLanguage lang, boolean animated, long now) {
        String text = template.raw();
        if (!template.animations().isEmpty()) {
            text = plugin.animations().replace(text, lang, animated, now);
        }
        return resolve(player, text, lang);
    }

    public String resolve(Player player, String text, PlayerLanguage lang) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher m = TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 16);
        while (m.find()) {
            String value = builtin(player, m.group(1), lang);
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(sb);
        String result = sb.toString();
        if (result.indexOf('%') >= 0 && papi.available()) {
            result = papi.apply(player, result);
        }
        if (result.indexOf('%') >= 0) {
            result = TOKEN.matcher(result).replaceAll(Matcher.quoteReplacement(unresolved));
        }
        return result;
    }

    private String builtin(Player p, String key, PlayerLanguage lang) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "player":
                return p.getName();
            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max":
                return String.valueOf(Bukkit.getMaxPlayers());
            case "ping":
                return String.valueOf(p.getPing());
            case "world":
                return p.getWorld().getName();
            case "rank": {
                Rank rank = plugin.tab().rankOf(p);
                String label = rank.label(lang);
                return label.isEmpty() ? rank.displayKey() : label;
            }
            case "group":
                return plugin.tab().rankOf(p).key();
            case "money": {
                String balance = vault.balance(p);
                return balance != null ? balance : unresolved;
            }
            case "uc": {
                if (!papi.available()) {
                    return unresolved;
                }
                String value = papi.apply(p, "%ultrasuc_balance%");
                return value.contains("%") ? unresolved : value;
            }
            case "kills":
                return String.valueOf(p.getStatistic(Statistic.PLAYER_KILLS));
            case "deaths":
                return String.valueOf(p.getStatistic(Statistic.DEATHS));
            case "playtime":
                return playtime(p);
            default:
                return null;
        }
    }

    private String playtime(Player p) {
        if (playTime == null) {
            return unresolved;
        }
        long minutes = p.getStatistic(playTime) / 1200L;
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    /** The play-time statistic was renamed between Minecraft versions, so look it up by name. */
    private static Statistic findPlayTime() {
        for (String name : new String[]{"PLAY_TIME", "PLAY_ONE_MINUTE"}) {
            try {
                return Statistic.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // try the next name
            }
        }
        return null;
    }
}
