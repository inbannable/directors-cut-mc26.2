package dev.directorscut.event.impl;

import dev.directorscut.event.DirectorEvent;
import dev.directorscut.event.EventCategory;

abstract class BaseEvent implements DirectorEvent {
    private final String id;
    private final EventCategory category;
    private final long cooldown;
    private final boolean unique;
    private final boolean permanent;
    private final boolean major;

    BaseEvent(String id, EventCategory category, long cooldown, boolean unique, boolean permanent, boolean major) {
        this.id = id;
        this.category = category;
        this.cooldown = cooldown;
        this.unique = unique;
        this.permanent = permanent;
        this.major = major;
    }

    @Override public final String id() { return id; }
    @Override public final EventCategory category() { return category; }
    @Override public final long minimumCooldownTicks() { return cooldown; }
    @Override public final boolean uniquePerPlayer() { return unique; }
    @Override public final boolean permanentlyModifiesWorld() { return permanent; }
    @Override public final boolean major() { return major; }
}
