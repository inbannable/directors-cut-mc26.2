package dev.directorscut.event;

public interface DirectorEvent {
    String id();

    EventCategory category();

    default double weight(EventContext context) {
        return 1.0;
    }

    default long minimumCooldownTicks() {
        return 12_000;
    }

    default boolean canTrigger(EventContext context) {
        return true;
    }

    default boolean uniquePerPlayer() {
        return false;
    }

    default boolean permanentlyModifiesWorld() {
        return false;
    }

    default boolean major() {
        return false;
    }

    boolean trigger(EventContext context);

    default void cleanup(EventContext context) {
    }
}
