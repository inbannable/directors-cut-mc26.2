package dev.directorscut.event.impl;

import dev.directorscut.event.EventCategory;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

final class ComedyEvents {
    private ComedyEvents() {
    }

    static void register(EventRegistry registry) {
        registry.register(new SheepMeeting());
        registry.register(new Gerald());
        registry.register(new FalseBoss());
        registry.register(new WrongChest());
        registry.register(new RevengeOfChickens());
        registry.register(new TinySiege());
    }

    private static final class SheepMeeting extends BaseEvent {
        SheepMeeting() { super("sheep_meeting", EventCategory.COMEDY, 60_000, true, false, false); }

        @Override public boolean canTrigger(EventContext context) {
            long dayTime = context.level().getOverworldClockTime() % 24_000;
            return dayTime > 13_000 && dayTime < 22_500 && context.level().canSeeSky(context.player().blockPosition().above());
        }

        @Override public boolean trigger(EventContext context) {
            BlockPos center = context.util().surfaceNear(context, 24, 42);
            if (!context.util().safeToModify(context, center, 22)) return false;
            int spawned = 0;
            for (int index = 0; index < 10; index++) {
                double angle = Math.PI * 2 * index / 10.0;
                int x = center.getX() + (int) Math.round(Math.cos(angle) * 4);
                int z = center.getZ() + (int) Math.round(Math.sin(angle) * 4);
                int y = context.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                var sheep = context.util().spawn(context, EntityTypes.SHEEP, new BlockPos(x, y, z));
                if (sheep != null) {
                    sheep.getLookControl().setLookAt(center.getX() + 0.5, center.getY() + 0.8, center.getZ() + 0.5);
                    context.util().freezeThenRelease(sheep, context.tick() + 240);
                    spawned++;
                }
            }
            if (spawned == 0) return false;
            context.state().setLead(center.getX(), center.getZ(), Math.sqrt(context.player().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)), context.tick() + 12_000);
            return true;
        }
    }

    private static final class Gerald extends BaseEvent {
        Gerald() { super("gerald", EventCategory.COMEDY, 120_000, true, false, false); }

        @Override public boolean canTrigger(EventContext context) {
            return !context.level().canSeeSky(context.player().blockPosition().above()) && context.player().getY() < 42;
        }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.player().blockPosition().offset(
                    context.random().nextInt(-8, 9), 0, context.random().nextInt(-8, 9));
            var cow = context.util().spawn(context, EntityTypes.COW, position);
            if (cow == null) return false;
            cow.setCustomName(Component.literal("Gerald"));
            cow.setCustomNameVisible(true);
            return true;
        }
    }

    private static final class FalseBoss extends BaseEvent {
        FalseBoss() { super("false_boss", EventCategory.COMEDY, 48_000, false, false, true); }

        @Override public boolean canTrigger(EventContext context) { return context.state().tension > 35 && context.state().danger < 55; }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.util().surfaceNear(context, 10, 18);
            context.level().playSound(null, context.player().blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.8f, 0.65f);
            context.level().sendParticles(ParticleTypes.OMINOUS_SPAWNING, position.getX(), position.getY() + 1, position.getZ(),
                    45, 1.2, 1.0, 1.2, 0.03);
            var chicken = context.util().spawn(context, EntityTypes.CHICKEN, position);
            if (chicken == null) return false;
            chicken.setCustomName(Component.literal("Harold, Devourer of Worlds"));
            chicken.setCustomNameVisible(true);
            return true;
        }
    }

    private static final class WrongChest extends BaseEvent {
        WrongChest() { super("wrong_chest", EventCategory.COMEDY, 72_000, true, true, false); }

        @Override public boolean canTrigger(EventContext context) { return context.state().curiosity > 30; }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.level().canSeeSky(context.player().blockPosition().above())
                    ? context.util().surfaceNear(context, 24, 45)
                    : context.player().blockPosition().offset(context.random().nextInt(-9, 10), 0, context.random().nextInt(-9, 10));
            List<ItemStack> hoes = new ArrayList<>();
            for (int slot = 0; slot < 27; slot++) hoes.add(new ItemStack(Items.WOODEN_HOE));
            if (!context.util().placeChest(context.level(), position, hoes)) return false;
            context.state().setLead(position.getX(), position.getZ(), Math.sqrt(context.player().distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5)), context.tick() + 12_000);
            return true;
        }
    }

    private static final class RevengeOfChickens extends BaseEvent {
        RevengeOfChickens() { super("revenge_of_chickens", EventCategory.CHAOS, 48_000, false, false, false); }

        @Override public boolean canTrigger(EventContext context) { return context.state().recentChickenKills >= 8; }
        @Override public double weight(EventContext context) { return 1.0 + context.state().recentChickenKills / 4.0; }

        @Override public boolean trigger(EventContext context) {
            int spawned = 0;
            for (int index = 0; index < 14; index++) {
                double angle = Math.PI * 2 * index / 14.0;
                BlockPos around = context.player().blockPosition().offset(
                        (int) Math.round(Math.cos(angle) * 9), 0, (int) Math.round(Math.sin(angle) * 9));
                int y = context.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        around.getX(), around.getZ());
                var chicken = context.util().spawn(context, EntityTypes.CHICKEN, new BlockPos(around.getX(), y, around.getZ()));
                if (chicken != null) {
                    context.util().registerFollower(chicken, context.player(), context.tick() + 1_200);
                    spawned++;
                }
            }
            context.state().recentChickenKills = 0;
            return spawned > 0;
        }
    }

    private static final class TinySiege extends BaseEvent {
        TinySiege() { super("very_small_zombie_siege", EventCategory.COMEDY, 36_000, false, false, false); }

        @Override public boolean canTrigger(EventContext context) { return context.state().comfort > 50 && context.state().danger < 45; }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.util().surfaceNear(context, 11, 18);
            context.level().playSound(null, context.player().blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                    SoundSource.HOSTILE, 1.5f, 0.7f);
            var zombie = context.util().spawn(context, EntityTypes.ZOMBIE, position);
            if (zombie == null) return false;
            zombie.setBaby(true);
            zombie.setCustomName(Component.literal("THE HORDE"));
            zombie.setCustomNameVisible(true);
            return true;
        }
    }
}
