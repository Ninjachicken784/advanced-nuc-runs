package com.ninjachicken784.advancednucruns;

/**
 * One waypoint. The id is what the player types in "/anc clear waypoint <id>".
 */
public record Waypoint(int id, Kind kind, int x, int y, int z) {

	/** Where the waypoint came from. Re-running /anc JT replaces only JUNGLE_TEMPLE waypoints. */
	public enum Kind {
		JUNGLE_TEMPLE,
		MANUAL
	}

	public String label() {
		return "Waypoint " + id;
	}
}
