package me.uc.hussein.ultrastab.scoreboard;

import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.util.Cfg;
import me.uc.hussein.ultrastab.util.Template;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One scoreboard design from scoreboard.yml. A line is either a plain string (shared by all languages) or a
 * section with optional "id" (hideable by players), "priority" (1 = most important, used by compact modes)
 * and "text" (shared) or "en"/"ar" texts.
 */
public final class ScoreboardProfile {

    public record Line(String id, int priority, Map<PlayerLanguage, Template> text) {
        public Template text(PlayerLanguage lang) {
            return text.get(lang);
        }
    }

    private final String name;
    private final Set<String> worlds;
    private final boolean enabled;
    private final Map<PlayerLanguage, Template> title = new EnumMap<>(PlayerLanguage.class);
    private final List<Line> lines = new ArrayList<>();
    private final Map<PlayerLanguage, Set<String>> animations = new EnumMap<>(PlayerLanguage.class);
    private final Map<PlayerLanguage, Boolean> dynamic = new EnumMap<>(PlayerLanguage.class);

    public ScoreboardProfile(String name, ConfigurationSection s) {
        this.name = name;
        this.worlds = Set.copyOf(s.getStringList("worlds"));
        this.enabled = s.getBoolean("enabled", true);
        Cfg.text(s, "title").forEach((lang, raw) -> title.put(lang, Template.of(raw)));

        for (Object entry : s.getList("lines", List.of())) {
            lines.add(parseLine(entry));
        }
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            Set<String> anims = new HashSet<>(title.get(lang).animations());
            boolean dyn = title.get(lang).dynamic();
            for (Line line : lines) {
                anims.addAll(line.text(lang).animations());
                dyn |= line.text(lang).dynamic();
            }
            animations.put(lang, Set.copyOf(anims));
            dynamic.put(lang, dyn);
        }
    }

    @SuppressWarnings("unchecked")
    private static Line parseLine(Object entry) {
        Map<PlayerLanguage, Template> text = new EnumMap<>(PlayerLanguage.class);
        if (entry instanceof Map<?, ?> map) {
            Map<String, Object> m = (Map<String, Object>) map;
            String shared = str(m.get("text"), "");
            String en = str(m.get("en"), shared);
            for (PlayerLanguage lang : PlayerLanguage.values()) {
                text.put(lang, Template.of(str(m.get(lang.code()), en)));
            }
            Object id = m.get("id");
            Object priority = m.get("priority");
            return new Line(id == null ? null : String.valueOf(id).toLowerCase(),
                    priority instanceof Number n ? Math.max(1, Math.min(4, n.intValue())) : 1, text);
        }
        Template plain = Template.of(str(entry, ""));
        for (PlayerLanguage lang : PlayerLanguage.values()) {
            text.put(lang, plain);
        }
        return new Line(null, 1, text);
    }

    private static String str(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    public String name() {
        return name;
    }

    public Set<String> worlds() {
        return worlds;
    }

    public boolean enabled() {
        return enabled;
    }

    public Template title(PlayerLanguage lang) {
        return title.get(lang);
    }

    public List<Line> lines() {
        return lines;
    }

    public Set<String> animations(PlayerLanguage lang) {
        return animations.get(lang);
    }

    public boolean dynamic(PlayerLanguage lang) {
        return dynamic.get(lang);
    }
}
