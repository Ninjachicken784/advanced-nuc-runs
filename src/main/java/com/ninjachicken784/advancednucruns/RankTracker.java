package com.ninjachicken784.advancednucruns;

import java.util.Properties;

/**
 * Your place on the Crystal Nucleus runs leaderboard, and the runs you still need to reach the
 * next place or a goal place. The leaderboard itself (for example the SkyHelper bot on Discord)
 * cannot be read by the mod, so you enter your rank, your runs, and the runs of the player you want
 * to beat. After that the mod counts your runs itself: every finished run adds one. Contains no
 * Minecraft code, so it can be tested on its own.
 */
public final class RankTracker {
	private long rank;
	private long checkRuns;
	private long checkIndex;
	private long nextRuns;
	private long goalRank;
	private long goalRuns;

	/** Sets your rank and your runs, as read from the leaderboard just now. */
	public synchronized void setCheck(long newRank, long runs, long runIndexNow) {
		rank = Math.max(0L, newRank);
		checkRuns = Math.max(0L, runs);
		checkIndex = runIndexNow;
	}

	public synchronized void setRank(long newRank) {
		rank = Math.max(0L, newRank);
	}

	/** Sets your runs as of now. Runs that finish after this are added to it. */
	public synchronized void setRuns(long runs, long runIndexNow) {
		checkRuns = Math.max(0L, runs);
		checkIndex = runIndexNow;
	}

	/** The runs of the player one place above you, so the mod can say how far away he is. 0 clears it. */
	public synchronized void setNextRuns(long runs) {
		nextRuns = Math.max(0L, runs);
	}

	/** The place you want to reach, and the runs of the player who holds it. */
	public synchronized void setGoal(long place, long runs) {
		goalRank = Math.max(0L, place);
		goalRuns = Math.max(0L, runs);
	}

	public synchronized void setGoalRank(long place) {
		goalRank = Math.max(0L, place);
	}

	public synchronized void setGoalRuns(long runs) {
		goalRuns = Math.max(0L, runs);
	}

	public synchronized void clear() {
		rank = 0L;
		checkRuns = 0L;
		checkIndex = 0L;
		nextRuns = 0L;
		goalRank = 0L;
		goalRuns = 0L;
	}

	public synchronized long rank() {
		return rank;
	}

	public synchronized long nextRuns() {
		return nextRuns;
	}

	public synchronized long goalRank() {
		return goalRank;
	}

	public synchronized long goalRuns() {
		return goalRuns;
	}

	/** Your runs now: the runs you entered plus every run finished since. */
	public synchronized long runs(long runIndexNow) {
		return checkRuns + Math.max(0L, runIndexNow - checkIndex);
	}

	/** True once there is anything to show. */
	public synchronized boolean hasData() {
		return rank > 0L || checkRuns > 0L;
	}

	/** "#91 Ninjachicken784". Without a rank it is only the name. */
	public synchronized String headline(String name) {
		String who = name == null || name.isBlank() ? "You" : name;
		return rank > 0L ? "#" + rank + " " + who : who;
	}

	public synchronized String runsLine(long runIndexNow) {
		return runs(runIndexNow) + " runs";
	}

	/** "Next #90: 1 run left", or null if the runs of the player above are not known. */
	public synchronized String nextLine(long runIndexNow) {
		if (nextRuns <= 0L) {
			return null;
		}
		String label = rank > 1L ? "Next #" + (rank - 1L) : "Next";
		return label + ": " + left(nextRuns - runs(runIndexNow));
	}

	/** "Top 10: 5754 runs left", or null if there is no goal. */
	public synchronized String goalLine(long runIndexNow) {
		if (goalRuns <= 0L) {
			return null;
		}
		String label = goalRank > 0L ? "Top " + goalRank : "Goal";
		return label + ": " + left(goalRuns - runs(runIndexNow));
	}

	private static String left(long remaining) {
		if (remaining <= 0L) {
			return "reached";
		}
		return remaining + (remaining == 1L ? " run left" : " runs left");
	}

	public synchronized void save(Properties out) {
		out.setProperty("rank.rank", Long.toString(rank));
		out.setProperty("rank.runs", Long.toString(checkRuns));
		out.setProperty("rank.index", Long.toString(checkIndex));
		out.setProperty("rank.next", Long.toString(nextRuns));
		out.setProperty("rank.goalRank", Long.toString(goalRank));
		out.setProperty("rank.goalRuns", Long.toString(goalRuns));
	}

	public synchronized void load(Properties in) {
		rank = parse(in.getProperty("rank.rank"));
		checkRuns = parse(in.getProperty("rank.runs"));
		checkIndex = parse(in.getProperty("rank.index"));
		nextRuns = parse(in.getProperty("rank.next"));
		goalRank = parse(in.getProperty("rank.goalRank"));
		goalRuns = parse(in.getProperty("rank.goalRuns"));
	}

	private static long parse(String text) {
		try {
			return text == null ? 0L : Math.max(0L, Long.parseLong(text.trim()));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
