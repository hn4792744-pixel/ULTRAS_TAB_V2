package me.uc.hussein.ultrastab.animation;

import me.uc.hussein.ultrastab.language.PlayerLanguage;

import java.util.List;
import java.util.Map;

/**
 * A named frame sequence. The frame is derived from the clock (no per-animation task, no per-player
 * state), so every player sees the same frame at the same time at zero cost.
 */
public record Animation(String name, long interval, Map<PlayerLanguage, List<String>> frames) {

    private List<String> list(PlayerLanguage lang) {
        List<String> list = frames.get(lang);
        return list == null || list.isEmpty() ? frames.get(PlayerLanguage.EN) : list;
    }

    public int index(PlayerLanguage lang, long now) {
        List<String> list = list(lang);
        return list.size() <= 1 ? 0 : (int) ((now / interval) % list.size());
    }

    /** Current frame, or the first (base) frame when the player disabled animations. */
    public String frame(PlayerLanguage lang, long now, boolean animated) {
        List<String> list = list(lang);
        return list.get(animated ? index(lang, now) : 0);
    }

    public boolean animated() {
        return frames.values().stream().anyMatch(list -> list.size() > 1);
    }
}
