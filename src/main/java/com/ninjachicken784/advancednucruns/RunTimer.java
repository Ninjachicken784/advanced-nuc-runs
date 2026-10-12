package com.ninjachicken784.advancednucruns;

import java.util.Locale;

/**
 * Times runs back to back: each run is measured from the previous finish to the next finish.
 * Contains no Minecraft code, and the current time is passed in, so it can be tested on its own.
 */
public final class RunTimer {
	/** The loot bundle line Hypixel prints when a Nucleus run is complete (compared in lower case). */
	private static final String FINISH_TEXT = "crystal nucleus loot bundle";

	public enum State {
		/** The clock is not running: nothing has finished yet, or the timer was reset. */
		IDLE,
		/** The clock is running and counting the current run. */
		RUNNING
	}

	private State state = State.IDLE;
	private long startNanos;

	/** Starts the clock now. Whatever was being counted is discarded. */
	public synchronized void start(long nowNanos) {
		state = State.RUNNING;
		startNanos = nowNanos;
	}

	/**
	 * Ends the current run and starts the next one at the same moment, so no time is lost between
	 * runs. Returns the time of the run that just ended in nanoseconds. If the clock was not
	 * running yet (the very first finish), it starts the clock and returns -1.
	 */
	public synchronized long finishRun(long nowNanos) {
		if (state != State.RUNNING) {
			start(nowNanos);
			return -1L;
		}
		long runNanos = Math.max(0L, nowNanos - startNanos);
		startNanos = nowNanos;
		return runNanos;
	}

	/** Stops the clock and clears it. The next finish starts it again. */
	public synchronized void reset() {
		state = State.IDLE;
		startNanos = 0L;
	}

	public synchronized State state() {
		return state;
	}

	/** Time of the current run so far, or zero when the clock is not running. */
	public synchronized long elapsedNanos(long nowNanos) {
		return state == State.RUNNING ? Math.max(0L, nowNanos - startNanos) : 0L;
	}

	/** Formats a time as minutes:seconds.hundredths, for example 3:42.18. Used in chat. */
	public static String format(long nanos) {
		long hundredths = Math.max(0L, nanos) / 10_000_000L;
		long minutes = hundredths / 6000L;
		long seconds = (hundredths / 100L) % 60L;
		long rest = hundredths % 100L;
		return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, rest);
	}

	/**
	 * Formats a time as minutes and seconds only, for example "1min 22sec", or "45sec" when it is
	 * under a minute. Partial seconds are dropped. Used on the on-screen GUIs.
	 */
	public static String formatShort(long nanos) {
		long totalSeconds = Math.max(0L, nanos) / 1_000_000_000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return minutes > 0L ? minutes + "min " + seconds + "sec" : seconds + "sec";
	}

	/** True if this chat line is the Crystal Nucleus loot bundle header. */
	public static boolean isFinishMessage(String chatLine) {
		return chatLine != null && chatLine.toLowerCase(Locale.ROOT).contains(FINISH_TEXT);
	}
}
