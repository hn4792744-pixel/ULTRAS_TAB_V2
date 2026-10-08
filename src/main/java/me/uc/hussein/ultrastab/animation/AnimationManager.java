package me.uc.hussein.ultrastab.animation;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.util.Cfg;
import me.uc.hussein.ultrastab.util.Template;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Holds all animations. {@link #advance(long)} is called once per tick of the single shared scheduler
 * and returns which animations moved to a new frame, so only text that uses them is re-rendered.
 */
public final class AnimationManager {

    private Map<String, Animation> animations = Map.of();
    private final Map<String, Integer> lastFrame = new HashMap<>();
    private final Set<String> changed = new HashSet<>();

    public void load(YamlConfiguration yaml) {
        Map<String, Animation> loaded = new HashMap<>();
        for (String name : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            Map<PlayerLanguage, List<String>> frames = Cfg.lines(section, "frames");
            if (frames.get(PlayerLanguage.EN).isEmpty()) {
                continue;
            }
            long interval = Math.max(50L, section.getLong("interval", 1000L));
            loaded.put(name, new Animation(name, interval, frames));
        }
        animations = loaded;
        lastFrame.clear();
    }

    /** Names of animations whose current frame differs from the previous call. The set is reused. */
    public Set<String> advance(long now) {
        changed.clear();
        for (Animation a : animations.values()) {
            if (!a.animated()) {
                continue;
            }
            int key = a.index(PlayerLanguage.EN, now) * 100003 + a.index(PlayerLanguage.AR, now);
            Integer previous = lastFrame.put(a.name(), key);
            if (previous == null || previous != key) {
                changed.add(a.name());
            }
        }
        return changed;
    }

    public String replace(String text, PlayerLanguage lang, boolean animated, long now) {
        if (!text.contains("%animation:")) {
            return text;
        }
        Matcher m = Template.ANIMATION.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 32);
        while (m.find()) {
            Animation a = animations.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(a == null ? "" : a.frame(lang, now, animated)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
