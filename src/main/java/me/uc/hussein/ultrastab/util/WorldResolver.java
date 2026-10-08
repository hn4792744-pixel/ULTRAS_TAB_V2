package me.uc.hussein.ultrastab.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Maps a world name to the first profile that lists it, falling back to a default profile (or null). */
public final class WorldResolver<T> {

    private final Map<String, T> byWorld = new HashMap<>();
    private final T fallback;

    public WorldResolver(Collection<T> profiles, Function<T, Collection<String>> worlds, T fallback) {
        for (T profile : profiles) {
            for (String world : worlds.apply(profile)) {
                byWorld.putIfAbsent(world, profile);
            }
        }
        this.fallback = fallback;
    }

    public T resolve(String world) {
        T profile = byWorld.get(world);
        return profile != null ? profile : fallback;
    }
}
