package dev.directorscut.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class EventHistory {
    private final Map<String, Long> lastTriggered = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Set<String> uniqueSeen = new HashSet<>();

    public long lastTriggered(String id) {
        return lastTriggered.getOrDefault(id, Long.MIN_VALUE / 4);
    }

    public int count(String id) {
        return counts.getOrDefault(id, 0);
    }

    public boolean hasSeen(String id) {
        return uniqueSeen.contains(id);
    }

    public void record(String id, long tick, boolean unique) {
        lastTriggered.put(id, tick);
        counts.merge(id, 1, Integer::sum);
        if (unique) uniqueSeen.add(id);
    }

    public void clearCooldowns() {
        lastTriggered.clear();
    }

    public void load(Properties properties, String prefix) {
        uniqueSeen.clear();
        String seen = properties.getProperty(prefix + "seen", "");
        if (!seen.isBlank()) {
            for (String id : seen.split(",")) {
                if (!id.isBlank()) uniqueSeen.add(id);
            }
        }
        readLongMap(properties.getProperty(prefix + "last", ""), lastTriggered);
        readIntMap(properties.getProperty(prefix + "counts", ""), counts);
    }

    public void save(Properties properties, String prefix) {
        properties.setProperty(prefix + "seen", String.join(",", uniqueSeen));
        properties.setProperty(prefix + "last", writeMap(lastTriggered));
        properties.setProperty(prefix + "counts", writeMap(counts));
    }

    private static void readLongMap(String encoded, Map<String, Long> target) {
        target.clear();
        if (encoded.isBlank()) return;
        for (String pair : encoded.split(",")) {
            int separator = pair.lastIndexOf(':');
            if (separator <= 0) continue;
            try {
                target.put(pair.substring(0, separator), Long.parseLong(pair.substring(separator + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void readIntMap(String encoded, Map<String, Integer> target) {
        target.clear();
        if (encoded.isBlank()) return;
        for (String pair : encoded.split(",")) {
            int separator = pair.lastIndexOf(':');
            if (separator <= 0) continue;
            try {
                target.put(pair.substring(0, separator), Integer.parseInt(pair.substring(separator + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static String writeMap(Map<String, ? extends Number> source) {
        StringBuilder result = new StringBuilder();
        source.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (!result.isEmpty()) result.append(',');
            result.append(entry.getKey()).append(':').append(entry.getValue());
        });
        return result.toString();
    }
}
