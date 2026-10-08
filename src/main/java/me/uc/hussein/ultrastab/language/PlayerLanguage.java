package me.uc.hussein.ultrastab.language;

import java.util.Locale;

public enum PlayerLanguage {
    EN("en"),
    AR("ar");

    private final String code;

    PlayerLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public PlayerLanguage other() {
        return this == EN ? AR : EN;
    }

    public static PlayerLanguage from(String raw, PlayerLanguage fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        for (PlayerLanguage lang : values()) {
            if (lang.code.equals(s)) {
                return lang;
            }
        }
        return fallback;
    }
}
