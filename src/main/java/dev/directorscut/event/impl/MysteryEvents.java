package dev.directorscut.event.impl;

import dev.directorscut.event.EventCategory;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

final class MysteryEvents {
    private MysteryEvents() {
    }

    static void register(EventRegistry registry) {
        registry.register(new DontGoNorth());
        registry.register(new DistantExplosion());
        registry.register(new SomeoneWasHere());
        registry.register(new StrangeDoor());
        registry.register(new Observer());
        registry.register(new LastTorch());
    }

    private static boolean surface(EventContext context) {
        return context.level().canSeeSky(context.player().blockPosition().above()) && context.player().getY() > 48;
    }

    private static boolean underground(EventContext context) {
        return !context.level().canSeeSky(context.player().blockPosition().above()) || context.player().getY() < context.level().getSeaLevel() - 12;
    }

    private static final class DontGoNorth extends BaseEvent {
        DontGoNorth() { super("dont_go_north", EventCategory.MYSTERY, 72_000, true, true, true); }

        @Override public boolean canTrigger(EventContext context) {
            return surface(context) && context.state().curiosity > 38;
        }

        @Override public double weight(EventContext context) { return 0.62 + context.state().curiosity / 90.0; }

        @Override public boolean trigger(EventContext context) {
            int distance = context.random().nextInt(24, 39);
            int x = context.player().getBlockX() + context.random().nextInt(-5, 6);
            int z = context.player().getBlockZ() - distance;
            int y = context.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos sign = new BlockPos(x, y, z);
            if (!context.util().safeToModify(context, sign, 20)) return false;
            if (!context.util().placeSign(context.level(), sign, "DON'T", "GO", "NORTH.")) return false;
            context.state().northChainStage = 1;
            context.state().northOriginZ = context.player().getZ();
            context.state().northChainExpiry = context.tick() + 36_000;
            double distanceToLead = Math.sqrt(context.player().distanceToSqr(sign.getX() + 0.5, sign.getY() + 0.5, sign.getZ() + 0.5));
            context.state().setLead(sign.getX(), sign.getZ(), distanceToLead, context.tick() + 12_000);
            return true;
        }
    }

    private static final class DistantExplosion extends BaseEvent {
        DistantExplosion() { super("distant_explosion", EventCategory.MYSTERY, 24_000, false, true, true); }

        @Override public boolean canTrigger(EventContext context) { return context.state().boredom > 48; }
        @Override public double weight(EventContext context) { return 0.7 + context.state().boredom / 70.0; }

        @Override public boolean trigger(EventContext context) {
            BlockPos source = context.util().surfaceNear(context, 54, 86);
            context.util().playDistantExplosion(context, source);
            double outcome = context.random().nextDouble();
            if (outcome < 0.30 || !context.util().safeToModify(context, source, 28)) return true; // The bluff matters.
            if (outcome < 0.58) {
                context.util().makeCrater(context, source);
            } else if (outcome < 0.80) {
                context.util().placeChest(context.level(), source, context.util().stacks(
                        Items.BREAD, 3, Items.COAL, 7, Items.IRON_INGOT, 2, Items.STRING, 4));
            } else {
                context.level().setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState());
                context.util().placeSign(context.level(), source.offset(2, 0, 0), "We heard", "it too.");
            }
            context.state().setLead(source.getX(), source.getZ(), Math.sqrt(context.player().distanceToSqr(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5)), context.tick() + 16_000);
            return true;
        }
    }

    private static final class SomeoneWasHere extends BaseEvent {
        SomeoneWasHere() { super("someone_was_here", EventCategory.DISCOVERY, 36_000, false, true, true); }

        @Override public boolean canTrigger(EventContext context) {
            return underground(context) && context.state().mining(context.tick()) && context.state().boredom > 35;
        }

        @Override public boolean trigger(EventContext context) {
            Direction direction = context.util().horizontalDirection(context.player());
            BlockPos start = context.player().blockPosition().relative(direction, 5);
            if (!context.util().carveTunnel(context, start, direction, context.random().nextInt(8, 15), "wrong way")) return false;
            context.level().setBlockAndUpdate(start.relative(direction, 2), Blocks.FURNACE.defaultBlockState());
            context.level().setBlockAndUpdate(start.relative(direction, 4), Blocks.CRAFTING_TABLE.defaultBlockState());
            return true;
        }
    }

    private static final class StrangeDoor extends BaseEvent {
        StrangeDoor() { super("the_door", EventCategory.MYSTERY, 48_000, true, true, false); }

        @Override public boolean canTrigger(EventContext context) { return surface(context) && context.state().curiosity > 28; }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.util().surfaceNear(context, 28, 52);
            if (!context.util().safeToModify(context, position, 18) || !context.level().getBlockState(position).canBeReplaced()
                    || !context.level().getBlockState(position.above()).canBeReplaced()) return false;
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.level().getRandom());
            var lower = Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            var upper = lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
            context.level().setBlockAndUpdate(position, lower);
            context.level().setBlockAndUpdate(position.above(), upper);
            context.state().setLead(position.getX(), position.getZ(), Math.sqrt(context.player().distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5)), context.tick() + 12_000);
            return true;
        }
    }

    private static final class Observer extends BaseEvent {
        Observer() { super("the_observer", EventCategory.HORROR, 144_000, true, false, true); }

        @Override public double weight(EventContext context) { return 0.08 + context.state().mystery / 500.0; }

        @Override public boolean trigger(EventContext context) {
            BlockPos position = context.util().surfaceNear(context, 44, 68);
            var stand = context.util().spawn(context, EntityTypes.ARMOR_STAND, position);
            if (stand == null) return false;
            stand.setNoGravity(true);
            stand.setCustomName(Component.literal(" "));
            context.manager().schedule(context.tick() + 120, () -> {
                if (stand.isAlive()) {
                    context.level().sendParticles(ParticleTypes.SMOKE, stand.getX(), stand.getY() + 1, stand.getZ(), 8, 0.1, 0.5, 0.1, 0.01);
                    stand.discard();
                }
            });
            return true;
        }
    }

    private static final class LastTorch extends BaseEvent {
        LastTorch() { super("last_torch", EventCategory.MERCY, 36_000, false, true, false); }

        @Override public boolean canTrigger(EventContext context) {
            return underground(context) && context.manager().countItem(context.player(), Items.TORCH) == 0;
        }

        @Override public boolean trigger(EventContext context) {
            Direction direction = context.util().horizontalDirection(context.player());
            BlockPos cursor = context.player().blockPosition();
            int placed = 0;
            for (int step = 5; step <= 20; step += 5) {
                BlockPos target = cursor.relative(direction, step);
                for (int y = 2; y >= -2; y--) {
                    BlockPos candidate = target.offset(0, y, 0);
                    if (context.level().getBlockState(candidate).isAir() && context.level().getBlockState(candidate.below()).isSolid()) {
                        context.level().setBlockAndUpdate(candidate, Blocks.TORCH.defaultBlockState());
                        placed++;
                        break;
                    }
                }
            }
            if (placed == 0) return false;
            if (context.random().nextDouble() < 0.35) {
                BlockPos end = cursor.relative(direction, 22);
                context.util().placeSign(context.level(), end, "sorry");
            }
            return true;
        }
    }
}
