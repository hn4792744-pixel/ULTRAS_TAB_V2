package me.uc.hussein.ultrastab.proxy;

import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class VelocityTabRanks {

    private final List<VelocityTabRank> ranks;
    private final String permissionPrefix;

    public VelocityTabRanks(VelocityTabConfig config) {
        this.permissionPrefix = "ultrastab.group.";

        List<VelocityTabRank> loaded = new ArrayList<>();

        for (Map.Entry<String, Object> entry : config.getGroups().entrySet()) {
            String key = entry.getKey();

            if (!(entry.getValue() instanceof Map<?, ?> group)) {
                continue;
            }

            String displayEn = text(group.get("display"), "en");
            String displayAr = text(group.get("display"), "ar");
            Object colorValue = group.get("color");
            String color = colorValue == null ? "#FFFFFF" : String.valueOf(colorValue);
            String permission = permissionPrefix + key;

            loaded.add(new VelocityTabRank(
                    key,
                    displayEn,
                    displayAr,
                    color,
                    permission
            ));
        }

        this.ranks = Collections.unmodifiableList(loaded);
    }

    public List<VelocityTabRank> getRanks() {
        return ranks;
    }

    public VelocityTabRank resolve(Player player) {
        VelocityTabRank fallback = null;

        for (VelocityTabRank rank : ranks) {
            if (rank.key().equalsIgnoreCase("default")) {
                fallback = rank;
                continue;
            }

            if (player.hasPermission(rank.permission())) {
                return rank;
            }
        }

        if (fallback != null) {
            return fallback;
        }

        return ranks.isEmpty()
                ? new VelocityTabRank("default", "", "", "#FFFFFF", permissionPrefix + "default")
                : ranks.get(ranks.size() - 1);
    }

    private static String text(Object display, String language) {
        if (!(display instanceof Map<?, ?> map)) {
            return "";
        }

        Object value = map.get(language);
        return value == null ? "" : String.valueOf(value);
    }
}
