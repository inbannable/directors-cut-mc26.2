package dev.directorscut.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventRegistry {
    private final Map<String, DirectorEvent> events = new LinkedHashMap<>();

    public void register(DirectorEvent event) {
        if (events.putIfAbsent(event.id(), event) != null) {
            throw new IllegalArgumentException("Duplicate Director event id: " + event.id());
        }
    }

    public DirectorEvent get(String id) {
        return events.get(id);
    }

    public List<DirectorEvent> all() {
        return Collections.unmodifiableList(new ArrayList<>(events.values()));
    }

    public String ids() {
        return String.join(", ", events.keySet());
    }
}
