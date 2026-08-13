package dev.directorscut.event.impl;

import dev.directorscut.event.EventRegistry;

public final class BuiltInEvents {
    private BuiltInEvents() {
    }

    public static void registerAll(EventRegistry registry) {
        MysteryEvents.register(registry);
        ComedyEvents.register(registry);
        DiscoveryEvents.register(registry);
        MercyEvents.register(registry);
        CinematicEvents.register(registry);
    }
}
