package dev.directorscut.event.impl;

import dev.directorscut.event.EventCategory;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;

final class MercyEvents {
    private MercyEvents() {
    }

    static void register(EventRegistry registry) {
        registry.register(new Mercy());
        registry.register(new ConvenientChest());
        registry.register(new Homecoming());
        registry.register(new HelpfulSkeleton());
    }

    private static final class Mercy extends BaseEvent {
        Mercy() { super("mercy", EventCategory.MERCY, 24_000, false, true, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.player().getHealth() <= 6.0f
                    && context.tick() - context.state().lastDamageTick < 300
                    && context.state().danger > 65;
        }

        @Override public double weight(EventContext context) { return 2.0 + context.personality().benevolence(); }

        @Override public boolean trigger(EventContext context) {
            if (context.level().isRaining() && context.random().nextBoolean()) {
                context.manager().server().setWeatherParameters(6_000, 0, false, false);
                return true;
            }
            BlockPos position;
            if (context.level().canSeeSky(context.player().blockPosition().above())) {
                position = context.util().surfaceNear(context, 11, 18);
            } else {
                position = context.player().blockPosition().offset(context.random().nextInt(-8, 9), 0, context.random().nextInt(-8, 9));
            }
            return context.util().placeChest(context.level(), position, context.util().stacks(
                    Items.BREAD, 3, Items.COOKED_CHICKEN, 2, Items.TORCH, 5, Items.STRING, 2));
        }
    }

    private static final class ConvenientChest extends BaseEvent {
        ConvenientChest() { super("convenient_chest", EventCategory.REWARD, 36_000, false, true, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.state().danger > 52
                    && (context.manager().countItem(context.player(), Items.IRON_INGOT) < 3
                    || context.player().getFoodData().getFoodLevel() < 8);
        }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.level().canSeeSky(context.player().blockPosition().above())
                    ? context.util().surfaceNear(context, 16, 28)
                    : context.player().blockPosition().offset(context.random().nextInt(-10, 11), 0, context.random().nextInt(-10, 11));
            if (!context.util().placeChest(context.level(), position, context.util().stacks(
                    Items.IRON_INGOT, 4,
                    Items.BREAD, 2,
                    Items.STRING, 3,
                    Items.WOODEN_SHOVEL, 1,
                    Items.POISONOUS_POTATO, 1))) return false;
            context.state().setLead(position.getX(), position.getZ(), Math.sqrt(context.player().distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5)), context.tick() + 10_000);
            return true;
        }
    }

    private static final class Homecoming extends BaseEvent {
        Homecoming() { super("homecoming", EventCategory.AMBIENT, 36_000, false, true, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.state().returningHome && context.state().comfort > 42;
        }

        @Override public boolean trigger(EventContext context) {
            BlockPos spawn = context.level().getRespawnData().pos();
            int changes = 0;
            for (int index = 0; index < 7; index++) {
                int x = spawn.getX() + context.random().nextInt(-6, 7);
                int z = spawn.getZ() + context.random().nextInt(-6, 7);
                int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos flower = new BlockPos(x, y, z);
                if (context.level().getBlockState(flower).canBeReplaced()) {
                    context.level().setBlockAndUpdate(flower, index % 2 == 0
                            ? Blocks.POPPY.defaultBlockState() : Blocks.DANDELION.defaultBlockState());
                    changes++;
                }
            }
            if (context.random().nextDouble() < 0.55) {
                int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX() + 3, spawn.getZ());
                var cat = context.util().spawn(context, EntityTypes.CAT, new BlockPos(spawn.getX() + 3, y, spawn.getZ()));
                if (cat != null) changes++;
            }
            context.state().returningHome = false;
            context.state().wasFarFromHome = false;
            return changes > 0;
        }
    }

    private static final class HelpfulSkeleton extends BaseEvent {
        HelpfulSkeleton() { super("helpful_skeleton", EventCategory.MERCY, 72_000, false, false, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.state().inCombat(context.tick()) && context.player().getHealth() < 12;
        }

        @Override public double weight(EventContext context) { return 0.18 + context.personality().benevolence() * 0.25; }

        @Override public boolean trigger(EventContext context) {
            var nearby = context.level().getEntitiesOfClass(Monster.class,
                    context.player().getBoundingBox().inflate(18), monster -> monster.isAlive());
            Monster target = nearby.stream().min(Comparator.comparingDouble(context.player()::distanceToSqr)).orElse(null);
            if (target == null) return false;
            BlockPos position = context.player().blockPosition().offset(context.random().nextInt(-7, 8), 0, context.random().nextInt(-7, 8));
            var skeleton = context.util().spawn(context, EntityTypes.SKELETON, position);
            if (skeleton == null) return false;
            skeleton.setTarget(target);
            skeleton.setCustomName(Component.literal("?"));
            context.manager().schedule(context.tick() + 240, () -> {
                if (skeleton.isAlive() && context.player().isAlive()) skeleton.setTarget(context.player());
            });
            return true;
        }
    }
}
