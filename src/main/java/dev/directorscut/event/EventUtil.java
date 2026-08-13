package dev.directorscut.event;

import dev.directorscut.DirectorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class EventUtil {
    private final DirectorManager manager;

    public EventUtil(DirectorManager manager) {
        this.manager = manager;
    }

    public BlockPos surfaceNear(EventContext context, int minimumDistance, int maximumDistance) {
        RandomGenerator random = context.random();
        double angle = random.nextDouble(Math.PI * 2.0);
        int distance = random.nextInt(minimumDistance, maximumDistance + 1);
        int x = context.player().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = context.player().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    public boolean safeToModify(EventContext context, BlockPos center, int radius) {
        ServerLevel level = context.level();
        if (!level.getWorldBorder().isWithinBounds(center)) return false;
        for (ServerPlayer other : level.players()) {
            if (other != context.player() && other.blockPosition().distSqr(center) < radius * radius) return false;
        }
        return true;
    }

    public boolean placeSign(ServerLevel level, BlockPos position, String... lines) {
        if (!level.getBlockState(position).canBeReplaced() || level.getBlockState(position.below()).isAir()) return false;
        if (!level.setBlockAndUpdate(position, Blocks.OAK_SIGN.defaultBlockState())) return false;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof SignBlockEntity sign) {
            var text = sign.getFrontText();
            for (int index = 0; index < Math.min(4, lines.length); index++) {
                text = text.setMessage(index, Component.literal(lines[index]));
            }
            sign.setText(text, true);
            sign.setChanged();
        }
        return true;
    }

    public boolean placeChest(ServerLevel level, BlockPos position, List<ItemStack> contents) {
        if (!level.getBlockState(position).canBeReplaced()) return false;
        if (!level.setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState())) return false;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < contents.size() && slot < container.getContainerSize(); slot++) {
                container.setItem(slot, contents.get(slot));
            }
            blockEntity.setChanged();
        }
        return true;
    }

    public List<ItemStack> stacks(Object... itemCounts) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index + 1 < itemCounts.length; index += 2) {
            stacks.add(new ItemStack((Item) itemCounts[index], (Integer) itemCounts[index + 1]));
        }
        return stacks;
    }

    public ItemStack named(Item item, int count, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    public <T extends Entity> T spawn(EventContext context, EntityType<T> type, BlockPos position) {
        T entity = type.create(context.level(), EntitySpawnReason.EVENT);
        if (entity == null) return null;
        entity.snapTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        if (!context.level().addFreshEntity(entity)) return null;
        return entity;
    }

    public BlockPos undergroundNear(EventContext context, int minimumDistance, int maximumDistance) {
        RandomGenerator random = context.random();
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        Direction direction = directions[random.nextInt(directions.length)];
        int distance = random.nextInt(minimumDistance, maximumDistance + 1);
        return context.player().blockPosition().relative(direction, distance);
    }

    public Direction horizontalDirection(ServerPlayer player) {
        Direction direction = player.getDirection();
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    public boolean carveTunnel(EventContext context, BlockPos start, Direction direction, int length, String... ending) {
        ServerLevel level = context.level();
        if (!safeToModify(context, start.relative(direction, length / 2), Math.max(18, length / 2))) return false;
        for (int distance = 0; distance < length; distance++) {
            BlockPos floor = start.relative(direction, distance).below();
            for (int height = 0; height < 2; height++) {
                BlockPos target = floor.above(height + 1);
                if (level.getBlockEntity(target) != null || level.getBlockState(target).getDestroySpeed(level, target) < 0) return false;
            }
        }
        for (int distance = 0; distance < length; distance++) {
            BlockPos floor = start.relative(direction, distance).below();
            level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
            if (distance % 9 == 2 && level.getBlockState(floor).isSolid()) {
                level.setBlockAndUpdate(floor.above(), Blocks.TORCH.defaultBlockState());
            }
        }
        BlockPos end = start.relative(direction, length - 1);
        placeSign(level, end, ending);
        return true;
    }

    public void playDistantExplosion(EventContext context, BlockPos source) {
        context.level().playSound(
                null, source.getX(), source.getY(), source.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 4.0f, 0.78f
        );
        context.level().sendParticles(ParticleTypes.LARGE_SMOKE, source.getX() + 0.5, source.getY() + 1, source.getZ() + 0.5,
                24, 1.2, 0.7, 1.2, 0.04);
    }

    public void makeCrater(EventContext context, BlockPos center) {
        ServerLevel level = context.level();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double distance = Math.hypot(x, z);
                if (distance > 3.3) continue;
                int depth = distance < 1.6 ? 2 : 1;
                for (int y = 0; y < depth; y++) {
                    BlockPos target = center.offset(x, -y - 1, z);
                    if (level.getBlockEntity(target) == null && level.getBlockState(target).getDestroySpeed(level, target) >= 0) {
                        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    public void registerFollower(Chicken chicken, ServerPlayer owner, long untilTick) {
        manager.registerFollower(chicken, owner, untilTick);
    }

    public void freezeThenRelease(Mob mob, long releaseTick) {
        mob.setNoAi(true);
        manager.schedule(releaseTick, () -> {
            if (mob.isAlive()) mob.setNoAi(false);
        });
    }
}
