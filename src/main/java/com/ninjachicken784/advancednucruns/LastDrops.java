package com.ninjachicken784.advancednucruns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Remembers when the rarest drops last dropped, counted in runs, so the chat can say "Last Alloy:
 * 552 runs ago". It has its own run counter, so editing the run count in the tracker screen does
 * not change it. Contains no Minecraft code, so it can be tested on its own.
 */
public final class LastDrops {
	/** A drop whose last time is reported: the save-file key of the item and the short name used in chat. */
	public record Tracked(String key, String shortName) { }

	public static final List<Tracked> TRACKED = List.of(
		new Tracked("DIVANS_ALLOY", "Alloy"),
		new Tracked("QUICK_CLAW", "Quick Claw"),
		new Tracked("JADE_DYE", "Jade Dye"));

	private long runIndex;
	private final Map<String, Long> lastRun = new HashMap<>();

	/** The short chat name of a tracked drop, or null if the drop is not tracked. */
	public static String shortName(String key) {
		for (Tracked tracked : TRACKED) {
			if (tracked.key().equals(key)) {
				return tracked.shortName();
			}
		}
		return null;
	}

	/** A run finished. */
	public synchronized void runFinished() {
		runIndex++;
	}

	/**
	 * A tracked drop dropped in the current run. Returns how many runs ago the one before it
	 * dropped (a drop in the run right before counts as 1), or -1 if no earlier drop is known.
	 */
	public synchronized long dropped(String key) {
		Long previous = lastRun.put(key, runIndex);
		return previous == null ? -1L : runIndex - previous;
	}

	/** Runs since this drop last dropped, or -1 if it never did while the mod was counting. */
	public synchronized long runsSince(String key) {
		Long previous = lastRun.get(key);
		return previous == null ? -1L : runIndex - previous;
	}

	/** The chat line for a drop, such as "Last Alloy: 552 runs ago". */
	public static String message(String shortName, long runsAgo) {
		if (runsAgo < 0L) {
			return shortName + ": first one tracked";
		}
		return "Last " + shortName + ": " + runsAgo + (runsAgo == 1L ? " run ago" : " runs ago");
	}

	public synchronized void save(Properties out) {
		out.setProperty("last.runIndex", Long.toString(runIndex));
		for (Map.Entry<String, Long> entry : lastRun.entrySet()) {
			out.setProperty("last.drop." + entry.getKey(), Long.toString(entry.getValue()));
		}
	}

	public synchronized void load(Properties in) {
		runIndex = 0L;
		lastRun.clear();
		runIndex = parse(in.getProperty("last.runIndex"));
		for (String name : in.stringPropertyNames()) {
			if (name.startsWith("last.drop.")) {
				long value = parse(in.getProperty(name));
				if (value >= 0L && value <= runIndex) {
					lastRun.put(name.substring("last.drop.".length()), value);
				}
			}
		}
	}

	private static long parse(String text) {
		try {
			return text == null ? 0L : Math.max(0L, Long.parseLong(text.trim()));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
