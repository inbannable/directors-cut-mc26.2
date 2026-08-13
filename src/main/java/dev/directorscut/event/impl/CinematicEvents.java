package dev.directorscut.event.impl;

import dev.directorscut.event.EventCategory;
import dev.directorscut.event.EventContext;
import dev.directorscut.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

final class CinematicEvents {
    private CinematicEvents() {
    }

    static void register(EventRegistry registry) {
        registry.register(new StormTiming());
        registry.register(new BadFeeling());
    }

    private static final class StormTiming extends BaseEvent {
        StormTiming() { super("storm_timing", EventCategory.CINEMATIC, 48_000, false, false, true); }

        @Override public boolean canTrigger(EventContext context) {
            return context.level().dimension() == Level.OVERWORLD
                    && context.manager().server().getPlayerCount() == 1
                    && (context.state().tension > 58 || context.state().returningHome || context.state().curiosity > 72);
        }

        @Override public double weight(EventContext context) { return 0.4 + context.personality().theatricality(); }

        @Override public boolean trigger(EventContext context) {
            if (context.level().dimension() != Level.OVERWORLD) return false;
            if (context.level().isRaining()) {
                context.manager().server().setWeatherParameters(8_000, 0, false, false);
            } else {
                context.manager().server().setWeatherParameters(0, 1_800, true, true);
                context.level().playSound(null, context.player().blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 1.5f, 0.85f);
            }
            return true;
        }
    }

    private static final class BadFeeling extends BaseEvent {
        BadFeeling() { super("bad_feeling", EventCategory.AMBIENT, 18_000, false, false, false); }

        @Override public boolean canTrigger(EventContext context) {
            return context.state().tension > 38 && context.state().danger < 68;
        }

        @Override public double weight(EventContext context) { return 0.7 + context.state().tension / 90.0; }

        @Override public boolean trigger(EventContext context) {
            BlockPos source = context.player().blockPosition().offset(
                    context.random().nextInt(-12, 13), context.random().nextInt(-4, 5), context.random().nextInt(-12, 13));
            context.level().playSound(null, source.getX(), source.getY(), source.getZ(),
                    SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 1.2f, 0.65f + context.random().nextFloat() * 0.2f);
            context.level().sendParticles(ParticleTypes.ASH, source.getX(), source.getY() + 1, source.getZ(),
                    18, 2.5, 1.0, 2.5, 0.015);
            return true;
        }
    }
}
