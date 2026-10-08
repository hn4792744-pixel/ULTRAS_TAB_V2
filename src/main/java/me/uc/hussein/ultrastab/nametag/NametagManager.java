package me.uc.hussein.ultrastab.nametag;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.PlayerSettings;
import me.uc.hussein.ultrastab.tab.Rank;
import me.uc.hussein.ultrastab.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Locale;

/**
 * Nametags (rank prefix above the head) and tab-list sorting, both done with scoreboard teams on each
 * viewer's private board, so every viewer can have their own language and privacy choices.
 * <p>
 * Teams are shared per (rank, rank-shown) pair, e.g. "u03r" / "u03n", not per player: this keeps the team
 * count tiny, and since the client sorts the tab list by team name then player name, the team name's
 * leading rank index gives sorting by group and alphabetical order inside each group.
 * Main thread only.
 */
public final class NametagManager {

    private final UltrasTabPlugin plugin;
    private NametagProfile profile = new NametagProfile(true, "", true, true);

    public NametagManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(YamlConfiguration yaml) {
        profile = NametagProfile.load(yaml);
        refreshAll();
    }

    // ------------------------------------------------------------ lifecycle

    /** A player joined: give their board everyone, and add them to everyone's board. */
    public void join(Player player) {
        refreshViewer(player);
        refreshTarget(player);
    }

    /** A player is leaving: take them off every other viewer's board. */
    public void remove(Player target) {
        String entry = target.getName();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            Scoreboard board = plugin.scoreboard().boardOf(viewer);
            Team team = board == null ? null : board.getEntryTeam(entry);
            if (team != null) {
                team.removeEntry(entry);
            }
        }
    }

    public void refreshAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewer(viewer);
        }
    }

    /** The viewer changed language/privacy: restyle their teams and re-place every target. */
    public void refreshViewer(Player viewer) {
        Scoreboard board = plugin.scoreboard().boardOf(viewer);
        if (board == null) {
            return;
        }
        PlayerSettings vs = plugin.settings().get(viewer);
        for (Rank rank : plugin.tab().ranks().all()) {
            for (boolean shown : new boolean[]{true, false}) {
                Team team = board.getTeam(teamName(rank, shown));
                if (team != null) {
                    style(team, rank, shown, vs);
                }
            }
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            assign(viewer, board, target);
        }
    }

    /** The target's rank/privacy changed: re-place them on every viewer's board. */
    public void refreshTarget(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = plugin.scoreboard().boardOf(viewer);
            if (board != null) {
                assign(viewer, board, target);
            }
        }
    }

    // ------------------------------------------------------------ internals

    private void assign(Player viewer, Scoreboard board, Player target) {
        Rank rank = plugin.tab().rankOf(target);
        PlayerSettings vs = plugin.settings().get(viewer);
        PlayerSettings ts = plugin.settings().get(target);
        PlayerLanguage lang = vs.language();

        boolean shown = profile.enabled() && profile.showRank() && !rank.label(lang).isEmpty()
                && ts.flag("nametag-rank")
                && (viewer.equals(target) || vs.flag("nametag-ranks"));

        String name = teamName(rank, shown);
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
            style(team, rank, shown, vs);
        }
        String entry = target.getName();
        Team current = board.getEntryTeam(entry);
        if (current != team) {
            if (current != null) {
                current.removeEntry(entry);
            }
            team.addEntry(entry);
        }
    }

    private void style(Team team, Rank rank, boolean shown, PlayerSettings viewer) {
        PlayerLanguage lang = viewer.language();
        if (shown) {
            String hex = rank.color().asHexString();
            String label = "<" + hex + ">" + rank.label(lang) + "</" + hex + ">";
            team.prefix(Text.parse(profile.rankFormat().replace("{rank}", label)));
        } else {
            team.prefix(Component.empty());
        }
        if (profile.enabled()) {
            team.color(NamedTextColor.nearestTo(rank.color()));
        }
        boolean visible = profile.enabled() && profile.showName() && viewer.flag("nametag-name");
        team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                visible ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
    }

    private static String teamName(Rank rank, boolean shown) {
        return String.format(Locale.ROOT, "u%02d%s", rank.order(), shown ? "r" : "n");
    }
}
