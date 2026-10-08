package me.uc.hussein.ultrastab.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.PlayerSettings;
import me.uc.hussein.ultrastab.util.Text;
import me.uc.hussein.ultrastab.util.WorldResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns every player's private {@link Scoreboard} (also used by the nametag teams) and the sidebar on it.
 * Lines are score entries with a custom display name, so there are no 16-character limits, no number column,
 * and a changed line is updated in place without recreating the board. Main thread only.
 */
public final class ScoreboardManager {

    private static final class Board {
        final Scoreboard scoreboard;
        final Objective objective;
        final String[] last = new String[ScoreboardRenderer.MAX_LINES];
        int count;
        String title;
        boolean shown;
        ScoreboardProfile profile;
        PlayerLanguage lang;
        boolean animated;
        long nextDynamic;
        boolean force = true;

        Board(Scoreboard scoreboard, Objective objective) {
            this.scoreboard = scoreboard;
            this.objective = objective;
        }
    }

    private static final String[] ENTRIES = new String[ScoreboardRenderer.MAX_LINES];

    static {
        for (int i = 0; i < ENTRIES.length; i++) {
            ENTRIES[i] = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
        }
    }

    private final UltrasTabPlugin plugin;
    private final ScoreboardRenderer renderer;
    private final Map<UUID, Board> boards = new HashMap<>();
    private boolean enabled = true;
    private WorldResolver<ScoreboardProfile> resolver = new WorldResolver<>(List.of(), ScoreboardProfile::worlds, null);
    private Set<String> fieldIds = Set.of();

    public ScoreboardManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new ScoreboardRenderer(plugin);
    }

    public void load(YamlConfiguration yaml) {
        enabled = yaml.getBoolean("enabled", true);
        List<ScoreboardProfile> profiles = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("scoreboards");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(name);
                if (s != null) {
                    profiles.add(new ScoreboardProfile(name, s));
                }
            }
        }
        String fallbackName = plugin.getConfig().getString("unlisted-world-profile", "default");
        ScoreboardProfile fallback = profiles.stream().filter(p -> p.name().equals(fallbackName)).findFirst().orElse(null);
        resolver = new WorldResolver<>(profiles, ScoreboardProfile::worlds, fallback);

        Set<String> ids = new LinkedHashSet<>();
        for (ScoreboardProfile profile : profiles) {
            for (ScoreboardProfile.Line line : profile.lines()) {
                if (line.id() != null) {
                    ids.add(line.id());
                }
            }
        }
        fieldIds = Collections.unmodifiableSet(ids);
        forceAll();
    }

    /** Ids of all hideable lines across every scoreboard profile (used by commands and the GUI). */
    public Set<String> fieldIds() {
        return fieldIds;
    }

    // ------------------------------------------------------------ lifecycle

    public void attach(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("ultras", Criteria.DUMMY, Component.empty());
        objective.numberFormat(NumberFormat.blank());
        boards.put(player.getUniqueId(), new Board(scoreboard, objective));
        player.setScoreboard(scoreboard);
    }

    public void detach(Player player) {
        boards.remove(player.getUniqueId());
    }

    /** The viewer's private scoreboard (shared with the nametag teams), or null if not attached. */
    public Scoreboard boardOf(Player player) {
        Board b = boards.get(player.getUniqueId());
        return b == null ? null : b.scoreboard;
    }

    public void force(Player player) {
        Board b = boards.get(player.getUniqueId());
        if (b != null) {
            b.force = true;
        }
    }

    public void forceAll() {
        for (Board b : boards.values()) {
            b.force = true;
        }
    }

    /** Gives the player back the server's main scoreboard (used on disable). */
    public void release(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ------------------------------------------------------------ updating

    public void tick(long now, Set<String> changedAnimations) {
        if (!enabled) {
            return;
        }
        long interval = plugin.updateInterval();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Board b = boards.get(player.getUniqueId());
            if (b == null) {
                continue;
            }
            PlayerSettings ps = plugin.settings().get(player);
            ScoreboardProfile profile = resolver.resolve(player.getWorld().getName());
            if (!ps.flag("scoreboard") || profile == null || !profile.enabled()) {
                hide(b);
                continue;
            }
            boolean due = now >= b.nextDynamic;
            if (due) {
                b.nextDynamic = now + interval;
            }
            PlayerLanguage lang = ps.language();
            boolean animated = ps.flag("animations");
            boolean animationMoved = animated && !changedAnimations.isEmpty()
                    && !Collections.disjoint(changedAnimations, profile.animations(lang));
            boolean needs = b.force || !b.shown || profile != b.profile || lang != b.lang || animated != b.animated
                    || animationMoved || (due && profile.dynamic(lang));
            if (!needs) {
                continue;
            }
            b.force = false;
            b.profile = profile;
            b.lang = lang;
            b.animated = animated;
            apply(b, player, profile, ps, lang, animated, now);
        }
    }

    private void hide(Board b) {
        if (b.shown) {
            b.scoreboard.clearSlot(DisplaySlot.SIDEBAR);
            b.shown = false;
        }
    }

    private void apply(Board b, Player player, ScoreboardProfile profile, PlayerSettings ps,
                       PlayerLanguage lang, boolean animated, long now) {
        List<String> lines = renderer.lines(profile, player, ps, lang, animated, now);
        String title = renderer.title(profile, player, lang, animated, now);

        if (!b.shown) {
            b.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            b.shown = true;
        }
        if (!title.equals(b.title)) {
            b.title = title;
            b.objective.displayName(Text.parse(title));
        }

        int count = lines.size();
        boolean countChanged = count != b.count;
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            if (countChanged || !line.equals(b.last[i])) {
                Score score = b.objective.getScore(ENTRIES[i]);
                score.setScore(count - i);
                score.customName(Text.parse(line));
                b.last[i] = line;
            }
        }
        for (int i = count; i < b.last.length; i++) {
            if (b.last[i] != null) {
                b.scoreboard.resetScores(ENTRIES[i]);
                b.last[i] = null;
            }
        }
        b.count = count;
    }
}
