package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Run goals such as 10000 runs. You type your runs once (/anc Goal 10000 3300); after that the mod
 * adds every run you finish, so it can say how many runs are left to each goal and, with your
 * average run time, how long that takes. Contains no Minecraft code, so it can be tested on its own.
 */
public final class GoalTracker {
	private final TreeSet<Long> goals = new TreeSet<>();
	private long checkRuns;
	private long checkIndex;
	private boolean runsKnown;

	/** Adds a goal and sets your runs as of now. Runs that finish after this are added to it. */
	public synchronized void set(long target, long runs, long runIndexNow) {
		goals.add(Math.max(1L, target));
		checkRuns = Math.max(0L, runs);
		checkIndex = runIndexNow;
		runsKnown = true;
	}

	/** Adds a goal using the runs already known. Returns false if no runs were typed in yet. */
	public synchronized boolean add(long target) {
		if (!runsKnown) {
			return false;
		}
		goals.add(Math.max(1L, target));
		return true;
	}

	/** Removes one goal. Returns false if there was no such goal. */
	public synchronized boolean remove(long target) {
		return goals.remove(target);
	}

	public synchronized void clear() {
		goals.clear();
		checkRuns = 0L;
		checkIndex = 0L;
		runsKnown = false;
	}

	public synchronized List<Long> goals() {
		return new ArrayList<>(goals);
	}

	public synchronized boolean hasData() {
		return !goals.isEmpty();
	}

	/** Your runs now: the runs you entered plus every run finished since. */
	public synchronized long runs(long runIndexNow) {
		return checkRuns + Math.max(0L, runIndexNow - checkIndex);
	}

	/** Runs left to the goal, 0 if it is reached. */
	public synchronized long remaining(long target, long runIndexNow) {
		return Math.max(0L, target - runs(runIndexNow));
	}

	/** 10000 becomes "10k", 1500 "1.5k", 2000000 "2m". Smaller numbers stay as they are. */
	public static String shortNumber(long value) {
		if (value >= 1_000_000L && value % 100_000L == 0L) {
			return trim(value / 1_000_000d) + "m";
		}
		if (value >= 1_000L && value % 100L == 0L) {
			return trim(value / 1_000d) + "k";
		}
		return Long.toString(value);
	}

	private static String trim(double value) {
		String text = String.format(Locale.ROOT, "%.1f", value);
		return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
	}

	/** The time for these runs at this average, such as "223h 30min", or "--" if there is no average yet. */
	public static String timeLeft(long runs, long averageNanos) {
		if (averageNanos <= 0L) {
			return "--";
		}
		long minutes = Math.round(runs * (averageNanos / 1_000_000_000d) / 60d);
		long hours = minutes / 60L;
		return hours > 0L ? hours + "h " + (minutes % 60L) + "min" : minutes + "min";
	}

	public synchronized void save(Properties out) {
		out.setProperty("goal.runs", Long.toString(checkRuns));
		out.setProperty("goal.index", Long.toString(checkIndex));
		out.setProperty("goal.known", Boolean.toString(runsKnown));
		StringBuilder list = new StringBuilder();
		for (long goal : goals) {
			list.append(list.length() == 0 ? "" : ",").append(goal);
		}
		out.setProperty("goal.list", list.toString());
	}

	public synchronized void load(Properties in) {
		goals.clear();
		checkRuns = parse(in.getProperty("goal.runs"));
		checkIndex = parse(in.getProperty("goal.index"));
		runsKnown = Boolean.parseBoolean(in.getProperty("goal.known", "false"));
		String list = in.getProperty("goal.list", "");
		for (String part : list.split(",")) {
			long value = parse(part);
			if (value > 0L) {
				goals.add(value);
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
