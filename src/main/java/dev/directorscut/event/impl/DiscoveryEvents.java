package dev.directorscut.event.impl;

import dev.directorscut.event.EventCategory;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

final class DiscoveryEvents {
    private DiscoveryEvents() {
    }

    static void register(EventRegistry registry) {
        registry.register(new Camp());
        registry.register(new Tunnel());
        registry.register(new PerfectView());
        registry.register(new NorthFollowup());
    }

    static boolean buildCamp(EventContext context, BlockPos center, String signMessage) {
        if (!context.util().safeToModify(context, center, 26)) return false;
        context.level().setBlockAndUpdate(center, Blocks.CAMPFIRE.defaultBlockState());
        context.level().setBlockAndUpdate(center.offset(2, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        context.level().setBlockAndUpdate(center.offset(-2, 0, 0), Blocks.FURNACE.defaultBlockState());
        context.util().placeChest(context.level(), center.offset(0, 0, 2), context.util().stacks(
                Items.BREAD, 4,
                Items.TORCH, 11,
                Items.COOKED_BEEF, 2,
                Items.IRON_PICKAXE, 1,
                Items.COAL, 6
        ));
        context.util().placeSign(context.level(), center.offset(0, 0, -2), signMessage);
        return true;
    }

    private static final class Camp extends BaseEvent {
        Camp() { super("the_camp", EventCategory.DISCOVERY, 36_000, false, true, true); }

        @Override public boolean canTrigger(EventContext context) {
            return context.level().canSeeSky(context.player().blockPosition().above())
                    && (context.state().curiosity > 42 || context.state().recentTravel > 70);
        }

        @Override public double weight(EventContext context) { return 0.6 + context.state().curiosity / 90.0; }

        @Override public boolean trigger(EventContext context) {
            BlockPos center = context.util().surfaceNear(context, 42, 72);
            String[] messages = {
                    "Day 12. Still heading east.",
                    "The caves are louder tonight.",
                    "If you find this, take the food.",
                    "I should not have followed the torches."
            };
            if (!buildCamp(context, center, messages[context.random().nextInt(messages.length)])) return false;
            context.state().setLead(center.getX(), center.getZ(), Math.sqrt(context.player().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)), context.tick() + 18_000);
            return true;
        }
    }

    private static final class Tunnel extends BaseEvent {
        Tunnel() { super("the_tunnel", EventCategory.DISCOVERY, 48_000, false, true, true); }

        @Override public boolean canTrigger(EventContext context) {
            return !context.level().canSeeSky(context.player().blockPosition().above())
                    && context.state().mining(context.tick()) && context.state().boredom > 42;
        }

        @Override public boolean trigger(EventContext context) {
            Direction direction = context.util().horizontalDirection(context.player());
            BlockPos start = context.player().blockPosition().relative(direction, 7);
            int length = context.random().nextInt(30, 51);
            if (!context.util().carveTunnel(context, start, direction, length, "You weren't", "supposed to", "find this.")) return false;
            if (context.random().nextDouble() < 0.55) {
                context.util().placeChest(context.level(), start.relative(direction, length - 3), context.util().stacks(
                        Items.IRON_INGOT, 4, Items.BREAD, 2, Items.REDSTONE, 9, Items.BONE, 3));
            }
            return true;
        }
    }

    private static final class PerfectView extends BaseEvent {
        PerfectView() { super("perfect_view", EventCategory.CINEMATIC, 48_000, false, true, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.level().canSeeSky(context.player().blockPosition().above()) && context.state().curiosity > 55;
        }

        @Override public boolean trigger(EventContext context) {
            BlockPos origin = context.player().blockPosition();
            BlockPos best = null;
            int bestHeight = origin.getY();
            for (int sample = 0; sample < 16; sample++) {
                double angle = sample * Math.PI * 2.0 / 16.0;
                int x = origin.getX() + (int) Math.round(Math.cos(angle) * 72);
                int z = origin.getZ() + (int) Math.round(Math.sin(angle) * 72);
                int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (y > bestHeight) {
                    bestHeight = y;
                    best = new BlockPos(x, y, z);
                }
            }
            if (best == null) best = context.util().surfaceNear(context, 54, 78);
            double dx = best.getX() - origin.getX();
            double dz = best.getZ() - origin.getZ();
            double length = Math.max(1, Math.hypot(dx, dz));
            for (int step = 1; step <= 12; step++) {
                double fraction = step / 13.0;
                double x = origin.getX() + dx * fraction;
                double z = origin.getZ() + dz * fraction;
                int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
                context.level().sendParticles(ParticleTypes.END_ROD, x + 0.5, y + 1.4, z + 0.5, 3, 0.2, 0.3, 0.2, 0.005);
                if (step <= 3) {
                    BlockPos flower = new BlockPos((int) x, y, (int) z);
                    if (context.level().getBlockState(flower).canBeReplaced()) {
                        context.level().setBlockAndUpdate(flower, Blocks.DANDELION.defaultBlockState());
                    }
                }
            }
            context.state().setLead(best.getX(), best.getZ(), length, context.tick() + 20_000);
            return true;
        }
    }

    private static final class NorthFollowup extends BaseEvent {
        NorthFollowup() { super("north_followup", EventCategory.MYSTERY, 12_000, false, true, true); }

        @Override public boolean canTrigger(EventContext context) {
            return context.state().northChainStage > 0
                    && context.tick() < context.state().northChainExpiry
                    && context.player().getZ() < context.state().northOriginZ - 70;
        }

        @Override public double weight(EventContext context) { return 8.0; }

        @Override public boolean trigger(EventContext context) {
            BlockPos center = context.util().surfaceNear(context, 32, 55);
            int stage = Math.max(1, context.state().northChainStage);
            boolean success;
            if (stage == 1 && context.random().nextDouble() < 0.72) {
                success = buildCamp(context, center, "I warned you.");
            } else {
                success = context.util().placeSign(context.level(), center,
                        stage >= 2 ? "You actually came." : "Nothing happened.");
            }
            if (!success) return false;
            context.state().northChainStage++;
            if (context.state().northChainStage > 2) context.state().northChainExpiry = 0;
            context.state().setLead(center.getX(), center.getZ(), Math.sqrt(context.player().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)), context.tick() + 15_000);
            return true;
        }
    }
}
