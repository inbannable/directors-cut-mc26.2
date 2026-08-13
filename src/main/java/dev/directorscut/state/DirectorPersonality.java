package dev.directorscut.state;

import dev.directorscut.event.EventCategory;

import java.util.SplittableRandom;

public record DirectorPersonality(
        double benevolence,
        double cruelty,
        double humor,
        double mystery,
        double chaos,
        double patience,
        double theatricality
) {
    public static DirectorPersonality fromSeed(long seed) {
        SplittableRandom random = new SplittableRandom(mix(seed ^ 0x4449524543544F52L));
        return new DirectorPersonality(
                shaped(random), shaped(random), shaped(random), shaped(random),
                shaped(random), shaped(random), shaped(random)
        );
    }

    private static double shaped(SplittableRandom random) {
        return clamp((random.nextDouble() + random.nextDouble()) * 0.5);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public double categoryBias(EventCategory category) {
        return switch (category) {
            case MERCY, REWARD -> 0.65 + benevolence;
            case DANGER -> 0.45 + cruelty;
            case COMEDY -> 0.5 + humor * 1.25;
            case MYSTERY, HORROR -> 0.5 + mystery;
            case CHAOS -> 0.45 + chaos;
            case CINEMATIC -> 0.55 + theatricality;
            case DISCOVERY, AMBIENT -> 0.75 + (patience + mystery) * 0.35;
        };
    }

    public long baseCooldownTicks() {
        return Math.round(7_200 + patience * 13_200); // 6-17 minutes
    }

    public String compact() {
        return String.format(
                "kind %.2f | cruel %.2f | funny %.2f | mystery %.2f | chaos %.2f | patient %.2f | theatrical %.2f",
                benevolence, cruelty, humor, mystery, chaos, patience, theatricality
        );
    }
}
