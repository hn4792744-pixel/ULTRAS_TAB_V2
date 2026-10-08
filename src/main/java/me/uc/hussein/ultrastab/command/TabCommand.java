package me.uc.hussein.ultrastab.command;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.SettingsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /tab - every personal command, plus admin sub-commands (delegated to {@link TabAdminCommand}).
 * <pre>
 * /tab setting | tab | scoreboard [field|all|compact] | animations | sound | messages
 * /tab language [en|ar] | tab-rank | nametag-rank | nametag-ranks | nametag-name
 * /tab reload | reset &lt;player&gt;
 * </pre>
 */
public final class TabCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBS = List.of(
            "setting", "tab", "scoreboard", "animations", "sound", "messages", "language",
            "tab-rank", "nametag-rank", "nametag-ranks", "nametag-name");

    private final UltrasTabPlugin plugin;
    private final TabAdminCommand admin;

    public TabCommand(UltrasTabPlugin plugin, TabAdminCommand admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (admin.handles(sub)) {
            admin.handle(sender, sub, args);
            return true;
        }
        if (!canUse(sender)) {
            plugin.lang().sendAlways(sender, "error.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.lang().sendAlways(sender, "error.player-only");
            return true;
        }
        SettingsManager settings = plugin.settings();
        switch (sub) {
            case "setting", "settings" -> plugin.gui().open(player, "settings");
            case "tab" -> settings.toggle(player, "tab");
            case "animations", "animation" -> settings.toggle(player, "animations");
            case "sound", "sounds" -> settings.toggle(player, "sounds");
            case "messages", "message" -> settings.toggle(player, "messages");
            case "tab-rank" -> settings.toggle(player, "tab-rank");
            case "nametag-rank" -> settings.toggle(player, "nametag-rank");
            case "nametag-ranks" -> settings.toggle(player, "nametag-ranks");
            case "nametag-name" -> settings.toggle(player, "nametag-name");
            case "language", "lang" -> language(player, args);
            case "scoreboard", "sb" -> scoreboard(player, args);
            default -> plugin.lang().sendAlways(sender, "error.unknown");
        }
        return true;
    }

    private void language(Player player, String[] args) {
        SettingsManager settings = plugin.settings();
        if (args.length < 2) {
            settings.setLanguage(player, settings.get(player).language().other());
            return;
        }
        PlayerLanguage requested = PlayerLanguage.from(args[1], null);
        if (requested == null) {
            plugin.lang().sendAlways(player, "error.language");
            return;
        }
        settings.setLanguage(player, requested);
    }

    private void scoreboard(Player player, String[] args) {
        SettingsManager settings = plugin.settings();
        if (args.length < 2) {
            settings.toggle(player, "scoreboard");
            return;
        }
        String arg = args[1].toLowerCase(Locale.ROOT);
        switch (arg) {
            case "compact" -> settings.cycleCompact(player);
            case "all" -> settings.toggleAllFields(player);
            default -> {
                if (plugin.scoreboard().fieldIds().contains(arg)) {
                    settings.toggleField(player, arg);
                } else {
                    plugin.lang().sendAlways(player, "error.unknown-field", "input", arg);
                }
            }
        }
    }

    private boolean canUse(CommandSender sender) {
        String node = plugin.getConfig().getString("permissions.use", "");
        return node == null || node.isBlank() || sender.hasPermission(node);
    }

    private void help(CommandSender sender) {
        var lang = plugin.lang();
        var language = lang.langOf(sender);
        lang.sendAlways(sender, "help.header");
        for (String line : lang.rawList(language, "help.lines")) {
            sender.sendMessage(me.uc.hussein.ultrastab.util.Text.parse(lang.fill(language, line)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (canUse(sender)) {
                out.addAll(PLAYER_SUBS);
            }
            for (String sub : TabAdminCommand.SUBS) {
                if (admin.allowed(sender, sub)) {
                    out.add(sub);
                }
            }
            return filter(out, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("scoreboard") || sub.equals("sb")) {
                out.add("all");
                out.add("compact");
                out.addAll(plugin.scoreboard().fieldIds());
                return filter(out, args[1]);
            }
            if (sub.equals("language") || sub.equals("lang")) {
                for (PlayerLanguage l : PlayerLanguage.values()) {
                    out.add(l.code());
                }
                return filter(out, args[1]);
            }
            return filter(admin.complete(sender, args), args[1]);
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
