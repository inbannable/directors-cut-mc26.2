package dev.directorscut;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;

public final class DirectorsCut implements ModInitializer {
    public static final String MOD_ID = "directors_cut";
    private static final DirectorManager MANAGER = new DirectorManager();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(MANAGER::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(MANAGER::stop);
        ServerTickEvents.END_SERVER_TICK.register(MANAGER::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                DirectorCommands.register(dispatcher, MANAGER));

        PlayerBlockBreakEvents.AFTER.register((level, player, position, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) MANAGER.onBlockBroken(serverPlayer);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player) MANAGER.onDamage(player, damageTaken);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) MANAGER.onDeath(player);
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, entity, killed, source) -> {
            if (entity instanceof ServerPlayer player) MANAGER.onKill(player, killed);
        });
    }
}
