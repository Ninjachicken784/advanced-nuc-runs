package com.ninjachicken784.advancednucruns;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the text after "/anc" into an action. Case does not matter, so "/anc Jungle Temple",
 * "/anc jt" and "/anc JT" all work. Contains no Minecraft code so it can be tested on its own.
 */
public final class CommandParser {
	public static final String USAGE =
		"Usage: /anc JT | /anc Jungle Temple | /anc WP | /anc Waypoints"
			+ " | /anc Clear all | /anc Clear waypoint <number>"
			+ " | /anc Timer start | /anc Timer reset | /anc GUI"
			+ " | /anc Run <time> | /anc Stats reset";

	private static final String TIMER_USAGE = "Usage: /anc Timer start | /anc Timer reset | /anc Timer move";
	private static final String RUN_USAGE =
		"Usage: /anc Run <time>, for example /anc Run 2 min or /anc Run 1 min 30 sec";
	private static final String STATS_USAGE = "Usage: /anc Stats reset";

	/** A longest test run, so a typo cannot overflow the time maths. */
	private static final double MAX_RUN_SECONDS = 24 * 60 * 60;

	/** One part of a time such as "2 min" or "30 sec". Longer unit names come first. */
	private static final Pattern TIME_PART = Pattern.compile(
		"\\s*(\\d+(?:\\.\\d+)?)\\s*(minutes|minute|mins|min|m|seconds|second|secs|sec|s)(?![a-z])");

	/** Full command texts offered for tab completion. */
	public static final List<String> SUGGESTIONS = List.of(
		"Jungle Temple",
		"JT",
		"Waypoints",
		"WP",
		"Clear all",
		"Clear waypoint 1",
		"Timer start",
		"Timer reset",
		"Timer move",
		"GUI",
		"Run 2 min",
		"Stats reset"
	);

	public sealed interface Action {
		record JungleTemple() implements Action { }

		record AddWaypoint() implements Action { }

		record ClearAll() implements Action { }

		record ClearOne(int id) implements Action { }

		record TimerStart() implements Action { }

		record TimerReset() implements Action { }

		record TimerMove() implements Action { }

		/** Opens the screen where the GUIs are moved and hidden. */
		record Gui() implements Action { }

		/** A simulated finished run of this length, for testing. */
		record RunTest(long nanos) implements Action { }

		/** Clears the PB and this session's average. */
		record StatsReset() implements Action { }

		record Invalid(String message) implements Action { }
	}

	private CommandParser() { }

	public static Action parse(String input) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty()) {
			return new Action.Invalid(USAGE);
		}

		String lower = trimmed.toLowerCase(Locale.ROOT);
		String[] t = lower.split("\\s+");

		if (t.length == 1 && t[0].equals("jt")) {
			return new Action.JungleTemple();
		}
		if (t.length == 2 && t[0].equals("jungle") && t[1].equals("temple")) {
			return new Action.JungleTemple();
		}
		if (t.length == 1 && isWaypointWord(t[0])) {
			return new Action.AddWaypoint();
		}
		if (t.length == 1 && t[0].equals("gui")) {
			return new Action.Gui();
		}
		if (t[0].equals("clear")) {
			return parseClear(t);
		}
		if (t[0].equals("timer")) {
			return parseTimer(t);
		}
		if (t[0].equals("run")) {
			return parseRun(lower.substring("run".length()));
		}
		if (t[0].equals("stats")) {
			return parseStats(t);
		}
		return new Action.Invalid(USAGE);
	}

	private static Action parseTimer(String[] t) {
		if (t.length == 2) {
			switch (t[1]) {
				case "start":
					return new Action.TimerStart();
				case "reset":
					return new Action.TimerReset();
				case "move":
					return new Action.TimerMove();
				default:
					break;
			}
		}
		return new Action.Invalid(TIMER_USAGE);
	}

	private static Action parseStats(String[] t) {
		if (t.length == 2 && t[1].equals("reset")) {
			return new Action.StatsReset();
		}
		return new Action.Invalid(STATS_USAGE);
	}

	/**
	 * Reads a time such as "2 min", "90 sec", "1.5 min" or "1 min 30 sec". The text is the part
	 * after the word "run", already in lower case.
	 */
	private static Action parseRun(String rest) {
		if (rest.isBlank()) {
			return new Action.Invalid(RUN_USAGE);
		}

		Matcher matcher = TIME_PART.matcher(rest);
		int position = 0;
		double seconds = 0;
		while (position < rest.length()) {
			matcher.region(position, rest.length());
			if (!matcher.lookingAt()) {
				return new Action.Invalid(RUN_USAGE);
			}
			double value = Double.parseDouble(matcher.group(1));
			seconds += matcher.group(2).startsWith("m") ? value * 60 : value;
			position = matcher.end();
		}

		if (seconds > MAX_RUN_SECONDS) {
			return new Action.Invalid("A test run can be at most 24 hours.");
		}
		long nanos = Math.round(seconds * 1_000_000_000d);
		if (nanos <= 0L) {
			return new Action.Invalid("The run time must be more than 0.");
		}
		return new Action.RunTest(nanos);
	}

	private static Action parseClear(String[] t) {
		// /anc clear all
		if (t.length == 2 && t[1].equals("all")) {
			return new Action.ClearAll();
		}
		// /anc clear 2
		if (t.length == 2) {
			return clearById(t[1]);
		}
		// /anc clear waypoint 2
		if (t.length == 3 && isWaypointWord(t[1])) {
			return clearById(t[2]);
		}
		return new Action.Invalid("Usage: /anc Clear all | /anc Clear waypoint <number>");
	}

	private static Action clearById(String text) {
		try {
			int id = Integer.parseInt(text);
			if (id >= 1) {
				return new Action.ClearOne(id);
			}
		} catch (NumberFormatException ignored) {
			// falls through to the error below
		}
		return new Action.Invalid("Waypoint number must be a whole number of 1 or more.");
	}

	/** "waypoints", "waypoint" and "wp" all mean the same thing. */
	private static boolean isWaypointWord(String word) {
		return word.equals("waypoints") || word.equals("waypoint") || word.equals("wp");
	}
}
