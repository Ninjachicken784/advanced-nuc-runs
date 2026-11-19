package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Works out where the Metal Detector's treasure chest is in the Mines of Divan from the distances
 * it shows ("TREASURE: 7.3m"). The Mines are always the same, so a chest can only be at one of
 * {@link #KNOWN_OFFSETS} from the center of the Mines. Every reading, also while walking, removes
 * the spots that do not fit its distance, so one to three readings are enough. If no known spot
 * fits (Hypixel changed them), a search over the whole cave floor is used instead. Contains no
 * Minecraft code, so it can be tested on its own.
 */
public final class ChestFinder {
	/** Where a chest can be, as offsets (x, y, z) of the chest block from the center of the Mines. */
	public static final int[][] KNOWN_OFFSETS = {
		{-38, -22, 26}, {38, -22, -26}, {-40, -22, 18}, {-41, -20, 22},
		{-5, -21, 16}, {40, -22, -30}, {-42, -20, -28}, {-43, -22, -40},
		{42, -19, -41}, {43, -21, -16}, {-1, -22, -20}, {6, -21, 28},
		{7, -21, 11}, {7, -21, 22}, {-12, -21, -44}, {12, -22, 31},
		{12, -22, -22}, {12, -21, 7}, {12, -21, -43}, {-14, -21, 43},
		{-14, -21, 22}, {-17, -21, 20}, {-20, -22, 0}, {1, -21, 20},
		{19, -22, 29}, {20, -22, 0}, {20, -21, -26}, {-23, -22, 40},
		{22, -21, -14}, {-24, -22, 12}, {23, -22, 26}, {23, -22, -39},
		{24, -22, 27}, {25, -22, 17}, {29, -21, -44}, {-31, -21, -12},
		{-31, -21, -40}, {30, -21, -25}, {-32, -21, -40}, {-36, -20, 42},
		{-37, -21, -14}, {-37, -21, -22}
	};

	/** One chest block. */
	public record Spot(int x, int y, int z) { }

	/** What the readings say so far: the spots, and how sure that is. */
	public record Result(List<Spot> spots, boolean certain, boolean estimated) {
		public static final Result NONE = new Result(List.of(), false, false);

		/** The spot to point at: the only one, or the one that fits best. Null if there is none. */
		public Spot best() {
			return spots.isEmpty() ? null : spots.get(0);
		}
	}

	private record Reading(double x, double y, double z, double distance) { }

	/** A spot that is further off than this from a reading cannot be the chest, as the player moved a little. */
	private static final double TOLERANCE = 1.0;
	/** The same, for the search over the whole floor, which has many more spots to confuse with each other. */
	private static final double FLOOR_TOLERANCE = 0.6;
	private static final int MAX_READINGS = 10;
	private static final int FLOOR_RADIUS = 50;
	private static final int FLOOR_LOW = -24;
	private static final int FLOOR_HIGH = -17;

	private final List<Reading> readings = new ArrayList<>();
	private boolean hasCenter;
	private int centerX;
	private int centerY;
	private int centerZ;

	/** Sets the center of the Mines. The chest spots are measured from it. */
	public synchronized void setCenter(int x, int y, int z) {
		if (!hasCenter || x != centerX || y != centerY || z != centerZ) {
			readings.clear();
		}
		hasCenter = true;
		centerX = x;
		centerY = y;
		centerZ = z;
	}

	public synchronized boolean hasCenter() {
		return hasCenter;
	}

	/** Forgets the center and every reading, for example after changing to another world. */
	public synchronized void resetAll() {
		hasCenter = false;
		readings.clear();
	}

	/** Forgets the readings only, for example after the chest was opened and a new one will appear. */
	public synchronized void clearReadings() {
		readings.clear();
	}

	public synchronized int readingCount() {
		return readings.size();
	}

	/**
	 * Adds a reading: where the player stood (feet position) and the distance the detector showed.
	 * The same reading twice in a row is only added once.
	 */
	public synchronized void addReading(double x, double y, double z, double distance) {
		if (!readings.isEmpty()) {
			Reading last = readings.get(readings.size() - 1);
			if (last.distance() == distance && last.x() == x && last.y() == y && last.z() == z) {
				return;
			}
		}
		readings.add(new Reading(x, y, z, distance));
		if (readings.size() > MAX_READINGS) {
			readings.remove(0);
		}
	}

	/** The distance the detector would show for a chest block: from the feet to the top of the block's corner. */
	private static double distance(Reading r, int bx, int by, int bz) {
		double dx = r.x() - bx;
		double dy = r.y() - (by + 1);
		double dz = r.z() - bz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private double meanError(int bx, int by, int bz, double limit) {
		double sum = 0d;
		for (Reading r : readings) {
			double error = Math.abs(distance(r, bx, by, bz) - r.distance());
			if (error > limit) {
				return Double.NaN;
			}
			sum += error;
		}
		return sum / readings.size();
	}

	private record Scored(Spot spot, double error) { }

	/** The chest spots that fit all readings, best first. */
	public synchronized Result solve() {
		if (!hasCenter || readings.isEmpty()) {
			return Result.NONE;
		}

		List<Scored> fits = new ArrayList<>();
		for (int[] offset : KNOWN_OFFSETS) {
			int bx = centerX + offset[0];
			int by = centerY + offset[1];
			int bz = centerZ + offset[2];
			double error = meanError(bx, by, bz, TOLERANCE);
			if (!Double.isNaN(error)) {
				fits.add(new Scored(new Spot(bx, by, bz), error));
			}
		}
		fits.sort(Comparator.comparingDouble(Scored::error));

		if (!fits.isEmpty()) {
			boolean certain = fits.size() == 1
				|| (fits.get(0).error() <= 0.15 && fits.get(1).error() >= fits.get(0).error() + 0.5);
			List<Spot> spots = new ArrayList<>();
			for (Scored fit : fits) {
				spots.add(fit.spot());
			}
			return new Result(certain ? List.of(spots.get(0)) : spots, certain, false);
		}

		// No known spot fits. With enough readings from different places the floor can be searched.
		if (readings.size() < 3) {
			return Result.NONE;
		}
		Scored best = null;
		for (int x = centerX - FLOOR_RADIUS; x <= centerX + FLOOR_RADIUS; x++) {
			for (int z = centerZ - FLOOR_RADIUS; z <= centerZ + FLOOR_RADIUS; z++) {
				for (int y = centerY + FLOOR_LOW; y <= centerY + FLOOR_HIGH; y++) {
					double error = meanError(x, y, z, FLOOR_TOLERANCE);
					if (!Double.isNaN(error) && (best == null || error < best.error())) {
						best = new Scored(new Spot(x, y, z), error);
					}
				}
			}
		}
		return best == null ? Result.NONE : new Result(List.of(best.spot()), true, true);
	}
}
