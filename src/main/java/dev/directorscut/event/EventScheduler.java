package dev.directorscut.event;

import dev.directorscut.state.DirectorPersonality;
import dev.directorscut.state.DirectorState;

import java.util.ArrayList;
import java.util.List;

public final class EventScheduler {
    private final EventRegistry registry;

    public EventScheduler(EventRegistry registry) {
        this.registry = registry;
    }

    public DirectorEvent select(EventContext context) {
        DirectorState state = context.state();
        DirectorPersonality personality = context.personality();
        List<Weighted> eligible = new ArrayList<>();
        double total = 0;

        for (DirectorEvent event : registry.all()) {
            if (!context.forced()) {
                if (!event.canTrigger(context)) continue;
                if (event.uniquePerPlayer() && state.history.hasSeen(event.id())) continue;
                if (context.tick() - state.history.lastTriggered(event.id()) < event.minimumCooldownTicks()) continue;
            }
            double weight = Math.max(0, event.weight(context));
            weight *= personality.categoryBias(event.category());
            weight *= moodBias(event.category(), state);
            weight *= 0.65 + state.engagement * 0.7;
            if (weight <= 0) continue;
            total += weight;
            eligible.add(new Weighted(event, total));
        }
        if (eligible.isEmpty()) return null;
        double roll = context.random().nextDouble(total);
        for (Weighted weighted : eligible) {
            if (roll < weighted.cumulative()) return weighted.event();
        }
        return eligible.getLast().event();
    }

    public boolean shouldAttempt(EventContext context) {
        DirectorState state = context.state();
        if (context.tick() < state.cooldownUntil || state.ticksSinceEvent(context.tick()) < 3_600) return false;
        double chance = 0.045 + Math.max(0, state.directorInterest - 28) / 145.0;
        chance *= 1.15 - context.personality().patience() * 0.35;
        return context.random().nextDouble() < Math.min(0.58, chance);
    }

    private static double moodBias(EventCategory category, DirectorState state) {
        if (state.danger > 78 && (category == EventCategory.DANGER || category == EventCategory.CHAOS)) return 0.04;
        return switch (category) {
            case MERCY -> 0.35 + state.danger / 38.0;
            case DANGER -> 0.45 + state.tension / 90.0;
            case DISCOVERY -> 0.55 + state.boredom / 75.0 + state.curiosity / 130.0;
            case COMEDY -> 0.55 + state.boredom / 95.0;
            case MYSTERY, HORROR -> 0.5 + state.mystery / 85.0 + state.tension / 180.0;
            case CINEMATIC -> 0.65 + state.curiosity / 120.0 + state.comfort / 180.0;
            case REWARD -> 0.45 + state.danger / 120.0;
            case AMBIENT -> 0.8 + state.tension / 180.0;
            case CHAOS -> 0.45 + state.boredom / 110.0;
        };
    }

    private record Weighted(DirectorEvent event, double cumulative) {
    }
}
