package me.uc.hussein.ultrastab.nametag;

import org.bukkit.configuration.file.YamlConfiguration;

/** Server-wide nametag design from nametag.yml. */
public record NametagProfile(boolean enabled, String rankFormat, boolean showRank, boolean showName) {

    public static NametagProfile load(YamlConfiguration yaml) {
        return new NametagProfile(
                yaml.getBoolean("enabled", true),
                yaml.getString("format.rank", "<dark_gray>[</dark_gray>{rank}<dark_gray>]</dark_gray> "),
                yaml.getBoolean("visibility.show-rank", true),
                yaml.getBoolean("visibility.show-name", true));
    }
}
