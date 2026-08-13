package dev.directorscut.event;

import dev.directorscut.DirectorManager;
import dev.directorscut.state.DirectorPersonality;
import dev.directorscut.state.DirectorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.random.RandomGenerator;

public record EventContext(
        DirectorManager manager,
        ServerPlayer player,
        DirectorState state,
        DirectorPersonality personality,
        long tick,
        RandomGenerator random,
        boolean forced
) {
    public ServerLevel level() {
        return player.level();
    }

    public EventUtil util() {
        return manager.eventUtil();
    }
}
