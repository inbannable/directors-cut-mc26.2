package dev.directorscut.state;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.UUID;

public final class DirectorState {
    public final UUID playerId;
    public final EventHistory history = new EventHistory();

    public double tension = 8;
    public double boredom = 10;
    public double danger = 5;
    public double mystery = 8;
    public double comfort = 20;
    public double curiosity = 18;
    public double directorInterest = 8;
    public double engagement = 0.5;

    public float lastHealth = 20;
    public double lastX;
    public double lastY;
    public double lastZ;
    public double recentTravel;
    public long stationaryTicks;
    public long miningTicks;
    public long lastBlockBrokenTick = Long.MIN_VALUE / 4;
    public long lastCombatTick = Long.MIN_VALUE / 4;
    public long lastDamageTick = Long.MIN_VALUE / 4;
    public double recentDamage;
    public int recentKills;
    public int recentChickenKills;
    public int deaths;
    public int nearDeaths;
    public int sleeps;
    public boolean wasSleeping;
    public long lastObservedTick;
    public long lastEventTick;
    public long cooldownUntil;
    public boolean debug;
    public boolean hintsEnabled = true;
    public String lastHint = "还没有值得留意的迹象。";

    public float currentHealth;
    public int currentFood;
    public int armorQuality;
    public int toolQuality;
    public int inventoryValue;
    public int nearbyHostiles;
    public boolean underground;
    public boolean dark;
    public boolean nearVillage;
    public String biome = "unknown";
    public String dimension = "unknown";

    public long lastChunk = Long.MIN_VALUE;
    public final LinkedHashSet<Long> recentlyVisitedChunks = new LinkedHashSet<>();
    public boolean exploringNewChunk;
    public boolean wasFarFromHome;
    public double previousHomeDistance;
    public boolean returningHome;

    public boolean activeLead;
    public double leadX;
    public double leadZ;
    public double leadInitialDistance;
    public long leadExpiry;

    public int northChainStage;
    public double northOriginZ;
    public long northChainExpiry;

    public DirectorState(UUID playerId) {
        this.playerId = playerId;
    }

    public long ticksSinceEvent(long now) {
        return Math.max(0, now - lastEventTick);
    }

    public boolean inCombat(long now) {
        return now - lastCombatTick < 240 || now - lastDamageTick < 200;
    }

    public boolean mining(long now) {
        return now - lastBlockBrokenTick < 80;
    }

    public void markEvent(long tick, String id, boolean unique, double mysteryGain) {
        lastEventTick = tick;
        history.record(id, tick, unique);
        mystery = MoodModel.clamp(mystery + mysteryGain);
        boredom *= 0.42;
        directorInterest *= 0.25;
    }

    public void setLead(double x, double z, double initialDistance, long expiry) {
        activeLead = true;
        leadX = x;
        leadZ = z;
        leadInitialDistance = Math.max(1, initialDistance);
        leadExpiry = expiry;
    }

    public void updateEngagement(double playerX, double playerZ, long tick) {
        if (!activeLead) return;
        double currentDistance = Math.hypot(playerX - leadX, playerZ - leadZ);
        if (currentDistance < Math.max(9, leadInitialDistance * 0.28)) {
            engagement = Math.min(1.0, engagement + 0.13);
            curiosity = MoodModel.clamp(curiosity + 18);
            activeLead = false;
        } else if (tick > leadExpiry) {
            engagement = Math.max(0.05, engagement - 0.07);
            activeLead = false;
        }
    }

    public void load(Properties properties) {
        String prefix = "player." + playerId + ".";
        tension = decimal(properties, prefix + "tension", tension);
        boredom = decimal(properties, prefix + "boredom", boredom);
        danger = decimal(properties, prefix + "danger", danger);
        mystery = decimal(properties, prefix + "mystery", mystery);
        comfort = decimal(properties, prefix + "comfort", comfort);
        curiosity = decimal(properties, prefix + "curiosity", curiosity);
        directorInterest = decimal(properties, prefix + "interest", directorInterest);
        engagement = decimal(properties, prefix + "engagement", engagement);
        lastEventTick = whole(properties, prefix + "last_event", 0);
        cooldownUntil = whole(properties, prefix + "cooldown", 0);
        deaths = integer(properties, prefix + "deaths", 0);
        nearDeaths = integer(properties, prefix + "near_deaths", 0);
        sleeps = integer(properties, prefix + "sleeps", 0);
        hintsEnabled = Boolean.parseBoolean(properties.getProperty(prefix + "hints", "true"));
        lastHint = properties.getProperty(prefix + "last_hint", lastHint);
        northChainStage = integer(properties, prefix + "north_stage", 0);
        northOriginZ = decimal(properties, prefix + "north_z", 0);
        northChainExpiry = whole(properties, prefix + "north_expiry", 0);
        history.load(properties, prefix + "history.");
    }

    public void save(Properties properties) {
        String prefix = "player." + playerId + ".";
        properties.setProperty(prefix + "tension", number(tension));
        properties.setProperty(prefix + "boredom", number(boredom));
        properties.setProperty(prefix + "danger", number(danger));
        properties.setProperty(prefix + "mystery", number(mystery));
        properties.setProperty(prefix + "comfort", number(comfort));
        properties.setProperty(prefix + "curiosity", number(curiosity));
        properties.setProperty(prefix + "interest", number(directorInterest));
        properties.setProperty(prefix + "engagement", number(engagement));
        properties.setProperty(prefix + "last_event", Long.toString(lastEventTick));
        properties.setProperty(prefix + "cooldown", Long.toString(cooldownUntil));
        properties.setProperty(prefix + "deaths", Integer.toString(deaths));
        properties.setProperty(prefix + "near_deaths", Integer.toString(nearDeaths));
        properties.setProperty(prefix + "sleeps", Integer.toString(sleeps));
        properties.setProperty(prefix + "hints", Boolean.toString(hintsEnabled));
        properties.setProperty(prefix + "last_hint", lastHint);
        properties.setProperty(prefix + "north_stage", Integer.toString(northChainStage));
        properties.setProperty(prefix + "north_z", number(northOriginZ));
        properties.setProperty(prefix + "north_expiry", Long.toString(northChainExpiry));
        history.save(properties, prefix + "history.");
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private static double decimal(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long whole(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int integer(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
