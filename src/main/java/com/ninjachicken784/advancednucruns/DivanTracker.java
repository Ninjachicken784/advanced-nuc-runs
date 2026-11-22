package com.ninjachicken784.advancednucruns;

import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Counts the Scavenged tools found with the Metal Detector in the Mines of Divan, the sets of four
 * (Lapis Sword, Golden Hammer, Diamond Axe, Emerald Hammer) and how many of each come per hour.
 *
 * <p>A tool can only be found once per set, so a set is complete when all four different tools
 * were found; the next find then starts a new set. The timer starts when the first chest is opened (or at the first find, or with
 * {@link #start}), can be paused, and stops by itself after {@link #IDLE_MILLIS} without a find,
 * so a break does not ruin the hourly rate. Contains no Minecraft code, so it can be tested on its own.
 */
public final class DivanTracker {
	/** The four tools, in the order they are shown. */
	public enum Tool {
		LAPIS("Lapis Sword"),
		GOLD("Golden Hammer"),
		DIAMOND("Diamond Axe"),
		EMERALD("Emerald Hammer");

		private final String label;

		Tool(String label) {
			this.label = label;
		}

		/** The name without "Scavenged", such as "Lapis Sword". */
		public String label() {
			return label;
		}

		/** The full item name, such as "Scavenged Lapis Sword". */
		public String itemName() {
			return "Scavenged " + label;
		}
	}

	/** What a find did: which tool, how far the current set is, and whether it finished a set. */
	public record Found(Tool tool, int inSet, boolean setCompleted, long sets) { }

	/** No find for this long stops the timer, until the next find or Start. */
	public static final long IDLE_MILLIS = 15 * 60_000L;
	/** The same line twice within this time is one find. */
	private static final long DUPLICATE_MILLIS = 3_000L;
	/** Below this much timer time no rate is shown, since one find would make it huge. */
	private static final long MIN_RATE_MILLIS = 60_000L;

	/** Chat from players: "[MVP+] Name: text", also with a level in front. */
	private static final Pattern PLAYER_CHAT = Pattern.compile("^(\\[[^\\]]+\\]\\s*)*\\w{1,16}:\\s.*");
	/** Words that mean the line is not a find: NPC talk, handing a tool in, or "you already have it". */
	private static final String[] NOT_A_FIND = {"[npc]", "keeper", " gave ", " give ", "already", "party >", "guild >",
		"co-op >"};

	private final long[] found = new long[Tool.values().length];
	private final boolean[] inSet = new boolean[Tool.values().length];
	private final long[] lastCounted = new long[Tool.values().length];
	private long sets;
	private long chests;

	private long accumulated;
	private long segmentStart = -1L;
	private long lastActivity;
	private boolean paused;

	public DivanTracker() {
		java.util.Arrays.fill(lastCounted, Long.MIN_VALUE / 2);
	}

	/**
	 * Which tool a chat line says was found, or null. A line counts if it names one of the four
	 * tools and is not chat from a player, an NPC, or a hand-in.
	 */
	public static Tool match(String message) {
		if (message == null) {
			return null;
		}
		String text = message.strip();
		if (PLAYER_CHAT.matcher(text).matches()) {
			return null;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		for (String word : NOT_A_FIND) {
			if (lower.contains(word)) {
				return null;
			}
		}
		for (Tool tool : Tool.values()) {
			if (lower.contains(tool.itemName().toLowerCase(Locale.ROOT))) {
				return tool;
			}
		}
		return null;
	}

	/** Reads one chat line. Returns what it did, or null if the line is not a find. */
	public synchronized Found onMessage(String message, long now) {
		Tool tool = match(message);
		if (tool == null) {
			return null;
		}
		int index = tool.ordinal();
		if (now - lastCounted[index] < DUPLICATE_MILLIS) {
			return null;
		}
		lastCounted[index] = now;

		found[index]++;
		inSet[index] = true;
		touch(now);

		int count = 0;
		for (boolean held : inSet) {
			if (held) {
				count++;
			}
		}
		boolean completed = count == inSet.length;
		if (completed) {
			sets++;
			java.util.Arrays.fill(inSet, false);
			count = 0;
		}
		return new Found(tool, count, completed, sets);
	}

	// ---- numbers

	/** How many of this tool were found. */
	public synchronized long found(Tool tool) {
		return found[tool.ordinal()];
	}

	/** All tools found. */
	public synchronized long totalFound() {
		long total = 0L;
		for (long value : found) {
			total += value;
		}
		return total;
	}

	/** Chests opened. */
	public synchronized long chests() {
		return chests;
	}

	/** Chests opened per hour, or NaN while less than a minute is counted. */
	public synchronized double chestsPerHour(long now) {
		return perHour(chests, now);
	}

	/** Finished sets of four. */
	public synchronized long sets() {
		return sets;
	}

	/** True if this tool is already in the set that is being collected. */
	public synchronized boolean inCurrentSet(Tool tool) {
		return inSet[tool.ordinal()];
	}

	/** How many different tools the set that is being collected has, 0 to 3. */
	public synchronized int currentSetSize() {
		int count = 0;
		for (boolean held : inSet) {
			if (held) {
				count++;
			}
		}
		return count;
	}

	/** True if nothing was found and no time was counted. */
	public synchronized boolean isEmpty() {
		return totalFound() == 0L && chests == 0L && accumulated == 0L && segmentStart < 0L;
	}

	/** Tools per hour, or NaN while less than a minute is counted. */
	public synchronized double toolsPerHour(long now) {
		return perHour(totalFound(), now);
	}

	/** Finished sets per hour, or NaN while less than a minute is counted. */
	public synchronized double setsPerHour(long now) {
		return perHour(sets, now);
	}

	private double perHour(long count, long now) {
		long millis = activeMillis(now);
		return millis < MIN_RATE_MILLIS ? Double.NaN : count * 3_600_000d / millis;
	}

	// ---- timer

	private long segmentEnd(long now) {
		long mark = Math.max(segmentStart, lastActivity);
		return Math.min(now, mark + IDLE_MILLIS);
	}

	/** The time the timer has counted. */
	public synchronized long activeMillis(long now) {
		return accumulated + (segmentStart >= 0L ? Math.max(0L, segmentEnd(now) - segmentStart) : 0L);
	}

	/** True while the timer is counting. */
	public synchronized boolean running(long now) {
		return segmentStart >= 0L && now < Math.max(segmentStart, lastActivity) + IDLE_MILLIS;
	}

	/** Text for the button: Start before anything was counted, Pause while running, otherwise Resume. */
	public synchronized String buttonText(long now) {
		if (running(now)) {
			return "Pause";
		}
		return accumulated == 0L && segmentStart < 0L && !paused ? "Start" : "Resume";
	}

	/** Starts or resumes the timer. */
	public synchronized void start(long now) {
		if (running(now)) {
			return;
		}
		if (segmentStart >= 0L) {
			accumulated += Math.max(0L, segmentEnd(now) - segmentStart);
		}
		paused = false;
		segmentStart = now;
		lastActivity = now;
	}

	/** Stops the timer until Start. Finds are still counted while it is paused. */
	public synchronized void pause(long now) {
		if (segmentStart >= 0L) {
			accumulated += Math.max(0L, segmentEnd(now) - segmentStart);
		}
		segmentStart = -1L;
		paused = true;
	}

	/** The Pause / Resume button. */
	public synchronized void toggle(long now) {
		if (running(now)) {
			pause(now);
		} else {
			start(now);
		}
	}

	/** A Metal Detector chest was opened: starts the timer if it was never started, or keeps it going. */
	public synchronized void chestOpened(long now) {
		chests++;
		touch(now);
	}

	/** A find: keeps the timer going, and starts it if it was never started. */
	private void touch(long now) {
		if (paused) {
			return;
		}
		if (segmentStart >= 0L && now >= Math.max(segmentStart, lastActivity) + IDLE_MILLIS) {
			// The timer stopped by itself while nothing was found: the break is not counted.
			accumulated += segmentEnd(now) - segmentStart;
			segmentStart = now;
		} else if (segmentStart < 0L) {
			segmentStart = now;
		}
		lastActivity = now;
	}

	/** Back to zero. */
	public synchronized void reset() {
		java.util.Arrays.fill(found, 0L);
		java.util.Arrays.fill(inSet, false);
		java.util.Arrays.fill(lastCounted, Long.MIN_VALUE / 2);
		sets = 0L;
		chests = 0L;
		accumulated = 0L;
		segmentStart = -1L;
		lastActivity = 0L;
		paused = false;
	}

	// ---- saving

	public synchronized void save(Properties out, long now) {
		for (Tool tool : Tool.values()) {
			out.setProperty("divan.found." + tool.name(), Long.toString(found[tool.ordinal()]));
			out.setProperty("divan.inSet." + tool.name(), Boolean.toString(inSet[tool.ordinal()]));
		}
		out.setProperty("divan.sets", Long.toString(sets));
		out.setProperty("divan.chests", Long.toString(chests));
		out.setProperty("divan.activeMillis", Long.toString(activeMillis(now)));
		out.setProperty("divan.paused", Boolean.toString(paused));
	}

	public synchronized void load(Properties in) {
		reset();
		for (Tool tool : Tool.values()) {
			found[tool.ordinal()] = parse(in.getProperty("divan.found." + tool.name()));
			inSet[tool.ordinal()] = Boolean.parseBoolean(in.getProperty("divan.inSet." + tool.name(), "false"));
		}
		sets = parse(in.getProperty("divan.sets"));
		chests = parse(in.getProperty("divan.chests"));
		accumulated = parse(in.getProperty("divan.activeMillis"));
		// The timer is stopped after a restart: it starts again with the next find or with Start.
		paused = Boolean.parseBoolean(in.getProperty("divan.paused", "false"));
	}

	private static long parse(String text) {
		try {
			return text == null ? 0L : Math.max(0L, Long.parseLong(text.trim()));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
