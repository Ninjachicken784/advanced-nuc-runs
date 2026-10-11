package com.ninjachicken784.advancednucruns;

import java.util.Locale;

/**
 * Times one run from start to finish. Contains no Minecraft code, and the current time is passed
 * in, so it can be tested on its own.
 */
public final class RunTimer {
	/** The loot bundle line Hypixel prints when a Nucleus run is complete (compared in lower case). */
	private static final String FINISH_TEXT = "crystal nucleus loot bundle";

	public enum State {
		/** No run has been timed yet, or the timer was reset. */
		IDLE,
		/** A run is being timed. */
		RUNNING,
		/** The last run is done and its final time is shown. */
		FINISHED
	}

	private State state = State.IDLE;
	private long startNanos;
	private long finalNanos;

	/** Starts a fresh run. Anything already running or finished is discarded. */
	public synchronized void start(long nowNanos) {
		state = State.RUNNING;
		startNanos = nowNanos;
		finalNanos = 0L;
	}

	/**
	 * Stops the run and returns its time in nanoseconds, or -1 if no run was being timed.
	 */
	public synchronized long finish(long nowNanos) {
		if (state != State.RUNNING) {
			return -1L;
		}
		finalNanos = Math.max(0L, nowNanos - startNanos);
		state = State.FINISHED;
		return finalNanos;
	}

	/** Clears the timer back to its idle state. */
	public synchronized void reset() {
		state = State.IDLE;
		startNanos = 0L;
		finalNanos = 0L;
	}

	public synchronized State state() {
		return state;
	}

	/** Time to show right now: counting up while running, frozen when finished, zero when idle. */
	public synchronized long elapsedNanos(long nowNanos) {
		return switch (state) {
			case IDLE -> 0L;
			case RUNNING -> Math.max(0L, nowNanos - startNanos);
			case FINISHED -> finalNanos;
		};
	}

	/** Formats a time as minutes:seconds.hundredths, for example 3:42.18. */
	public static String format(long nanos) {
		long hundredths = Math.max(0L, nanos) / 10_000_000L;
		long minutes = hundredths / 6000L;
		long seconds = (hundredths / 100L) % 60L;
		long rest = hundredths % 100L;
		return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, rest);
	}

	/** True if this chat line is the Crystal Nucleus loot bundle header. */
	public static boolean isFinishMessage(String chatLine) {
		return chatLine != null && chatLine.toLowerCase(Locale.ROOT).contains(FINISH_TEXT);
	}
}
