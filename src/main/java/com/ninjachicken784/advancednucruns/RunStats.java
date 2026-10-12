package com.ninjachicken784.advancednucruns;

/**
 * Keeps the personal best (PB) and this session's average. Contains no Minecraft code so it can
 * be tested on its own.
 *
 * <p>The PB is the fastest run ever and is meant to be saved between sessions by the caller.
 * The average only covers the runs finished since the game was started.
 */
public final class RunStats {
	/** What happened when a run was recorded. */
	public record Result(boolean personalBest, long averageNanos) { }

	private long pbNanos = -1L;
	private long sessionTotalNanos;
	private int sessionRuns;

	/**
	 * Records a finished run. It is a personal best if it is the first run ever, or strictly faster
	 * than the current PB. A run that only ties the PB is not a new PB.
	 */
	public synchronized Result record(long runNanos) {
		long run = Math.max(1L, runNanos);
		boolean personalBest = pbNanos < 0L || run < pbNanos;
		if (personalBest) {
			pbNanos = run;
		}
		sessionTotalNanos += run;
		sessionRuns++;
		return new Result(personalBest, sessionTotalNanos / sessionRuns);
	}

	/** The fastest run in nanoseconds, or -1 if there is none yet. */
	public synchronized long pbNanos() {
		return pbNanos;
	}

	/** The average of this session's runs in nanoseconds, or -1 if none were finished yet. */
	public synchronized long averageNanos() {
		return sessionRuns == 0 ? -1L : sessionTotalNanos / sessionRuns;
	}

	/** How many runs were finished this session. */
	public synchronized int sessionRuns() {
		return sessionRuns;
	}

	/** Sets the PB loaded from the save file. Ignored if the value is not a real time. */
	public synchronized void restorePb(long nanos) {
		if (nanos > 0L) {
			pbNanos = nanos;
		}
	}

	/** Clears the PB and this session's runs. */
	public synchronized void reset() {
		pbNanos = -1L;
		sessionTotalNanos = 0L;
		sessionRuns = 0;
	}
}
