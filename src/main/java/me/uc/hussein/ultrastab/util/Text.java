package me.uc.hussein.ultrastab.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts config strings to Adventure components. Accepts MiniMessage, legacy "&" / "§" codes,
 * "&#RRGGBB" and bare "#RRGGBB" colors in any mix. Results are cached by input string.
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern BARE_HEX = Pattern.compile("(?<![<:/&\\w])#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final int CACHE_LIMIT = 4096;
    private static final Map<String, Component> CACHE = new ConcurrentHashMap<>();

    private static final Map<Character, String> CODES = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    private Text() {
    }

    /** Converts mixed legacy/hex/MiniMessage input to pure MiniMessage. */
    public static String toMini(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String s = input.replace('\u00a7', '&');
        s = AMP_HEX.matcher(s).replaceAll("<reset><#$1>");
        s = BARE_HEX.matcher(s).replaceAll("<#$1>");
        Matcher m = LEGACY.matcher(s);
        StringBuilder sb = new StringBuilder(s.length() + 16);
        while (m.find()) {
            char c = Character.toLowerCase(m.group(1).charAt(0));
            String tag = CODES.get(c);
            boolean color = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            m.appendReplacement(sb, Matcher.quoteReplacement(color ? "<reset><" + tag + ">" : "<" + tag + ">"));
        }
        m.appendTail(sb);
        return sb.toString().replace("\n", "<newline>");
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        Component cached = CACHE.get(input);
        if (cached != null) {
            return cached;
        }
        Component parsed;
        try {
            parsed = MM.deserialize(toMini(input));
        } catch (RuntimeException ex) {
            parsed = Component.text(input);
        }
        if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(input, parsed);
        return parsed;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
