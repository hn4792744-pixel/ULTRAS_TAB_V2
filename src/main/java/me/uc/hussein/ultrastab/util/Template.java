package me.uc.hussein.ultrastab.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A configured text line analysed once at load time: which animations it references and whether it
 * contains placeholders (dynamic). Static lines are never re-rendered.
 */
public record Template(String raw, Set<String> animations, boolean dynamic) {

    public static final Pattern ANIMATION = Pattern.compile("%animation:([A-Za-z0-9_\\-]+)%");
    private static final Pattern ANY_PLACEHOLDER = Pattern.compile("%[A-Za-z0-9_:.]+%");

    public static Template of(String raw) {
        String text = raw == null ? "" : raw;
        Set<String> animations = new HashSet<>();
        Matcher m = ANIMATION.matcher(text);
        while (m.find()) {
            animations.add(m.group(1));
        }
        String rest = ANIMATION.matcher(text).replaceAll("");
        return new Template(text, Set.copyOf(animations), ANY_PLACEHOLDER.matcher(rest).find());
    }

    public boolean isBlank() {
        return raw.isBlank();
    }
}
