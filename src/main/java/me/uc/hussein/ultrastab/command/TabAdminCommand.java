package me.uc.hussein.ultrastab.command;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Admin sub-commands of /tab: reload and reset. */
public final class TabAdminCommand {

    static final List<String> SUBS = List.of("reload", "reset");

    private final UltrasTabPlugin plugin;

    public TabAdminCommand(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    boolean handles(String sub) {
        return SUBS.contains(sub);
    }

    boolean allowed(CommandSender sender, String sub) {
        return sender.hasPermission("ultrastab.admin")
                || (sub.equals("reload") && sender.hasPermission("ultrastab.admin.reload"));
    }

    void handle(CommandSender sender, String sub, String[] args) {
        if (!allowed(sender, sub)) {
            plugin.lang().sendAlways(sender, "error.no-permission");
            return;
        }
        if (sub.equals("reload")) {
            try {
                plugin.reloadAll();
                plugin.lang().sendAlways(sender, "reload.success");
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Reload failed: " + ex);
                plugin.lang().sendAlways(sender, "reload.failure", "error", String.valueOf(ex.getMessage()));
            }
            return;
        }
        // reset <player>
        if (args.length < 2) {
            plugin.lang().sendAlways(sender, "error.usage", "usage", "/tab reset <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.lang().sendAlways(sender, "error.player-not-found", "input", args[1]);
            return;
        }
        plugin.settings().reset(target);
        plugin.lang().sendAlways(sender, "admin.reset", "player", target.getName());
    }

    List<String> complete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && allowed(sender, "reset")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
