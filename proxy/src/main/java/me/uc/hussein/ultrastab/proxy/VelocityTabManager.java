package me.uc.hussein.ultrastab.proxy;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VelocityTabManager {

    private final ProxyServer proxyServer;
    private final VelocityTabConfig config;
    private final VelocityTabRanks ranks;

    public VelocityTabManager(
            ProxyServer proxyServer,
            VelocityTabConfig config,
            VelocityTabRanks ranks
    ) {
        this.proxyServer = proxyServer;
        this.config = config;
        this.ranks = ranks;
    }

    public void update(Player viewer) {
        if (!config.isEnabled()) {
            viewer.getTabList().clearHeaderAndFooter();
            return;
        }

        Map<String, Object> profiles = config.getProfiles();

        if (profiles.isEmpty()) {
            viewer.getTabList().clearHeaderAndFooter();
            return;
        }

        Object defaultProfileObject = profiles.get("default");

        if (!(defaultProfileObject instanceof Map<?, ?> profile)) {
            viewer.getTabList().clearHeaderAndFooter();
            return;
        }

        boolean profileEnabled = booleanValue(profile.get("enabled"), true);

        if (!profileEnabled) {
            viewer.getTabList().clearHeaderAndFooter();
            return;
        }

        updateHeaderFooter(viewer, profile);
        updateEntries(viewer);
    }

    private void updateHeaderFooter(Player viewer, Map<?, ?> profile) {
        Component header = Component.empty();
        Component footer = Component.empty();

        Object headerObject = profile.get("header");

        if (headerObject instanceof Map<?, ?> headerMap
                && booleanValue(headerMap.get("enabled"), true)) {

            List<String> lines = languageLines(headerMap.get("lines"), "en");
            header = linesComponent(lines, viewer);
        }

        Object footerObject = profile.get("footer");

        if (footerObject instanceof Map<?, ?> footerMap
                && booleanValue(footerMap.get("enabled"), true)) {

            List<String> lines = languageLines(footerMap.get("lines"), "en");
            footer = linesComponent(lines, viewer);
        }

        viewer.getTabList().setHeaderAndFooter(header, footer);
    }

    private void updateEntries(Player viewer) {
        var tabList = viewer.getTabList();

        tabList.clearAll();

        int order = 0;

        for (Player target : proxyServer.getAllPlayers()) {
            VelocityTabRank rank = ranks.resolve(target);

            String rankDisplay = rank.displayEn();

            String nameFormat = resolveNameFormat();

            String formattedName = nameFormat
                    .replace("{rank}", rankDisplay)
                    .replace("{color}", rank.color())
                    .replace("{name}", target.getUsername());

            Component displayName = VelocityTabText.parse(formattedName);

            var entry = tabList.buildEntry(
                    target.getGameProfile(),
                    displayName,
                    (int) Math.max(
                            0,
                            Math.min(Integer.MAX_VALUE, target.getPing())
                    ),
                    0
            );

            entry.setListOrder(order++);

            tabList.addEntry(entry);
        }
    }

    private String resolveNameFormat() {
        Object profileObject = config.getProfiles().get("default");

        if (!(profileObject instanceof Map<?, ?> profile)) {
            return "{color}{name}";
        }

        Object nameFormatObject = profile.get("name-format");

        if (nameFormatObject instanceof Map<?, ?> map) {
            Object withRank = map.get("with-rank");

            if (withRank != null) {
                return String.valueOf(withRank);
            }
        }

        Object rootNameFormat = configValue("name-format");

        if (rootNameFormat instanceof Map<?, ?> map) {
            Object withRank = map.get("with-rank");

            if (withRank != null) {
                return String.valueOf(withRank);
            }

            Object noRank = map.get("no-rank");

            if (noRank != null) {
                return String.valueOf(noRank);
            }
        }

        return "{color}{name}";
    }

    private Object configValue(String key) {
        try {
            var field = VelocityTabConfig.class.getDeclaredField("root");
            field.setAccessible(true);

            Object value = field.get(config);

            if (value instanceof Map<?, ?> map) {
                return map.get(key);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private Component linesComponent(List<String> lines, Player viewer) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            String line = replaceBasicPlaceholders(lines.get(i), viewer);
            Component component = VelocityTabText.parse(line);

            if (i > 0) {
                result = result.append(Component.newline());
            }

            result = result.append(component);
        }

        return result;
    }

    private String replaceBasicPlaceholders(String text, Player viewer) {
        return text
                .replace("%player%", viewer.getUsername())
                .replace("%online%", String.valueOf(proxyServer.getPlayerCount()));
    }

    private static List<String> languageLines(Object linesObject, String language) {
        if (!(linesObject instanceof Map<?, ?> linesMap)) {
            return List.of();
        }

        Object languageObject = linesMap.get(language);

        if (!(languageObject instanceof List<?> list)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (Object value : list) {
            if (value != null) {
                result.add(String.valueOf(value));
            }
        }

        return result;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
}
