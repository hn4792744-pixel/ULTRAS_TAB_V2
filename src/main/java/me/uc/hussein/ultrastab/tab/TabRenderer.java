package me.uc.hussein.ultrastab.tab;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.util.Template;
import me.uc.hussein.ultrastab.util.TemplateSet;
import me.uc.hussein.ultrastab.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Turns TAB templates into final strings/components. Pure rendering - no state, no sending. */
final class TabRenderer {

    private final UltrasTabPlugin plugin;
    private String withRank = "{color}{name}";
    private String noRank = "{color}{name}";

    TabRenderer(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    void formats(String withRank, String noRank) {
        this.withRank = withRank;
        this.noRank = noRank;
    }

    /** Renders a header/footer block to one string (lines joined by newlines). */
    String block(TemplateSet set, Player player, PlayerLanguage lang, boolean animated, long now) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Template t : set.lines()) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            sb.append(plugin.placeholders().render(t, player, lang, animated, now));
        }
        return sb.toString();
    }

    Component listName(Player player, Rank rank, boolean showRank, PlayerLanguage lang) {
        String label = rank.label(lang);
        boolean withLabel = showRank && !label.isEmpty();
        String color = "<" + rank.color().asHexString() + ">";
        String closing = "</" + rank.color().asHexString() + ">";
        String text = (withLabel ? withRank : noRank)
                .replace("{rank}", color + label + closing)
                .replace("{color}", color)
                .replace("{name}", player.getName());
        return Text.parse(text);
    }
}
