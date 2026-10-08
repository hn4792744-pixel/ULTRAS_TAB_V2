package me.uc.hussein.ultrastab.scoreboard;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.PlayerSettings;
import me.uc.hussein.ultrastab.util.Template;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the visible lines for one player: applies hidden fields and the compact level, removes
 * redundant blank lines and renders placeholders. Compact levels keep lines whose priority is
 * at most 4 / 3 / 2 / 1 (Full / I / II / III) and drop spacer lines in the compact modes.
 */
final class ScoreboardRenderer {

    static final int MAX_LINES = 15;
    private static final int[] MAX_PRIORITY = {4, 3, 2, 1};

    private final UltrasTabPlugin plugin;

    ScoreboardRenderer(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    String title(ScoreboardProfile profile, Player player, PlayerLanguage lang, boolean animated, long now) {
        return plugin.placeholders().render(profile.title(lang), player, lang, animated, now);
    }

    List<String> lines(ScoreboardProfile profile, Player player, PlayerSettings settings,
                       PlayerLanguage lang, boolean animated, long now) {
        int level = Math.max(0, Math.min(MAX_PRIORITY.length - 1, settings.compact()));
        int maxPriority = MAX_PRIORITY[level];
        boolean compact = level > 0;
        List<String> out = new ArrayList<>();
        for (ScoreboardProfile.Line line : profile.lines()) {
            Template t = line.text(lang);
            if (t.isBlank()) {
                if (!compact && !out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
                    out.add("");
                }
                continue;
            }
            if ((line.id() != null && settings.hidden(line.id())) || line.priority() > maxPriority) {
                continue;
            }
            out.add(plugin.placeholders().render(t, player, lang, animated, now));
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out.size() > MAX_LINES ? new ArrayList<>(out.subList(0, MAX_LINES)) : out;
    }
}
