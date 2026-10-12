package com.ninjachicken784.advancednucruns.client;

/**
 * One movable, hideable box on the screen. The position is the top-left corner in GUI pixels.
 */
final class HudBox {
	enum Kind {
		/** The live timer counting the current run. */
		TIMER,
		/** The popup shown for a few seconds after a run: "Run Time", or "PB" for a new best. */
		RUN_TIME,
		/** The fastest run. */
		PB,
		/** The average of this session's runs. */
		AVERAGE
	}

	final Kind kind;
	/** Name used in the config file. */
	final String key;
	int x;
	int y;
	boolean hidden;

	HudBox(Kind kind, String key, int defaultX, int defaultY) {
		this.kind = kind;
		this.key = key;
		this.x = defaultX;
		this.y = defaultY;
	}
}
