package me.uc.hussein.ultrastab.tab;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;
import java.util.Map;

/** A permission group used for TAB sorting, the rank label and the name color. */
public record Rank(String key, int order, Map<PlayerLanguage, String> display, TextColor color) {

    public String label(PlayerLanguage lang) {
        return display.getOrDefault(lang, "");
    }

    /** Human-readable fallback used by %rank% when the group has no label (e.g. "Default"). */
    public String displayKey() {
        return key.isEmpty() ? key : key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
    }
}
