package com.craisinlord.anthology.gravity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class GravityCommand {
    private static final GravityTransition COMMAND_TRANSITION = new GravityTransition(12);

    private GravityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("gravity")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(context -> getGravity(context, EntityArgument.getEntities(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.literal("down")
                                                .executes(context -> setGravity(context, EntityArgument.getEntities(context, "targets"), GravityDirection.DOWN)))
                                        .then(Commands.literal("up")
                                                .executes(context -> setGravity(context, EntityArgument.getEntities(context, "targets"), GravityDirection.UP)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(context -> clearGravity(context, EntityArgument.getEntities(context, "targets")))))
        );
    }

    private static int getGravity(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets) {
        for (Entity target : targets) {
            GravityDirection direction = GravityApi.getGravityDirection(target);
            context.getSource().sendSuccess(
                    () -> Component.literal(target.getName().getString() + ": " + direction.getSerializedName()),
                    false
            );
        }

        return targets.size();
    }

    private static int setGravity(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets, GravityDirection direction) {
        for (Entity target : targets) {
            GravityApi.setGravityDirection(target, direction, COMMAND_TRANSITION);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set gravity " + direction.getSerializedName() + " for " + targets.size() + " target(s)"),
                true
        );
        return targets.size();
    }

    private static int clearGravity(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets) {
        for (Entity target : targets) {
            GravityApi.setGravityDirection(target, GravityDirection.DOWN, COMMAND_TRANSITION);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Reset gravity for " + targets.size() + " target(s)"),
                true
        );
        return targets.size();
    }
}
