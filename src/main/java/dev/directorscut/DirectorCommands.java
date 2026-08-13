package dev.directorscut;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DirectorCommands {
    private DirectorCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, DirectorManager manager) {
        dispatcher.register(Commands.literal("director")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    context.getSource().sendSuccess(() -> Component.literal(manager.status(player)), false);
                    return 1;
                }))
                .then(Commands.literal("events").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(manager.registry().ids()), false);
                    return manager.registry().all().size();
                }))
                .then(Commands.literal("event")
                        .then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(context, "id");
                            boolean success = manager.triggerById(player, id);
                            if (!success) context.getSource().sendFailure(Component.literal("Unknown event or no safe setup: " + id));
                            return success ? 1 : 0;
                        })))
                .then(Commands.literal("random").executes(context -> {
                    boolean success = manager.triggerRandom(context.getSource().getPlayerOrException());
                    if (!success) context.getSource().sendFailure(Component.literal("No context-appropriate event could be set up."));
                    return success ? 1 : 0;
                }))
                .then(Commands.literal("cooldown")
                        .then(Commands.literal("reset").executes(context -> {
                            var state = manager.state(context.getSource().getPlayerOrException());
                            state.cooldownUntil = 0;
                            state.lastEventTick = 0;
                            state.history.clearCooldowns();
                            context.getSource().sendSuccess(() -> Component.literal("Director cooldowns reset."), false);
                            return 1;
                        })))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> setDebug(context.getSource(), manager, true)))
                        .then(Commands.literal("off").executes(context -> setDebug(context.getSource(), manager, false)))));
    }

    private static int setDebug(CommandSourceStack source, DirectorManager manager, boolean enabled)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        manager.state(source.getPlayerOrException()).debug = enabled;
        source.sendSuccess(() -> Component.literal("Director debug " + (enabled ? "enabled" : "disabled") + "."), false);
        return 1;
    }
}
