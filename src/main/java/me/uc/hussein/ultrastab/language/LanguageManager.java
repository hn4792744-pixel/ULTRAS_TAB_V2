package me.uc.hussein.ultrastab.language;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads messages/en.yml and messages/ar.yml. Keys missing from a user-edited file fall back to the
 * bundled copy, then to English, so an outdated file never produces blank messages.
 */
public final class LanguageManager {

    private final UltrasTabPlugin plugin;
    private final Map<PlayerLanguage, YamlConfiguration> files = new EnumMap<>(PlayerLanguage.class);

    public LanguageManager(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        files.clear();
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            String path = "messages/" + lang.code() + ".yml";
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), path));
            try (InputStream in = plugin.getResource(path)) {
                if (in != null) {
                    yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not read bundled " + path + ": " + ex.getMessage());
            }
            files.put(lang, yaml);
        }
    }

    /** Raw (unparsed) message text, or the key itself when missing everywhere. */
    public String raw(PlayerLanguage lang, String key) {
        YamlConfiguration yaml = files.get(lang);
        String value = yaml == null ? null : yaml.getString(key);
        if (value == null && lang != PlayerLanguage.EN) {
            return raw(PlayerLanguage.EN, key);
        }
        return value == null ? key : value;
    }

    public List<String> rawList(PlayerLanguage lang, String key) {
        YamlConfiguration yaml = files.get(lang);
        List<String> value = yaml == null ? List.of() : yaml.getStringList(key);
        if (value.isEmpty() && lang != PlayerLanguage.EN) {
            return rawList(PlayerLanguage.EN, key);
        }
        return value;
    }

    /** Raw text or a fallback when the key does not exist. */
    public String rawOr(PlayerLanguage lang, String key, String fallback) {
        String value = raw(lang, key);
        return value.equals(key) ? fallback : value;
    }

    public String fill(PlayerLanguage lang, String template, String... kv) {
        String text = template.replace("{prefix}", raw(lang, "prefix"));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            text = text.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return text;
    }

    public Component component(PlayerLanguage lang, String key, String... kv) {
        return Text.parse(fill(lang, raw(lang, key), kv));
    }

    public PlayerLanguage langOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return plugin.settings().get(player).language();
        }
        return plugin.defaultLanguage();
    }

    /** Normal feedback: suppressed when the player turned messages off. */
    public void send(Player player, String key, String... kv) {
        if (plugin.settings().get(player).flag("messages")) {
            sendAlways(player, key, kv);
        }
    }

    /** Errors and admin output: always delivered. */
    public void sendAlways(CommandSender sender, String key, String... kv) {
        sender.sendMessage(component(langOf(sender), key, kv));
    }

    public void sendToggle(Player player, String label, boolean on, boolean always) {
        PlayerLanguage lang = langOf(player);
        String state = raw(lang, on ? "state.on" : "state.off");
        String[] kv = {"label", label, "state", state, "c", on ? "#7fc29b" : "#d96c6c", "sym", on ? "✓" : "×"};
        if (always) {
            sendAlways(player, "toggle", kv);
        } else {
            send(player, "toggle", kv);
        }
    }
}
