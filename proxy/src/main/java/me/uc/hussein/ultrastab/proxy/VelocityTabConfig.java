package me.uc.hussein.ultrastab.proxy;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VelocityTabConfig {

    private final Map<String, Object> root;

    private VelocityTabConfig(Map<String, Object> root) {
        this.root = root;
    }

    public static VelocityTabConfig load(InputStream input) {
        if (input == null) {
            throw new IllegalArgumentException("tab.yml was not found.");
        }

        Object loaded = new Yaml().load(input);

        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("tab.yml root must be a YAML map.");
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                values.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return new VelocityTabConfig(values);
    }

    public boolean isEnabled() {
        Object value = root.get("enabled");
        return !(value instanceof Boolean) || (Boolean) value;
    }

    public Map<String, Object> getGroups() {
        return map(root.get("groups"));
    }

    public Map<String, Object> getProfiles() {
        return map(root.get("profiles"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        return list.stream()
                .filter(item -> item != null)
                .map(String::valueOf)
                .toList();
    }
}
