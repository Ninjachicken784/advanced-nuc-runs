package com.ninjachicken784.advancednucruns.client;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import com.ninjachicken784.advancednucruns.CommandParser;
import com.ninjachicken784.advancednucruns.Waypoint;

final class AncCommand {
	private static final String ARGS = "args";

	private AncCommand() { }

	/**
	 * Builds the command under the given root name. Everything after the root is read as one
	 * piece of text and parsed by {@link CommandParser}, which ignores upper and lower case.
	 */
	static LiteralArgumentBuilder<FabricClientCommandSource> build(String name) {
		return ClientCommands.literal(name)
			.executes(context -> run(context, ""))
			.then(ClientCommands.argument(ARGS, StringArgumentType.greedyString())
				.suggests((context, builder) -> {
					String typed = builder.getRemainingLowerCase();
					for (String suggestion : CommandParser.SUGGESTIONS) {
						if (suggestion.toLowerCase(Locale.ROOT).startsWith(typed)) {
							builder.suggest(suggestion);
						}
					}
					return builder.buildFuture();
				})
				.executes(context -> run(context, StringArgumentType.getString(context, ARGS))));
	}

	private static int run(CommandContext<FabricClientCommandSource> context, String input) {
		FabricClientCommandSource source = context.getSource();
		CommandParser.Action action = CommandParser.parse(input);

		if (action instanceof CommandParser.Action.JungleTemple) {
			BlockPos pos = source.getPlayer().blockPosition();
			List<Waypoint> added = AdvancedNucRunsClient.WAYPOINTS.setJungleTemple(pos.getX(), pos.getY(), pos.getZ());
			source.sendFeedback(Component.literal("Jungle Temple waypoints set:"));
			for (Waypoint w : added) {
				source.sendFeedback(Component.literal(describe(w)));
			}
		} else if (action instanceof CommandParser.Action.AddWaypoint) {
			BlockPos pos = source.getPlayer().blockPosition();
			Waypoint w = AdvancedNucRunsClient.WAYPOINTS.addManual(pos.getX(), pos.getY(), pos.getZ());
			source.sendFeedback(Component.literal("Waypoint set: " + describe(w)));
		} else if (action instanceof CommandParser.Action.ClearAll) {
			int count = AdvancedNucRunsClient.WAYPOINTS.clearAll();
			source.sendFeedback(Component.literal("Cleared " + count + (count == 1 ? " waypoint." : " waypoints.")));
		} else if (action instanceof CommandParser.Action.ClearOne clearOne) {
			if (AdvancedNucRunsClient.WAYPOINTS.clear(clearOne.id())) {
				source.sendFeedback(Component.literal("Cleared waypoint " + clearOne.id() + "."));
			} else {
				source.sendError(Component.literal("There is no waypoint " + clearOne.id() + "."));
			}
		} else if (action instanceof CommandParser.Action.TimerStart) {
			AdvancedNucRunsClient.RUN_TIMER.start(System.nanoTime());
			source.sendFeedback(Component.literal("Run timer started."));
		} else if (action instanceof CommandParser.Action.TimerReset) {
			AdvancedNucRunsClient.RUN_TIMER.reset();
			source.sendFeedback(Component.literal("Run timer reset."));
		} else if (action instanceof CommandParser.Action.TimerMove) {
			TimerHud.requestMoveScreen();
		} else if (action instanceof CommandParser.Action.Invalid invalid) {
			source.sendError(Component.literal(invalid.message()));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static String describe(Waypoint w) {
		return w.label() + " (" + w.x() + " " + w.y() + " " + w.z() + ")";
	}
}
