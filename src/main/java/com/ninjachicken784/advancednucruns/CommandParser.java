package com.ninjachicken784.advancednucruns;

import java.util.List;
import java.util.Locale;

/**
 * Turns the text after "/anc" into an action. Case does not matter, so "/anc Jungle Temple",
 * "/anc jt" and "/anc JT" all work. Contains no Minecraft code so it can be tested on its own.
 */
public final class CommandParser {
	public static final String USAGE =
		"Usage: /anc JT | /anc Jungle Temple | /anc WP | /anc Waypoints"
			+ " | /anc Clear all | /anc Clear waypoint <number>"
			+ " | /anc Timer start | /anc Timer reset | /anc Timer move";

	private static final String TIMER_USAGE = "Usage: /anc Timer start | /anc Timer reset | /anc Timer move";

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
		"Timer move"
	);

	public sealed interface Action {
		record JungleTemple() implements Action { }

		record AddWaypoint() implements Action { }

		record ClearAll() implements Action { }

		record ClearOne(int id) implements Action { }

		record TimerStart() implements Action { }

		record TimerReset() implements Action { }

		record TimerMove() implements Action { }

		record Invalid(String message) implements Action { }
	}

	private CommandParser() { }

	public static Action parse(String input) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty()) {
			return new Action.Invalid(USAGE);
		}

		String[] t = trimmed.toLowerCase(Locale.ROOT).split("\\s+");

		if (t.length == 1 && t[0].equals("jt")) {
			return new Action.JungleTemple();
		}
		if (t.length == 2 && t[0].equals("jungle") && t[1].equals("temple")) {
			return new Action.JungleTemple();
		}
		if (t.length == 1 && isWaypointWord(t[0])) {
			return new Action.AddWaypoint();
		}
		if (t[0].equals("clear")) {
			return parseClear(t);
		}
		if (t[0].equals("timer")) {
			return parseTimer(t);
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
