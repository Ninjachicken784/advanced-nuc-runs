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
import com.ninjachicken784.advancednucruns.DivanTracker;
import com.ninjachicken784.advancednucruns.Format;
import com.ninjachicken784.advancednucruns.RankTracker;
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
		} else if (action instanceof CommandParser.Action.TimerMove || action instanceof CommandParser.Action.Gui) {
			RunHud.requestEditScreen();
		} else if (action instanceof CommandParser.Action.Tracker) {
			RunHud.requestTrackerScreen();
		} else if (action instanceof CommandParser.Action.Menu) {
			RunHud.requestMenuScreen();
		} else if (action instanceof CommandParser.Action.SessionReset) {
			SessionActions.reset();
		} else if (action instanceof CommandParser.Action.Divan divan) {
			long now = AdvancedNucRunsClient.nowMillis();
			switch (divan.what()) {
				case "start" -> {
					AdvancedNucRunsClient.DIVAN.start(now);
					source.sendFeedback(Component.literal("Divan Tools timer started."));
				}
				case "pause" -> {
					AdvancedNucRunsClient.DIVAN.pause(now);
					source.sendFeedback(Component.literal("Divan Tools timer paused."));
				}
				case "reset" -> {
					AdvancedNucRunsClient.DIVAN.reset();
					source.sendFeedback(Component.literal("Divan Tools reset."));
				}
				default -> source.sendFeedback(Component.literal(divanStatus()));
			}
			TrackerStore.save();
		} else if (action instanceof CommandParser.Action.RankShow) {
			source.sendFeedback(Component.literal(rankStatus()));
		} else if (action instanceof CommandParser.Action.RankSet set) {
			AdvancedNucRunsClient.RANK.setCheck(set.rank(), set.runs(), AdvancedNucRunsClient.LAST_DROPS.runIndex());
			TrackerStore.save();
			source.sendFeedback(Component.literal("Rank set. " + rankStatus()));
		} else if (action instanceof CommandParser.Action.RankNext next) {
			AdvancedNucRunsClient.RANK.setNextRuns(next.runs());
			TrackerStore.save();
			source.sendFeedback(Component.literal("Player above you set. " + rankStatus()));
		} else if (action instanceof CommandParser.Action.RankGoal goal) {
			AdvancedNucRunsClient.RANK.setGoal(goal.place(), goal.runs());
			TrackerStore.save();
			source.sendFeedback(Component.literal("Goal set. " + rankStatus()));
		} else if (action instanceof CommandParser.Action.RankClear) {
			AdvancedNucRunsClient.RANK.clear();
			TrackerStore.save();
			source.sendFeedback(Component.literal("Rank and goal cleared."));
		} else if (action instanceof CommandParser.Action.RunTest runTest) {
			// A simulated run. It goes through the same steps as a real one, but leaves the live timer alone.
			RunResults.finished(runTest.nanos(), true);
		} else if (action instanceof CommandParser.Action.StatsReset) {
			AdvancedNucRunsClient.RUN_STATS.reset();
			GuiConfig.save();
			source.sendFeedback(Component.literal("PB and session average cleared."));
		} else if (action instanceof CommandParser.Action.Invalid invalid) {
			source.sendError(Component.literal(invalid.message()));
		}

		return Command.SINGLE_SUCCESS;
	}

	/** One chat line with the Divan Tools numbers, such as "Sets: 2, Tools: 9, Sets/h: 1.3, Tools/h: 5.8, Timer: 1H 32min". */
	static String divanStatus() {
		DivanTracker divan = AdvancedNucRunsClient.DIVAN;
		long now = AdvancedNucRunsClient.nowMillis();
		return "Divan Tools: " + divan.sets() + " sets, " + divan.totalFound() + " tools, this set "
			+ divan.currentSetSize() + "/4. Per hour: "
			+ rate(divan.setsPerHour(now)) + " sets, " + rate(divan.toolsPerHour(now)) + " tools. Timer: "
			+ Format.duration(divan.activeMillis(now)) + ".";
	}

	private static String rate(double value) {
		return Double.isNaN(value) ? "--" : String.format(Locale.ROOT, "%.1f", value);
	}

	/** One chat line with the rank, the runs and the goal, such as "#91 Name, 3046 runs. Top 10: 5754 runs left". */
	static String rankStatus() {
		RankTracker rank = AdvancedNucRunsClient.RANK;
		if (!rank.hasData()) {
			return "No rank yet. Use /anc Rank <your rank> <your runs>, for example /anc Rank 91 3046.";
		}
		long index = AdvancedNucRunsClient.LAST_DROPS.runIndex();
		StringBuilder line = new StringBuilder(rank.headline(RankHud.playerName())).append(", ").append(rank.runsLine(index));
		String next = rank.nextLine(index);
		String goal = rank.goalLine(index);
		if (next != null) {
			line.append(". ").append(next);
		}
		if (goal != null) {
			line.append(". ").append(goal);
		}
		return line.toString();
	}

	private static String describe(Waypoint w) {
		return w.label() + " (" + w.x() + " " + w.y() + " " + w.z() + ")";
	}
}
