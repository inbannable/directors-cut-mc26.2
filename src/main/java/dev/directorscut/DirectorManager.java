package dev.directorscut;

import dev.directorscut.event.DirectorEvent;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import dev.directorscut.event.EventScheduler;
import dev.directorscut.event.EventUtil;
import dev.directorscut.event.impl.BuiltInEvents;
import dev.directorscut.state.DirectorPersonality;
import dev.directorscut.state.DirectorState;
import dev.directorscut.state.DirectorStorage;
import dev.directorscut.state.MoodModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SplittableRandom;
import java.util.UUID;

public final class DirectorManager {
    private final DirectorStorage storage = new DirectorStorage();
    private final EventRegistry registry = new EventRegistry();
    private final EventScheduler scheduler = new EventScheduler(registry);
    private final EventUtil eventUtil = new EventUtil(this);
    private final PriorityQueue<ScheduledAction> scheduled = new PriorityQueue<>(Comparator.comparingLong(ScheduledAction::tick));
    private final List<Follower> followers = new ArrayList<>();

    private MinecraftServer server;
    private DirectorPersonality personality = DirectorPersonality.fromSeed(0);
    private long tick;

    public DirectorManager() {
        BuiltInEvents.registerAll(registry);
    }

    public void start(MinecraftServer server) {
        this.server = server;
        this.tick = server.overworld().getGameTime();
        this.personality = DirectorPersonality.fromSeed(server.overworld().getSeed());
        storage.open(server);
        System.out.println("[Director's Cut] Watching. " + registry.all().size() + " events registered.");
    }

    public void stop(MinecraftServer ignored) {
        storage.close();
        scheduled.clear();
        followers.clear();
        server = null;
    }

    public void tick(MinecraftServer server) {
        if (this.server == null) start(server);
        tick = server.overworld().getGameTime();
        while (!scheduled.isEmpty() && scheduled.peek().tick() <= tick) {
            try {
                scheduled.remove().action().run();
            } catch (RuntimeException exception) {
                System.err.println("[Director's Cut] Delayed action failed: " + exception.getMessage());
            }
        }
        if (tick % 20 == 0) {
            updateFollowers();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) observe(player);
        }
        if (tick % 600 == 37) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) attemptEvent(player);
        }
        if (tick % 6_000 == 101) storage.save();
    }

    private void observe(ServerPlayer player) {
        DirectorState state = state(player);
        ServerLevel level = player.level();
        BlockPos position = player.blockPosition();

        if (state.lastObservedTick == 0) {
            state.lastX = player.getX();
            state.lastY = player.getY();
            state.lastZ = player.getZ();
            state.lastHealth = player.getHealth();
        }

        double moved = Math.sqrt(player.distanceToSqr(state.lastX, state.lastY, state.lastZ));
        state.recentTravel = state.recentTravel * 0.84 + moved;
        state.stationaryTicks = moved < 0.55 ? state.stationaryTicks + 20 : 0;
        state.lastX = player.getX();
        state.lastY = player.getY();
        state.lastZ = player.getZ();
        state.lastObservedTick = tick;

        long chunk = ChunkPos.pack(position.getX() >> 4, position.getZ() >> 4);
        state.exploringNewChunk = chunk != state.lastChunk && !state.recentlyVisitedChunks.contains(chunk);
        state.lastChunk = chunk;
        state.recentlyVisitedChunks.add(chunk);
        while (state.recentlyVisitedChunks.size() > 160) {
            Iterator<Long> iterator = state.recentlyVisitedChunks.iterator();
            iterator.next();
            iterator.remove();
        }

        float health = player.getHealth();
        if (health < state.lastHealth - 0.05f) {
            state.lastDamageTick = tick;
            state.lastCombatTick = tick;
            state.recentDamage += state.lastHealth - health;
            if (health <= 4 && state.lastHealth > 4) state.nearDeaths++;
        }
        state.lastHealth = health;
        state.recentDamage *= 0.93;
        if (tick % 200 == 0) {
            state.recentKills = Math.max(0, state.recentKills - 1);
            state.recentChickenKills = Math.max(0, state.recentChickenKills - 1);
        }

        boolean sleeping = player.isSleeping();
        if (sleeping && !state.wasSleeping) state.sleeps++;
        state.wasSleeping = sleeping;
        if (state.mining(tick)) state.miningTicks += 20;
        else state.miningTicks = Math.max(0, state.miningTicks - 40);

        state.nearbyHostiles = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(18, 10, 18), Entity::isAlive).size();
        state.underground = !level.canSeeSky(position.above()) || player.getY() < level.getSeaLevel() - 12;
        state.dark = level.getMaxLocalRawBrightness(position) < 7;
        state.currentHealth = health;
        state.currentFood = player.getFoodData().getFoodLevel();
        state.armorQuality = player.getArmorValue();
        state.toolQuality = toolQuality(player.getMainHandItem().getItem());
        state.inventoryValue = inventoryValue(player);
        state.nearVillage = level.isCloseToVillage(position, 3);
        state.biome = level.getBiome(position).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
        state.dimension = level.dimension().identifier().toString();

        BlockPos home = level.getRespawnData().pos();
        double homeDistance = Math.sqrt(position.distSqr(home));
        if (homeDistance > 180) state.wasFarFromHome = true;
        state.returningHome = state.wasFarFromHome && homeDistance < 48
                && state.previousHomeDistance > homeDistance + 6;
        state.previousHomeDistance = homeDistance;

        MoodModel.update(state, new MoodModel.Inputs(
                state.underground,
                state.dark,
                state.stationaryTicks > 240,
                state.exploringNewChunk,
                state.mining(tick),
                state.inCombat(tick),
                homeDistance < 48,
                health / Math.max(1, player.getMaxHealth()),
                state.currentFood,
                state.nearbyHostiles,
                state.recentTravel,
                state.ticksSinceEvent(tick)
        ));
        state.updateEngagement(player.getX(), player.getZ(), tick);

        if (state.debug && tick % 100 == 0) {
            player.sendOverlayMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "Director  B %.0f  T %.0f  D %.0f  C %.0f  M %.0f  I %.0f",
                    state.boredom, state.tension, state.danger, state.curiosity, state.mystery, state.directorInterest)));
        }
    }

    private void attemptEvent(ServerPlayer player) {
        DirectorState state = state(player);
        EventContext context = context(player, false);
        if (!scheduler.shouldAttempt(context)) return;
        DirectorEvent selected = scheduler.select(context);
        if (selected != null) trigger(context, selected);
    }

    private EventContext context(ServerPlayer player, boolean forced) {
        long seed = player.level().getSeed() ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 21) ^ tick;
        return new EventContext(this, player, state(player), personality, tick, new SplittableRandom(seed), forced);
    }

    private boolean trigger(EventContext context, DirectorEvent event) {
        try {
            if (!event.trigger(context)) {
                if (context.state().debug) context.player().sendSystemMessage(Component.literal("[Director] " + event.id() + " could not find a safe setup."));
                return false;
            }
            DirectorState state = context.state();
            double mysteryGain = switch (event.category()) {
                case MYSTERY, HORROR -> 16;
                case COMEDY, CHAOS -> 7;
                default -> 4;
            };
            state.markEvent(tick, event.id(), event.uniquePerPlayer(), mysteryGain);
            long cooldown = personality.baseCooldownTicks();
            if (event.major()) cooldown += 4_800;
            if (event.category() == dev.directorscut.event.EventCategory.MERCY) cooldown = Math.min(cooldown, 8_000);
            state.cooldownUntil = tick + cooldown;
            if (state.debug) {
                context.player().sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                        "[Director] selected %s (%s), next window in %.1f min",
                        event.id(), event.category(), cooldown / 1_200.0)));
            }
            event.cleanup(context);
            return true;
        } catch (RuntimeException exception) {
            System.err.println("[Director's Cut] Event " + event.id() + " failed: " + exception);
            if (context.state().debug) context.player().sendSystemMessage(Component.literal("[Director] event failed: " + exception.getMessage()));
            return false;
        }
    }

    public boolean triggerById(ServerPlayer player, String id) {
        DirectorEvent event = registry.get(id);
        return event != null && trigger(context(player, true), event);
    }

    public boolean triggerRandom(ServerPlayer player) {
        EventContext context = context(player, false);
        DirectorEvent event = scheduler.select(context);
        return event != null && trigger(context, event);
    }

    public void onBlockBroken(ServerPlayer player) {
        DirectorState state = state(player);
        state.lastBlockBrokenTick = currentTick(player);
    }

    public void onDamage(ServerPlayer player, float damage) {
        DirectorState state = state(player);
        state.lastDamageTick = currentTick(player);
        state.lastCombatTick = state.lastDamageTick;
        state.recentDamage += damage;
    }

    public void onKill(ServerPlayer player, LivingEntity killed) {
        DirectorState state = state(player);
        state.lastCombatTick = currentTick(player);
        state.recentKills++;
        if (killed instanceof Chicken) state.recentChickenKills++;
    }

    public void onDeath(ServerPlayer player) {
        DirectorState state = state(player);
        state.deaths++;
        state.danger = 15;
        state.tension = 5;
        state.cooldownUntil = currentTick(player) + 3_600;
    }

    public DirectorState state(ServerPlayer player) {
        return storage.state(player.getUUID());
    }

    public long currentTick(ServerPlayer player) {
        return player.level().getGameTime();
    }

    public int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    private static int toolQuality(Item item) {
        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_PICKAXE) return 10;
        if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_PICKAXE) return 8;
        if (item == Items.IRON_SWORD || item == Items.IRON_PICKAXE) return 5;
        if (item == Items.STONE_SWORD || item == Items.STONE_PICKAXE) return 3;
        return 1;
    }

    private static int inventoryValue(ServerPlayer player) {
        int value = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            int multiplier = stack.getItem() == Items.DIAMOND || stack.getItem() == Items.NETHERITE_INGOT ? 12
                    : stack.getItem() == Items.EMERALD || stack.getItem() == Items.GOLD_INGOT ? 5
                    : stack.getItem() == Items.IRON_INGOT ? 3 : 1;
            value += Math.min(64, stack.getCount()) * multiplier;
        }
        return value;
    }

    public void schedule(long dueTick, Runnable action) {
        scheduled.add(new ScheduledAction(dueTick, action));
    }

    public void registerFollower(Chicken chicken, ServerPlayer owner, long untilTick) {
        followers.add(new Follower(chicken, owner.getUUID(), untilTick));
    }

    private void updateFollowers() {
        if (server == null) return;
        Iterator<Follower> iterator = followers.iterator();
        while (iterator.hasNext()) {
            Follower follower = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(follower.owner());
            if (!follower.chicken().isAlive() || owner == null || tick > follower.untilTick()) {
                iterator.remove();
                continue;
            }
            var towardChicken = follower.chicken().position().subtract(owner.getEyePosition()).normalize();
            boolean watched = owner.getLookAngle().dot(towardChicken) > 0.94 && owner.hasLineOfSight(follower.chicken());
            if (watched) follower.chicken().getNavigation().stop();
            else follower.chicken().getNavigation().moveTo(owner, 1.05);
        }
    }

    public String status(ServerPlayer player) {
        DirectorState state = state(player);
        return String.format(java.util.Locale.ROOT,
                "mood B %.1f T %.1f D %.1f C %.1f M %.1f comfort %.1f interest %.1f | hp %.1f food %d armor %d tool %d value %d hostiles %d | %s %s | cooldown %.1fs | personality %s",
                state.boredom, state.tension, state.danger, state.curiosity, state.mystery, state.comfort, state.directorInterest,
                state.currentHealth, state.currentFood, state.armorQuality, state.toolQuality, state.inventoryValue, state.nearbyHostiles,
                state.biome, state.dimension, Math.max(0, state.cooldownUntil - tick) / 20.0, personality.compact());
    }

    public EventUtil eventUtil() { return eventUtil; }
    public EventRegistry registry() { return registry; }
    public DirectorPersonality personality() { return personality; }
    public MinecraftServer server() { return server; }

    private record ScheduledAction(long tick, Runnable action) {
    }

    private record Follower(Chicken chicken, UUID owner, long untilTick) {
    }
}
