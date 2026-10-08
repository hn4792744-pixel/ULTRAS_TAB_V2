package me.uc.hussein.ultrastab.tab;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.PlayerSettings;
import me.uc.hussein.ultrastab.util.TemplateSet;
import me.uc.hussein.ultrastab.util.Text;
import me.uc.hussein.ultrastab.util.WorldResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Header/footer and tab-list names. Driven by the single shared scheduler: a player is only re-rendered
 * when an animation they use advanced, their dynamic placeholders are due, or something about them changed,
 * and packets are only sent when the rendered text actually differs from what they already have.
 * Main thread only.
 */
public final class TabManager {

    private static final class State {
        TabProfile profile;
        Rank rank;
        PlayerLanguage lang;
        boolean animated;
        String header;
        String footer;
        String nameKey;
        long nextDynamic;
        boolean force = true;
        boolean cleared;
    }

    private final UltrasTabPlugin plugin;
    private final TabRenderer renderer;
    private final Map<UUID, State> states = new HashMap<>();
    private boolean enabled = true;
    private Ranks ranks = Ranks.load(null, "ultrastab.group.");
    private WorldResolver<TabProfile> resolver = new WorldResolver<>(List.of(), TabProfile::worlds, null);

    public TabManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new TabRenderer(plugin);
    }

    public void load(YamlConfiguration yaml) {
        enabled = yaml.getBoolean("enabled", true);
        renderer.formats(
                yaml.getString("name-format.with-rank", "<dark_gray>[</dark_gray>{rank}<dark_gray>]</dark_gray> {color}{name}"),
                yaml.getString("name-format.no-rank", "{color}{name}"));
        ranks = Ranks.load(yaml.getConfigurationSection("groups"), plugin.getConfig().getString("permissions.group-prefix", "ultrastab.group."));

        List<TabProfile> profiles = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("profiles");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(name);
                if (s != null) {
                    profiles.add(new TabProfile(name, s));
                }
            }
        }
        String fallbackName = plugin.getConfig().getString("unlisted-world-profile", "default");
        TabProfile fallback = profiles.stream().filter(p -> p.name().equals(fallbackName)).findFirst().orElse(null);
        resolver = new WorldResolver<>(profiles, TabProfile::worlds, fallback);
        forceAll();
    }

    public Ranks ranks() {
        return ranks;
    }

    public Rank rankOf(Player player) {
        State st = states.get(player.getUniqueId());
        return st != null && st.rank != null ? st.rank : ranks.resolve(player);
    }

    public void join(Player player) {
        State st = new State();
        st.rank = ranks.resolve(player);
        states.put(player.getUniqueId(), st);
    }

    public void quit(Player player) {
        states.remove(player.getUniqueId());
    }

    public void force(Player player) {
        State st = states.get(player.getUniqueId());
        if (st != null) {
            st.force = true;
            st.nameKey = null;
        }
    }

    /** Marks everyone for a full refresh; ranks are re-resolved on the very next tick (config may have changed). */
    public void forceAll() {
        for (State st : states.values()) {
            st.force = true;
            st.nameKey = null;
            st.rank = null;
            st.nextDynamic = 0;
        }
    }

    /** Removes everything this plugin applied (used on disable). */
    public void clear(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        player.playerListName(null);
    }

    public void tick(long now, Set<String> changedAnimations) {
        if (!enabled) {
            return;
        }
        long interval = plugin.updateInterval();
        for (Player player : Bukkit.getOnlinePlayers()) {
            State st = states.get(player.getUniqueId());
            if (st == null) {
                continue;
            }
            PlayerSettings ps = plugin.settings().get(player);
            boolean due = now >= st.nextDynamic || st.rank == null;
            if (due) {
                st.nextDynamic = now + interval;
                Rank rank = ranks.resolve(player);
                if (st.rank == null || !rank.key().equals(st.rank.key()) || rank.order() != st.rank.order()) {
                    st.rank = rank;
                    st.nameKey = null;
                    plugin.nametag().refreshTarget(player);
                }
            }
            TabProfile profile = resolver.resolve(player.getWorld().getName());
            updateName(player, st, ps, profile);
            updateHeaderFooter(player, st, ps, profile, due, now, changedAnimations);
        }
    }

    private void updateName(Player player, State st, PlayerSettings ps, TabProfile profile) {
        boolean styled = profile != null && profile.enabled() && profile.listEnabled();
        String key = styled ? st.rank.key() + '|' + ps.flag("tab-rank") : "off";
        if (key.equals(st.nameKey)) {
            return;
        }
        st.nameKey = key;
        // The player-list name is one value shared by all viewers, so it uses the server default language.
        player.playerListName(styled ? renderer.listName(player, st.rank, ps.flag("tab-rank"), plugin.defaultLanguage()) : null);
    }

    private void updateHeaderFooter(Player player, State st, PlayerSettings ps, TabProfile profile,
                                    boolean due, long now, Set<String> changed) {
        if (!ps.flag("tab") || profile == null || !profile.enabled()) {
            if (!st.cleared) {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
                st.cleared = true;
                st.header = null;
                st.footer = null;
                st.profile = null;
            }
            return;
        }
        PlayerLanguage lang = ps.language();
        boolean animated = ps.flag("animations");
        TemplateSet header = profile.headerEnabled() ? profile.header(lang) : TemplateSet.EMPTY;
        TemplateSet footer = profile.footerEnabled() ? profile.footer(lang) : TemplateSet.EMPTY;

        boolean animationMoved = animated && (intersects(changed, header.animations()) || intersects(changed, footer.animations()));
        boolean needs = st.force || st.cleared || profile != st.profile || lang != st.lang || animated != st.animated
                || animationMoved || (due && (header.dynamic() || footer.dynamic()));
        if (!needs) {
            return;
        }
        st.force = false;
        st.cleared = false;
        st.profile = profile;
        st.lang = lang;
        st.animated = animated;

        String h = renderer.block(header, player, lang, animated, now);
        String f = renderer.block(footer, player, lang, animated, now);
        if (!h.equals(st.header) || !f.equals(st.footer)) {
            st.header = h;
            st.footer = f;
            player.sendPlayerListHeaderAndFooter(Text.parse(h), Text.parse(f));
        }
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        return !a.isEmpty() && !b.isEmpty() && !Collections.disjoint(a, b);
    }
}
