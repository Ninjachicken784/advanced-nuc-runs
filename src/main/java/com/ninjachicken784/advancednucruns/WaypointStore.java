package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the active waypoints. Contains no Minecraft code so it can be tested on its own.
 *
 * <p>Ids are the lowest free positive number, so what you see on a waypoint is what you type
 * in "/anc clear waypoint <id>". Clearing one waypoint does not renumber the others.
 */
public final class WaypointStore {
	/** Jungle Temple waypoints as offsets from the player's block position: {x, y, z}. */
	public static final int[][] JUNGLE_TEMPLE_OFFSETS = {
		{4, 10, 65},
		{29, -32, 65},
		{29, -32, 48}
	};

	private final List<Waypoint> waypoints = new ArrayList<>();

	/** Snapshot of the current waypoints, safe to iterate while the store changes. */
	public synchronized List<Waypoint> list() {
		return List.copyOf(waypoints);
	}

	/** Adds one waypoint exactly at the given block position. */
	public synchronized Waypoint addManual(int x, int y, int z) {
		return add(Waypoint.Kind.MANUAL, x, y, z);
	}

	/**
	 * Replaces any earlier Jungle Temple waypoints with a fresh set of three, offset from the
	 * given block position. Manual waypoints are left alone.
	 */
	public synchronized List<Waypoint> setJungleTemple(int x, int y, int z) {
		waypoints.removeIf(w -> w.kind() == Waypoint.Kind.JUNGLE_TEMPLE);
		List<Waypoint> added = new ArrayList<>();
		for (int[] o : JUNGLE_TEMPLE_OFFSETS) {
			added.add(add(Waypoint.Kind.JUNGLE_TEMPLE, x + o[0], y + o[1], z + o[2]));
		}
		return added;
	}

	/** Removes every waypoint and returns how many there were. */
	public synchronized int clearAll() {
		int count = waypoints.size();
		waypoints.clear();
		return count;
	}

	/** Removes the waypoint with this id. Returns false if there is none. */
	public synchronized boolean clear(int id) {
		return waypoints.removeIf(w -> w.id() == id);
	}

	private Waypoint add(Waypoint.Kind kind, int x, int y, int z) {
		Waypoint waypoint = new Waypoint(lowestFreeId(), kind, x, y, z);
		waypoints.add(waypoint);
		return waypoint;
	}

	private int lowestFreeId() {
		int id = 1;
		while (isUsed(id)) {
			id++;
		}
		return id;
	}

	private boolean isUsed(int id) {
		for (Waypoint w : waypoints) {
			if (w.id() == id) {
				return true;
			}
		}
		return false;
	}
}
